#!/bin/bash

# Configuration
scriptDir="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
readonly scriptDir
readonly composeDir="${scriptDir}/accessories/docker"
readonly podmanDir="${scriptDir}/accessories/podman"
readonly composeFile="${composeDir}/docker-compose.yml"
readonly PORT=${EDS_PORT:-8080}

# ==============================================================================
# Check if Compose services are running
# ==============================================================================
function isRunning() {
    local status
    status=$(docker compose -f "${composeFile}" ps --status running -q 2>/dev/null)
    [[ -n "${status}" ]]
}

# ==============================================================================
# Check if Compose services exist
# ==============================================================================
function exists() {
    local status
    status=$(docker compose -f "${composeFile}" ps -a -q 2>/dev/null)
    [[ -n "${status}" ]]
}

# ==============================================================================
# Check if runnable JARs exist
# ==============================================================================
function jarsExist() {
    [[ -f "${scriptDir}/eds-quarkus-h2/target/eds-runnable.jar" ]] || \
    [[ -f "${scriptDir}/eds-quarkus-pg/target/eds-runnable.jar" ]]
}

# ==============================================================================
# Shows the help
# ==============================================================================
function showHelp() {
    echo "EDS - Encrypted Data Share Control Script"
    echo
    echo "  Usage: $(basename "${0}") <command>"
    echo
    echo "  Run Commands:"
    echo "    run <module>     Run standalone JAR (quarkus | test)"
    echo "                     * Quarkus requires a running EDS PostgreSQL database."
    echo "                     * Test will launch Quarkus with an in-memory database."
    echo "    run fitnesse     Runs the Fitnesse test suite"
    echo
    echo "  Container Commands:"
    echo "    start            Start (build/prepare) containers"
    echo "    stop             Stop containers"
    echo "    status           Check health endpoints"
    echo "    logs             Follow container logs"
    echo "    remove           Remove containers & images"
    echo

    # Show current status
    echo "Current Status:"
    if isRunning; then
        echo "  Containers are RUNNING"
        echo "  Access at: http://localhost:${PORT}/eds"
    elif exists; then
        echo "  Containers exist but are STOPPED"
    else
        echo "  Containers do not exist"
    fi

    if jarsExist; then
        echo "  Runnable JARs are available"
    else
        echo "  Runnable JARs not found (run 'build' first)"
    fi
    echo
}

# ==============================================================================
# Build using Maven wrapper
# ==============================================================================
function doBuild() {
    echo "Minimalistic build of the Quarkus Uber Jar"
    echo "In case of errors, please following line to /etc/containers/registries.conf"
    echo "unqualified-search-registries = [\"docker.io\"]"

    podman build -f "${scriptDir}/build.container" -t eds-builder .
    podman run --rm -v "${scriptDir}":/app -v "${HOME}/.m2":/root/.m2 eds-builder
}

# ==============================================================================
# Start containers
# ==============================================================================
function doStart() {
    if isRunning; then
        echo "EDS containers are already running."
        return 0
    fi

    if ! jarsExist; then
        doBuild
    fi

    echo "Starting EDS containers..."
    mkdir -p "${composeDir}/volumes/logs" "${composeDir}/volumes/data" 2>/dev/null
    chmod 777 "${composeDir}/volumes/logs" "${composeDir}/volumes/data" 2>/dev/null || true
    docker compose -f "${composeFile}" up -d

    waitForReady
}

# ==============================================================================
# Stop containers
# ==============================================================================
function doStop() {
    if isRunning; then
        echo "Stopping EDS containers..."
        docker compose -f "${composeFile}" stop
    else
        echo "EDS containers are not running."
    fi
}

# ==============================================================================
# Restart containers
# ==============================================================================
function doRestart() {
    echo "Restarting EDS containers..."
    docker compose -f "${composeFile}" restart
    waitForReady
}

# ==============================================================================
# Show container status
# ==============================================================================
function doStatus() {
    echo "EDS Container Status:"
    echo "====================="
    docker compose -f "${composeFile}" ps
}

# ==============================================================================
# Check health endpoints
# ==============================================================================
function doHealth() {
    echo "EDS Health Check:"
    echo "================="

    # Check Quarkus health (default in docker-compose)
    echo -n "Quarkus health: "
    local quarkus_health
    quarkus_health=$(curl -s "http://localhost:${PORT}/eds/q/health/ready" 2>/dev/null)
    if [[ -n "${quarkus_health}" ]]; then
        echo "${quarkus_health}" | grep -qE '"status":\s*"UP"' && echo "UP" || echo "DOWN"

        # Also try the version endpoint
        echo -n "EDS API: "
        local version_response
        version_response=$(curl -s -X POST -H "Content-Type: application/json" \
            "http://localhost:${PORT}/eds/version" -d '{}' 2>/dev/null)
        if [[ -n "${version_response}" ]]; then
            local version
            version=$(echo "${version_response}" | grep -o '"version":"[^"]*"' | cut -d'"' -f4)
            echo "${version}"
        else
            echo "Not responding"
        fi
    else
        echo "Not running"
    fi
}

# ==============================================================================
# Show container logs
# ==============================================================================
function doLogs() {
    echo "Following EDS logs (Ctrl+C to exit)..."
    docker compose -f "${composeFile}" logs -f
}

# ==============================================================================
# Cleanup containers, images, and volumes
# ==============================================================================
function doCleanup() {
    echo "Cleaning up EDS container resources..."
    docker compose -f "${composeFile}" down -v --rmi local

    echo "Cleanup complete."
}

# ==============================================================================
# Wait for application to be ready
# ==============================================================================
function waitForReady() {
    echo -n "Waiting for EDS to start "
    local retries=0
    local max_retries=60

    while [[ ${retries} -lt ${max_retries} ]]; do
        local response
        response=$(curl -s -X POST -H "Content-Type: application/json" \
            "http://localhost:${PORT}/eds/version" -d '{}' 2>/dev/null)
        if [[ -n "${response}" ]]; then
            echo
            local version
            version=$(echo "${response}" | grep -o '"version":"[^"]*"' | cut -d'"' -f4)
            echo "EDS is ready! Version: ${version}"
            echo "Access at: http://localhost:${PORT}/eds"
            return 0
        fi
        echo -n "."
        sleep 1
        retries=$((retries + 1))
    done

    echo
    echo "Error: EDS did not start within ${max_retries} seconds"
    echo "Check logs with: $(basename "${0}") logs"
    return 1
}

# ==============================================================================
# Run standalone JAR
# ==============================================================================
function doRun() {
    local module="${1}"

    if [[ -z "${module}" ]]; then
        echo "Usage: $(basename "${0}") run <module>"
        echo "Available modules: quarkus, test"
        return 1
    fi

    if [[ "${module}" == "fitnesse" ]]; then
        jarFile="${scriptDir}/eds-fitnesse/fitnesse-standalone.jar"
        if [[ -e "${jarFile}" ]]; then
            echo "Starting FitNesse, test suite is reachable at: http://localhost:2080/EDS"
            cd "${scriptDir}/eds-fitnesse" && java -jar "${jarFile}" -p 2080
            return 0
        else
            echo "Missing the FitNesse standalone JAR file"
            echo "Please download from https://fitnesse.org and save as ${jarFile}"
            return 1
        fi
    else
        local jarFile=""
        local moduleName=""

        case "${module}" in
            quarkus)
                jarFile="${scriptDir}/eds-quarkus-pg/target/eds-runnable.jar"
                moduleName="Quarkus"
                ;;
            test)
                jarFile="${scriptDir}/eds-quarkus-h2/target/eds-runnable.jar"
                moduleName="Quarkus"
                ;;
            *)
                echo "Unknown module: ${module}"
                echo "Available modules: quarkus | test"
                return 1
                ;;
        esac

        if [[ ! -f "${jarFile}" ]]; then
            echo "Error: ${jarFile} not found."
            echo "Run '$(basename "${0}") build ${module}' first."
            return 1
        fi

        # Stop the EDS container(s) to free port 8080
        doStop

        echo "Starting EDS ${moduleName}..."
        echo "JAR: ${jarFile}"
        echo "Access at: http://localhost:${PORT}/eds"
        echo "Press Ctrl+C to stop"
        echo

        # Run the JAR (use exec to replace shell process for clean Ctrl+C handling)
        java -jar "${jarFile}"
    fi
}

# ==============================================================================
# Main
# ==============================================================================
action="${1}"
shift

case "${action}" in
    build)
        doBuild "$@"
        ;;
    run)
        doRun "$@"
        ;;
    start)
        doStart
        ;;
    stop)
        doStop
        ;;
    restart)
        doRestart
        ;;
    status|health)
        doHealth
        ;;
    logs)
        echo "Following EDS logs (Ctrl+C to exit)..."
        docker compose -f "${composeFile}" logs -f
        ;;
    pbuild)
        cp "${scriptDir}/eds-quarkus-pg/target/eds-runnable.jar" "${podmanDir}"
        podman build -f "${podmanDir}/eds-quarkus.container" -t eds-quarkus "${podmanDir}"
        rm "${podmanDir}/eds-runnable.jar"
        ;;
    pstart)
        podman run -d \
          --rm \
          --network=host \
          -e DB_URL=jdbc:postgresql://localhost:5432/eds \
          --name eds-quarkus \
          eds-quarkus
        ;;
    pstop)
        podman stop eds-quarkus
        ;;
    plogs)
        echo "Following EDS logs (Ctrl+C to exit)..."
        podman logs eds-quarkus -f
        ;;
    clean|cleanup|remove)
        doCleanup
        ;;
    *)
        showHelp
        ;;
esac
