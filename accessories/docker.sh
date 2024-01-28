#!/bin/bash

#
# EDS, Encrypted Data Share - open source Cryptographic Sharing system.
# Copyright (c) 2016-2024, haugr.net
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

port=${EDS_PORT:-}
if [[ "${port}" = "" ]]; then
    port=8080
fi

action=${1}
image="eds"
container="${image}-2.0"
source release/bin/edsFunctions.sh

# ==============================================================================
# Check if the Docker container exists
# ------------------------------------------------------------------------------
# Return:
#   True (0) if EDS exists, otherwise False (1)
# ==============================================================================
function exists() {
    readonly exists=$(docker images -a | grep "${image}")
    if [[ ${#exists} -eq 0 ]]; then
        return 1
    else
        return 0
    fi
}

# ==============================================================================
# Check if the Docker is running or not
# ------------------------------------------------------------------------------
# Return:
#   True (0) if EDS is running, otherwise False (1)
# ==============================================================================
function running() {
    readonly alive=$(docker ps -a | grep "${image}" | grep "Exited")
    if [[ ${#alive} -eq 0 ]]; then
        return 0
    else
        return 1
    fi
}

# ==============================================================================
# Shows the help for the Script
# ==============================================================================
function showHelp() {
    echo "${image^^} Control Script"
    echo "Usage: $(basename "${0}") [Action]"
    echo
    echo "  The Action must be one of the following:"

    if (exists); then
        if (running); then
            echo "    stop         ( $ docker stop ${container} )"
            echo "    logs         ( $ docker logs -f ${container} )"
            echo "    status       ( $ docker ps -f name=${container} )"
            echo "    interactive  ( $ docker exec -it ${container} bash )"
            echo
            echo "${image^^} is currently accessible at http://localhost:${port}/${image}"
        else
            echo "    start        ( $ docker start ${container} )"
            echo "    remove       ( $ docker rm ${container} && docker rmi ${image} )"
        fi
    else
        echo "    build        ( $ docker build -t ${image} -f Dockerfile )"
        echo "                 ( $ docker run --name ${container} -d -p ${port}:8080 ${image} )"
    fi
}

# ==============================================================================
# Builds & Runs the Docker image
# ==============================================================================
function doBuild() {
    if (exists); then
        echo "Cannot build the ${image^^} Docker image, as it already exists"
        echo
        showHelp
    else
        http_proxy=${http_proxy:-}
        if [[ "${http_proxy}" != "" ]]; then
            # Proxy can be defined in many ways, to ensure that it has the correct
            # format, the protocol and slashes are first stripped, if they were
            # set - to prevent that they are either missing or added double here
            proxy=${http_proxy//http:\/\//}
            proxy=${proxy//\//}
            echo "Acquire::http::Proxy \"http://${proxy}/\";" > docker/apt.conf
            echo "use_proxy = on" > docker/wgetrc
            echo "http_proxy = http://${proxy}/" >> docker/wgetrc
            echo "https_proxy = http://${proxy}/" >> docker/wgetrc
        else
            echo "" > docker/apt.conf
            echo "use_proxy = off" > docker/wgetrc
        fi

        # Create Docker image
        cp ../eds-wildfly/target/eds.war docker
        docker build --build-arg HTTP_PROXY="${proxy}" --build-arg HTTPS_PROXY="${proxy}" -t ${image} -f docker/Dockerfile .
        rm docker/apt.conf docker/wgetrc docker/eds.war

        echo
        echo "${image^^} Docker image build completed. Will now run the following command:"
        echo "$ docker run --name ${container} -d -p ${port}:8080 ${image}"
        docker run --name ${container} -d -p ${port}:8080 ${image} >/dev/null
        echo
        checkAlive
    fi
}

# ==============================================================================
# Starts the Docker container
# ==============================================================================
function doStart() {
    if (exists && ! running); then
        echo "Starting the ${container} container"
        docker start ${container} >/dev/null
        checkAlive
    else
        showHelp
    fi
}

# ==============================================================================
# Stopping a running image
# ==============================================================================
function doStop() {
    if (exists && running); then
        echo "Stopping the ${container} container"
        docker stop ${container} >/dev/null
    else
        showHelp
    fi
}

# ==============================================================================
# Showing the logs for the image
# ==============================================================================
function doLogs() {
    if (exists && running); then
        echo "Showing the ${container} logs"
        docker logs -f ${container}
    else
        showHelp
    fi
}

# ==============================================================================
# Shows status for the image
# ==============================================================================
function doStatus() {
    if (exists && running); then
        echo "Current status of the ${container} container"
        docker ps -f name=${container}
    else
        showHelp
    fi
}

# ==============================================================================
# Entering a running image
# ==============================================================================
function doInteractive() {
    if (exists && running); then
        echo "Entering the ${container} container"
        docker exec -it ${container} bash
    else
        showHelp
    fi
}

# ==============================================================================
# Remove a not running image
# ==============================================================================
function doRemove() {
    if (exists && ! running); then
        echo "Removing the ${container} container & image"
        docker rm ${container} && docker rmi ${image} >/dev/null
    else
        showHelp
    fi
}

# ==============================================================================
# Shows a progress bar until the Docker container is accessible
# ==============================================================================
function checkAlive() {
    echo -n "Waiting for the docker image to start "
    retries=0
    started=$(docker logs ${container} --since 2s 2>&1 | grep "WildFly Full" | grep "Started")
    while [[ ${#started} -eq 0 && ${retries} -lt 30 ]]; do
        echo -n "."
        sleep 1
        started=$(docker logs ${container} --since 2s 2>&1 | grep "WildFly Full" | grep "Started")
        retries=$((retries + 1))
    done
    echo

    if [[ ${retries} -le 30 ]]; then
        json=$(curl --silent --header "Content-Type: application/json" --request POST "http://localhost:${port}/eds/api/version")
        echo "${image^^} version available: $(inspectResponse "${json}" "version")"
    else
        echo "Error starting ${image^^} :-("
    fi
}

# ==============================================================================
# Main logic
# ==============================================================================
if [[ "${action}" = "build" ]]; then
    doBuild
elif [[ "${action}" = "start" ]]; then
    doStart
elif [[ "${action}" = "stop" ]]; then
    doStop
elif [[ "${action}" = "logs" ]]; then
    doLogs
elif [[ "${action}" = "status" ]]; then
    doStatus
elif [[ "${action}" = "it" || "${action}" = "interactive" ]]; then
    doInteractive
elif [[ "${action}" = "remove" ]]; then
    doRemove
else
    showHelp
fi
echo
