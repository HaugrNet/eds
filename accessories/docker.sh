#!/bin/bash

#
# EDS, Encrypted Data Share - open source Cryptographic Sharing system.
# Copyright (c) 2016-2026, haugr.net
# mailto: eds AT haugr DOT net
#
# EDS is free software; you can redistribute it and/or modify it under the
# terms of the Apache License, as published by the Apache Software Foundation.
#
# EDS is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the Apache License for more details.
#
# You should have received a copy of the Apache License, version 2, along with
# this program; If not, you can download a copy of the License
# here: https://www.apache.org/licenses/
#

# Configuration
port=${EDS_PORT:-8080}
action=${1}
image="eds"
ACCESSORIES="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
compose_dir="$(realpath "${ACCESSORIES}/docker")"
project_root="$(realpath "${ACCESSORIES}/..")"
source "${ACCESSORIES}/release/bin/edsFunctions.sh"

# ==============================================================================
# Check if the Docker Compose services are running
# ------------------------------------------------------------------------------
# Return:
#   True (0) if services are running, otherwise False (1)
# ==============================================================================
function running() {
    local status
    status=$(docker compose -f "${compose_dir}/docker-compose.yml" ps --status running -q 2>/dev/null)
    if [[ -n "${status}" ]]; then
        return 0
    else
        return 1
    fi
}

# ==============================================================================
# Check if the Docker Compose services exist (stopped or running)
# ------------------------------------------------------------------------------
# Return:
#   True (0) if services exist, otherwise False (1)
# ==============================================================================
function exists() {
    local status
    status=$(docker compose -f "${compose_dir}/docker-compose.yml" ps -a -q 2>/dev/null)
    if [[ -n "${status}" ]]; then
        return 0
    else
        return 1
    fi
}

# ==============================================================================
# Check if the Quarkus JAR exists
# ------------------------------------------------------------------------------
# Return:
#   True (0) if JAR exists, otherwise False (1)
# ==============================================================================
function jarExists() {
    if [[ -f "${project_root}/eds-quarkus/target/eds-runnable.jar" ]]; then
        return 0
    else
        return 1
    fi
}

# ==============================================================================
# Shows the help for the Script
# ==============================================================================
function showHelp() {
    echo "${image^^} Docker Compose Control Script"
    echo "Usage: $(basename "${0}") [Action]"
    echo
    echo "  Actions:"
    echo "    build        Build Quarkus JAR and start containers"
    echo "    start        Start existing containers"
    echo "    stop         Stop running containers"
    echo "    restart      Restart containers"
    echo "    reset        Stop containers and reset database (removes data)"
    echo "    logs         Follow container logs"
    echo "    status       Show container status"
    echo "    interactive  Enter the application container shell"
    echo "    remove       Remove containers and images"
    echo
    if (running); then
        echo "${image^^} is currently accessible at http://localhost:${port}/${image}"
    elif (exists); then
        echo "${image^^} containers exist but are stopped. Run 'start' to start them."
    else
        echo "${image^^} containers do not exist. Run 'build' to create them."
    fi
}

# ==============================================================================
# Builds the Quarkus JAR and starts Docker Compose services
# ==============================================================================
function doBuild() {
    if (running); then
        echo "${image^^} is already running. Use 'restart' or 'stop' first."
        return 1
    fi

    echo "Building Quarkus JAR..."
    cd "${project_root}" || exit 1
    if ! mvn clean package -pl eds-quarkus -am -DskipTests; then
        echo "Maven build failed!"
        return 1
    fi

    echo
    echo "Creating volumes directories..."
    mkdir -p "${compose_dir}/volumes/logs" "${compose_dir}/volumes/data"
    chmod 777 "${compose_dir}/volumes/logs" "${compose_dir}/volumes/data"

    echo
    echo "Starting Docker Compose services..."
    docker compose -f "${compose_dir}/docker-compose.yml" up -d --build

    echo
    checkAlive
}

# ==============================================================================
# Starts the Docker Compose services
# ==============================================================================
function doStart() {
    if (running); then
        echo "${image^^} is already running."
        return 0
    fi

    if ! (jarExists); then
        echo "Quarkus JAR not found. Run 'build' first."
        return 1
    fi

    echo "Starting ${image^^} containers..."
    docker compose -f "${compose_dir}/docker-compose.yml" up -d

    checkAlive
}

# ==============================================================================
# Stops the Docker Compose services
# ==============================================================================
function doStop() {
    if (running); then
        echo "Stopping ${image^^} containers..."
        docker compose -f "${compose_dir}/docker-compose.yml" stop
    else
        echo "${image^^} is not running."
    fi
}

# ==============================================================================
# Restarts the Docker Compose services
# ==============================================================================
function doRestart() {
    echo "Restarting ${image^^} containers..."
    docker compose -f "${compose_dir}/docker-compose.yml" restart

    checkAlive
}

# ==============================================================================
# Resets the database (removes data and restarts)
# ==============================================================================
function doReset() {
    echo "Resetting ${image^^} (this will delete all data)..."
    docker compose -f "${compose_dir}/docker-compose.yml" down

    echo "Removing database data..."
    rm -rf "${compose_dir}/volumes/data"/*

    echo "Creating volumes directories..."
    mkdir -p "${compose_dir}/volumes/logs" "${compose_dir}/volumes/data"
    chmod 777 "${compose_dir}/volumes/logs" "${compose_dir}/volumes/data"

    docker compose -f "${compose_dir}/docker-compose.yml" up -d

    checkAlive
}

# ==============================================================================
# Shows logs for the Docker Compose services
# ==============================================================================
function doLogs() {
    echo "Showing ${image^^} logs (Ctrl+C to exit)..."
    docker compose -f "${compose_dir}/docker-compose.yml" logs -f
}

# ==============================================================================
# Shows status of Docker Compose services
# ==============================================================================
function doStatus() {
    echo "Current status of ${image^^} containers:"
    docker compose -f "${compose_dir}/docker-compose.yml" ps
}

# ==============================================================================
# Enters the application container interactively
# ==============================================================================
function doInteractive() {
    if (running); then
        echo "Entering the eds-quarkus container..."
        docker compose -f "${compose_dir}/docker-compose.yml" exec eds-app sh
    else
        echo "${image^^} is not running."
    fi
}

# ==============================================================================
# Removes all Docker Compose services and images
# ==============================================================================
function doRemove() {
    echo "Removing ${image^^} containers, images, and volumes..."
    docker compose -f "${compose_dir}/docker-compose.yml" down -v --rmi local
}

# ==============================================================================
# Waits for the application to be ready
# ==============================================================================
function checkAlive() {
    echo -n "Waiting for ${image^^} to start "
    retries=0
    max_retries=60

    while [[ ${retries} -lt ${max_retries} ]]; do
        response=$(curl --silent --fail --header "Content-Type: application/json" \
            --request POST "http://localhost:${port}/eds/version" \
            --data '{}' 2>/dev/null)
        if [[ -n "${response}" ]]; then
            echo
            version=$(inspectResponse "${response}" "version")
            echo "${image^^} is ready! Version: ${version}"
            echo "Access at: http://localhost:${port}/${image}"
            return 0
        fi
        echo -n "."
        sleep 1
        retries=$((retries + 1))
    done

    echo
    echo "Error: ${image^^} did not start within ${max_retries} seconds"
    echo "Check logs with: $(basename "${0}") logs"
    return 1
}

# ==============================================================================
# Main logic
# ==============================================================================
case "${action}" in
    build)
        doBuild
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
    reset)
        doReset
        ;;
    logs)
        doLogs
        ;;
    status)
        doStatus
        ;;
    it|interactive)
        doInteractive
        ;;
    remove)
        doRemove
        ;;
    *)
        showHelp
        ;;
esac
echo
