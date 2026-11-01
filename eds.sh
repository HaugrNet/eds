#!/bin/bash

# Configuration
scriptDir="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
readonly scriptDir
readonly composeDir="${scriptDir}/accessories/docker"
readonly podmanDir="${scriptDir}/accessories/podman"
readonly composeFile="${composeDir}/docker-compose.yml"
readonly dockerFile="${composeDir}/Dockerfile.quarkus"
readonly targetDir="${scriptDir}/target"
readonly sbomImage="eds-quarkus:sbom-check"
readonly appJar="${scriptDir}/eds-quarkus/target/eds-runnable.jar"
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
    [[ -f "${appJar}" ]]
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
    echo "    run <module>       Run standalone JAR (quarkus | test)"
    echo "                       * Quarkus uses PostgreSQL, at EDS_DB_URL, or by default"
    echo "                         at jdbc:postgresql://localhost:5432/eds."
    echo "                       * Test uses the in-memory H2 database."
    echo "    run fitnesse       Runs the Fitnesse test suite"
    echo
    echo "  Container Commands:"
    echo "    start              Start (build/prepare) containers"
    echo "    stop               Stop containers"
    echo "    status             Check health endpoints"
    echo "    logs               Follow container logs"
    echo "    remove             Remove containers & images"
    echo
    echo "  SBOM Commands:"
    echo "    sbom               Build Maven & containers, generate the SBOMs and"
    echo "                       scan them for vulnerabilities (like the CI Build)"
    echo "    sbom --skip-build  Reuse the existing build, only regenerate the"
    echo "                       container SBOMs and run the scans"
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
    mkdir -p "${composeDir}/volumes/logs" 2>/dev/null
    chmod 777 "${composeDir}/volumes/logs" 2>/dev/null || true
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

    # Check Quarkus health (default in docker-compose). The health endpoints
    # are served on the management port 9000, which is not published to the
    # host, so the check runs inside the container.
    echo -n "Quarkus health: "
    if isRunning; then
        local quarkus_health
        quarkus_health=$(docker compose -f "${composeFile}" exec -T eds-app \
            wget -q -T 4 -O - "http://localhost:9000/q/health/ready" 2>/dev/null)
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
        # The same JAR is used for both, the database is selected by
        # EDS_DB_URL: set means PostgreSQL, unset means in-memory H2.
        local jarFile="${appJar}"
        local moduleName=""
        local databaseUrl=""

        case "${module}" in
            quarkus)
                moduleName="Quarkus (PostgreSQL)"
                databaseUrl="${EDS_DB_URL:-jdbc:postgresql://localhost:5432/eds}"
                ;;
            test)
                moduleName="Quarkus (in-memory H2)"
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
        EDS_DB_URL="${databaseUrl}" java -jar "${jarFile}"
    fi
}

# ==============================================================================
# SBOM Check
# ------------------------------------------------------------------------------
# Builds everything and checks the Software Bill of Materials (SBOM) for both
# the Maven build and the EDS container, the same way as the CI Build does:
#
#   1. Maven build with the "sbom" profile, producing the CycloneDX SBOMs.
#   2. Builds the EDS container image.
#   3. Generates a CycloneDX SBOM for the container image with Syft.
#   4. Validates the SBOMs, and scans them for vulnerabilities with Grype.
#
# Both fail the check on High or Critical findings. Reviewed findings can be
# suppressed in .grype.yaml.
#
# Requires: java 25, docker, syft, grype, jq (and network access, as the
# CycloneDX plugin is skipped when Maven runs offline).
# ==============================================================================
sbomFailures=()
sbomMavenFile=""
sbomLogDir=""

function sbomSection() {
    echo
    echo "${1}"
}

function sbomOk() {
    echo "  [OK]   ${1}"
}

function sbomFail() {
    echo "  [FAIL] ${1}"
    sbomFailures+=("${1}")
}

# Formats the seconds since the given start time, e.g. "1m 23s"
function sbomElapsed() {
    local seconds=$((SECONDS - ${1}))
    if [[ ${seconds} -ge 60 ]]; then
        echo "$((seconds / 60))m $((seconds % 60))s"
    else
        echo "${seconds}s"
    fi
}

# Runs a command with all output written to a log file. On success, a single
# line is printed; on failure, the end of the log is shown as well.
#   $1 = description, $2 = log name, $3... = command
function sbomRun() {
    local description="${1}"
    local log="${sbomLogDir}/${2}.log"
    shift 2

    local start=${SECONDS}
    if (cd "${scriptDir}" && "$@") > "${log}" 2>&1; then
        sbomOk "${description} ($(sbomElapsed "${start}"))"
        return 0
    fi

    sbomFail "${description}"
    echo "         Last lines of ${log}:"
    tail -n 25 "${log}" | sed 's/^/         | /'
    return 1
}

# Verifies that all required tools are available
function sbomPrerequisites() {
    local missing=()
    for tool in java docker syft grype jq; do
        command -v "${tool}" > /dev/null 2>&1 || missing+=("${tool}")
    done

    local result=0
    if [[ ${#missing[@]} -eq 0 ]]; then
        sbomOk "Tools: java, docker, syft, grype, jq"
    else
        sbomFail "Missing tools: ${missing[*]}"
        result=1
    fi

    if ! docker info > /dev/null 2>&1; then
        sbomFail "Docker daemon is not reachable"
        result=1
    fi

    return ${result}
}

# Maven build with the SBOM profile, and the container image
function sbomBuild() {
    local mvn="mvn"
    [[ -x "${scriptDir}/mvnw" ]] && mvn="${scriptDir}/mvnw"

    sbomRun "Maven build (-Psbom)" maven \
        "${mvn}" -B -q -ntp -DskipTests -Psbom clean package || return 1
    sbomRun "Built image ${sbomImage}" docker-build \
        docker build -q -f "${dockerFile}" -t "${sbomImage}" .
}

# Container SBOM, generated with Syft
function sbomGenerateContainerSboms() {
    mkdir -p "${targetDir}"
    sbomRun "Syft SBOM for ${sbomImage}" syft-eds \
        syft scan "${sbomImage}" -q -o "cyclonedx-json=${targetDir}/container-eds-quarkus-cyclonedx.json"
}

# Counts the components with a Maven package URL in an SBOM
function sbomCountMaven() {
    jq '[.components[]? | select(.purl? // "" | startswith("pkg:maven/"))] | length' "${1}"
}

# Validates a CycloneDX JSON SBOM, and prints a short content summary
#   $1 = label, $2 = file, $3 = expected spec version (empty = any)
function sbomValidate() {
    local label="${1}"
    local file="${2}"
    local spec="${3}"

    if [[ ! -s "${file}" ]]; then
        sbomFail "${label}: ${file#"${scriptDir}"/} is missing or empty"
        return
    fi

    if ! jq -e '.bomFormat == "CycloneDX"' "${file}" > /dev/null 2>&1; then
        sbomFail "${label}: not a valid CycloneDX JSON document"
        return
    fi

    local actualSpec components
    actualSpec=$(jq -r '.specVersion' "${file}")
    components=$(jq '.components | length' "${file}")

    if [[ -n "${spec}" && "${actualSpec}" != "${spec}" ]]; then
        sbomFail "${label}: CycloneDX ${actualSpec}, expected ${spec}"
    elif [[ "${components}" -eq 0 ]]; then
        sbomFail "${label}: contains no components"
    else
        sbomOk "${label}: CycloneDX ${actualSpec}, ${components} components ($(sbomCountMaven "${file}") Maven)"
    fi
}

function sbomValidateAll() {
    local candidates=("${targetDir}"/eds-*-cyclonedx.json)
    [[ -e "${candidates[0]}" ]] && sbomMavenFile="${candidates[0]}"

    local edsSbom="${targetDir}/container-eds-quarkus-cyclonedx.json"
    sbomValidate "Maven" "${sbomMavenFile:-${targetDir}/eds-<version>-cyclonedx.json}" "1.6"
    sbomValidate "EDS image" "${edsSbom}" ""

    # The EDS image is built from the uber-jar, so it must also contain the
    # Java libraries, not only the operating system packages and the JRE.
    if [[ -s "${edsSbom}" && "$(sbomCountMaven "${edsSbom}")" -eq 0 ]]; then
        sbomFail "EDS image: no Java libraries found, the uber-jar was not detected"
    fi
}

# Vulnerability scan of one SBOM with Grype. Prints the number of findings
# per severity; for gated scans, High and Critical findings are listed and
# fail the check (the same as "grype --fail-on high").
#   $1 = label, $2 = SBOM file, $3 = report name, $4 = "gate" to fail on High
function sbomScan() {
    local label="${1}"
    local file="${2}"
    local report="${targetDir}/${3}"
    local gate="${4:-}"

    if [[ ! -s "${file}" ]]; then
        sbomFail "${label}: no SBOM to scan"
        return
    fi

    # Run from the project root, so Grype picks up .grype.yaml
    local log="${sbomLogDir}/grype-${3%.json}.log"
    if ! (cd "${scriptDir}" && grype "sbom:${file}" -q -o "json=${report}") > "${log}" 2>&1; then
        sbomFail "${label}: scan failed, see ${log}"
        return
    fi

    local counts severe
    counts=$(jq -r '[.matches[].vulnerability.severity] as $s
        | ["Critical", "High", "Medium", "Low", "Negligible", "Unknown"]
        | map(. as $level | {level: $level, n: ($s | map(select(. == $level)) | length)})
        | map(select(.n > 0) | "\(.n) \(.level)")
        | if length == 0 then "no findings" else join(", ") end' "${report}")
    severe=$(jq '[.matches[] | select(.vulnerability.severity == "Critical" or .vulnerability.severity == "High")] | length' "${report}")

    if [[ "${gate}" != "gate" ]]; then
        sbomOk "${label}: ${counts} (report only)"
    elif [[ "${severe}" -eq 0 ]]; then
        sbomOk "${label}: ${counts}"
    else
        sbomFail "${label}: ${counts}"
        jq -r '[.matches[] | select(.vulnerability.severity == "Critical" or .vulnerability.severity == "High")
                | "\(.vulnerability.severity)\t\(.vulnerability.id)\t\(.artifact.name) \(.artifact.version)\t\(
                    if (.vulnerability.fix.versions | length) > 0 then "fixed in " + (.vulnerability.fix.versions | join(", ")) else "no fix" end)"]
            | unique | .[]' "${report}" | column -t -s $'\t' | sed 's/^/         /'
    fi
}

function sbomScanAll() {
    sbomScan "Maven" "${sbomMavenFile}" "grype-report.json" gate
    sbomScan "EDS image" "${targetDir}/container-eds-quarkus-cyclonedx.json" "grype-report-eds-container.json" gate
}

# Prints the summary, and returns the result
function sbomSummary() {
    echo
    if [[ ${#sbomFailures[@]} -eq 0 ]]; then
        echo "All SBOM checks passed. SBOMs and reports: ${targetDir#"${scriptDir}"/}/"
        rm -rf "${sbomLogDir}"
        return 0
    fi

    echo "${#sbomFailures[@]} SBOM check(s) failed. Logs: ${sbomLogDir}"
    return 1
}

function doSbom() {
    local skipBuild=false
    case "${1:-}" in
        --skip-build)
            skipBuild=true
            ;;
        "")
            ;;
        *)
            echo "Usage: $(basename "${0}") sbom [--skip-build]"
            return 1
            ;;
    esac

    sbomFailures=()
    sbomLogDir=$(mktemp -d -t eds-sbom-XXXXXX)
    local start=${SECONDS}

    echo "EDS SBOM Check"
    sbomSection "Build"
    sbomPrerequisites || { sbomSummary; return 1; }

    if [[ "${skipBuild}" == true ]]; then
        if docker image inspect "${sbomImage}" > /dev/null 2>&1; then
            sbomOk "Build skipped, reusing ${sbomImage}"
        else
            sbomFail "${sbomImage} does not exist, run without --skip-build"
            sbomSummary
            return 1
        fi
    else
        sbomBuild || { sbomSummary; return 1; }
    fi

    sbomGenerateContainerSboms

    sbomSection "SBOMs"
    sbomValidateAll

    sbomSection "Vulnerabilities (fails on High or Critical)"
    sbomScanAll

    echo
    echo "Completed in $(sbomElapsed "${start}")."
    sbomSummary
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
        cp "${appJar}" "${podmanDir}"
        podman build -f "${podmanDir}/eds-quarkus.container" -t eds-quarkus "${podmanDir}"
        rm "${podmanDir}/eds-runnable.jar"
        ;;
    pstart)
        podman run -d \
          --rm \
          --network=host \
          -e EDS_DB_URL="${EDS_DB_URL:-jdbc:postgresql://localhost:5432/eds}" \
          -e EDS_DB_USERNAME="${EDS_DB_USERNAME:-eds_user}" \
          -e EDS_DB_PASSWORD="${EDS_DB_PASSWORD:-}" \
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
    sbom)
        doSbom "$@"
        exit $?
        ;;
    *)
        showHelp
        ;;
esac
