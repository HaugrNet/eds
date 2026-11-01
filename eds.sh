#!/bin/bash
# ==============================================================================
# EDS - Encrypted Data Share Control Script
# ------------------------------------------------------------------------------
# Builds EDS, runs the EDS container and the FitNesse test suite, and checks
# their status. Containers are built and run with rootless Podman. Run without
# arguments for the help.
# ==============================================================================

# Configuration
scriptDir="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
readonly scriptDir
readonly podmanDir="${scriptDir}/accessories/podman"
readonly containerFile="${podmanDir}/Containerfile"
readonly envFile="${podmanDir}/eds.env"
readonly logsDir="${podmanDir}/logs"
readonly targetDir="${scriptDir}/target"
readonly appJar="${scriptDir}/eds-quarkus/target/eds-runnable.jar"
readonly builderFile="${scriptDir}/build.container"
readonly builderImage="eds-builder:local"
readonly containerImage="eds-quarkus:local"
readonly containerName="eds-quarkus"
readonly containerSbom="${targetDir}/container-eds-quarkus-cyclonedx.json"
readonly fitnesseDir="${scriptDir}/eds-fitnesse"
readonly fitnesseJar="${fitnesseDir}/fitnesse-standalone.jar"
readonly fitnesseFixtures="${fitnesseDir}/target/eds-fitnesse.jar"
readonly fitnesseLog="${fitnesseDir}/target/fitnesse.log"
readonly PORT=${EDS_PORT:-8080}
readonly FITNESSE_PORT=${FITNESSE_PORT:-2080}

script="$(basename "${0}")"
readonly script

# ==============================================================================
# Help
# ==============================================================================
function showHelp() {
    cat <<EOF
EDS - Encrypted Data Share Control Script

  Usage: ${script} <command> [options]

  Build:
    build [options]          Maven build of all modules, tests are skipped
      --test                 Also run the tests
      --sbom                 Also generate the SBOMs and scan them for
                             vulnerabilities, fails on High or Critical
                             findings, like the CI Build
      --containers           Also build the EDS container image
      --podman               Run the Maven build in the Podman build
                             container (build.container), instead of
                             with the local Java; the other steps run
                             on the host

  Run & Stop:
    run container            Start the EDS container, and wait until ready
    run fitnesse             Start FitNesse in the background
    stop container           Stop the EDS container
      --remove               Also remove the container and image
    stop fitnesse            Stop FitNesse

  Check:
    check status             Status of the build, EDS container and FitNesse
    check logs               Follow the EDS container logs

  The EDS container uses the in-memory H2 database, unless EDS_DB_URL is set
  for PostgreSQL, either in the environment or in accessories/podman/eds.env,
  see RUNNING.md. EDS is reachable at port ${PORT} (EDS_PORT), FitNesse at
  port ${FITNESSE_PORT} (FITNESSE_PORT).

EOF
}

function usageError() {
    echo "${1}"
    echo "Run '${script}' without arguments for the help."
    return 1
}

# ==============================================================================
# Common helpers
# ==============================================================================
failures=()
logDir=""

function section() {
    echo
    echo "${1}"
}

function ok() {
    echo "  [OK]   ${1}"
}

function fail() {
    echo "  [FAIL] ${1}"
    failures+=("${1}")
}

# Formats the seconds since the given start time, e.g. "1m 23s"
function elapsed() {
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
function runStep() {
    local description="${1}"
    local log="${logDir}/${2}.log"
    shift 2

    local start=${SECONDS}
    if (cd "${scriptDir}" && "$@") > "${log}" 2>&1; then
        ok "${description} ($(elapsed "${start}"))"
        return 0
    fi

    fail "${description}"
    echo "         Last lines of ${log}:"
    tail -n 25 "${log}" | sed 's/^/         | /'
    return 1
}

# Verifies that the given tools are available
function requireTools() {
    local missing=()
    for tool in "$@"; do
        command -v "${tool}" > /dev/null 2>&1 || missing+=("${tool}")
    done

    if [[ ${#missing[@]} -eq 0 ]]; then
        ok "Tools: $*"
    else
        fail "Missing tools: ${missing[*]}"
        return 1
    fi
}

# Prints the summary, and returns the result
function summary() {
    echo
    if [[ ${#failures[@]} -eq 0 ]]; then
        echo "${1}"
        rm -rf "${logDir}"
        return 0
    fi

    echo "${#failures[@]} step(s) failed. Logs: ${logDir}"
    return 1
}

function isContainerRunning() {
    [[ "$(podman container inspect -f '{{.State.Running}}' "${containerName}" 2>/dev/null)" == "true" ]]
}

function containerExists() {
    podman container exists "${containerName}" 2>/dev/null
}

function imageExists() {
    podman image exists "${containerImage}" 2>/dev/null
}

# Builds the EDS container image from the uber-jar. The Docker format is
# used, as the OCI format doesn't support the HEALTHCHECK instruction.
function buildImage() {
    podman build -q --format docker -f "${containerFile}" -t "${containerImage}" "$(dirname "${appJar}")"
}

function fitnessePid() {
    pgrep -f "fitnesse-standalone.jar -p ${FITNESSE_PORT}"
}

# Prints the version of the running EDS, or nothing if it doesn't respond
function edsVersion() {
    curl -s -X POST -H "Content-Type: application/json" \
        "http://localhost:${PORT}/eds/version" -d '{}' 2>/dev/null \
        | grep -o '"version":"[^"]*"' | cut -d'"' -f4
}

# ==============================================================================
# Build
# ==============================================================================
function doBuild() {
    local withTest=false withSbom=false withContainers=false withPodman=false
    for option in "$@"; do
        case "${option}" in
            --test) withTest=true ;;
            --sbom) withSbom=true ;;
            --containers) withContainers=true ;;
            --podman) withPodman=true ;;
            *) usageError "Unknown build option: ${option}"; return 1 ;;
        esac
    done

    failures=()
    logDir=$(mktemp -d -t eds-build-XXXXXX)
    local start=${SECONDS}

    local tools=(java)
    [[ "${withPodman}" == true ]] && tools=(podman)
    [[ "${withContainers}" == true && "${withPodman}" != true ]] && tools+=(podman)
    [[ "${withSbom}" == true ]] && tools+=(grype jq)
    [[ "${withSbom}" == true && "${withContainers}" == true ]] && tools+=(syft)

    echo "EDS Build"
    section "Build"
    requireTools "${tools[@]}" || { summary; return 1; }

    local mvn=(mvn)
    [[ -x "${scriptDir}/mvnw" ]] && mvn=("${scriptDir}/mvnw")
    local mavenArgs=(-B -q -ntp)
    local description="Maven build"
    if [[ "${withPodman}" == true ]]; then
        prepareBuilder || { summary; return 1; }
        # Rootless Podman maps the container root to the current user, so
        # the build output on the host is owned by the current user. SELinux
        # labelling is disabled, rather than relabelling the project and the
        # Maven repository, which are also used outside the container.
        mvn=(podman run --rm --security-opt label=disable -v "${scriptDir}:/app" -v "${HOME}/.m2:/root/.m2" -w /app "${builderImage}" ./mvnw)
        description+=" in ${builderImage}"
    fi
    if [[ "${withTest}" == true ]]; then
        description+=" with tests"
    else
        mavenArgs+=(-DskipTests)
    fi
    if [[ "${withSbom}" == true ]]; then
        # The CycloneDX plugin is skipped when Maven runs offline
        mavenArgs+=(-Psbom)
        description+=" (-Psbom)"
    fi

    runStep "${description}" maven "${mvn[@]}" "${mavenArgs[@]}" clean install || { summary; return 1; }

    if [[ "${withContainers}" == true ]]; then
        runStep "Container image ${containerImage}" container buildImage || { summary; return 1; }
    fi

    if [[ "${withSbom}" == true ]]; then
        sbomChecks "${withContainers}"
    fi

    echo
    echo "Completed in $(elapsed "${start}")."
    summary "Build succeeded. Artifacts: ${targetDir#"${scriptDir}"/}/ and the module target folders."
}

# Builds the Podman build container image, unless it already exists. The
# project is mounted when the container runs, so the build context is empty.
function prepareBuilder() {
    if podman image exists "${builderImage}"; then
        ok "Build container ${builderImage} exists"
        return 0
    fi

    local context="${logDir}/context"
    mkdir -p "${context}" "${HOME}/.m2"
    runStep "Build container ${builderImage}" builder \
        podman build -q -f "${builderFile}" -t "${builderImage}" "${context}"
}

# ==============================================================================
# SBOM checks
# ------------------------------------------------------------------------------
# Validates the Software Bill of Materials (SBOM) from the Maven build, and
# for the container image, and scans them for vulnerabilities with Grype, the
# same way as the CI Build does. High or Critical findings fail the check,
# reviewed findings can be suppressed in .grype.yaml.
# ==============================================================================
function sbomChecks() {
    local withContainers="${1}"
    local candidates=("${targetDir}"/eds-*-cyclonedx.json)
    local mavenSbom=""
    [[ -e "${candidates[0]}" ]] && mavenSbom="${candidates[0]}"

    if [[ "${withContainers}" == true ]]; then
        # Syft reads the image from an archive, as it doesn't need a running
        # container engine that way. The archive is removed with the logs.
        local archive="${logDir}/${containerName}.tar"
        runStep "Image archive for ${containerImage}" save \
            podman save -q --format oci-archive -o "${archive}" "${containerImage}" &&
        runStep "Syft SBOM for ${containerImage}" syft \
            syft scan "oci-archive:${archive}" -q -o "cyclonedx-json=${containerSbom}"
    fi

    section "SBOMs"
    sbomValidate "Maven" "${mavenSbom:-${targetDir}/eds-<version>-cyclonedx.json}" "1.6"
    if [[ "${withContainers}" == true ]]; then
        sbomValidate "EDS image" "${containerSbom}" ""
        # The EDS image is built from the uber-jar, so it must also contain
        # the Java libraries, not only the operating system packages and JRE.
        if [[ -s "${containerSbom}" && "$(sbomCountMaven "${containerSbom}")" -eq 0 ]]; then
            fail "EDS image: no Java libraries found, the uber-jar was not detected"
        fi
    fi

    section "Vulnerabilities (fails on High or Critical)"
    sbomScan "Maven" "${mavenSbom}" "grype-report.json"
    if [[ "${withContainers}" == true ]]; then
        sbomScan "EDS image" "${containerSbom}" "grype-report-eds-container.json"
    fi
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
        fail "${label}: ${file#"${scriptDir}"/} is missing or empty"
        return
    fi

    if ! jq -e '.bomFormat == "CycloneDX"' "${file}" > /dev/null 2>&1; then
        fail "${label}: not a valid CycloneDX JSON document"
        return
    fi

    local actualSpec components
    actualSpec=$(jq -r '.specVersion' "${file}")
    components=$(jq '.components | length' "${file}")

    if [[ -n "${spec}" && "${actualSpec}" != "${spec}" ]]; then
        fail "${label}: CycloneDX ${actualSpec}, expected ${spec}"
    elif [[ "${components}" -eq 0 ]]; then
        fail "${label}: contains no components"
    else
        ok "${label}: CycloneDX ${actualSpec}, ${components} components ($(sbomCountMaven "${file}") Maven)"
    fi
}

# Vulnerability scan of one SBOM with Grype. Prints the number of findings
# per severity; High and Critical findings are listed and fail the check (the
# same as "grype --fail-on high").
#   $1 = label, $2 = SBOM file, $3 = report name
function sbomScan() {
    local label="${1}"
    local file="${2}"
    local report="${targetDir}/${3}"

    if [[ ! -s "${file}" ]]; then
        fail "${label}: no SBOM to scan"
        return
    fi

    # Run from the project root, so Grype picks up .grype.yaml
    local log="${logDir}/grype-${3%.json}.log"
    if ! (cd "${scriptDir}" && grype "sbom:${file}" -q -o "json=${report}") > "${log}" 2>&1; then
        fail "${label}: scan failed, see ${log}"
        return
    fi

    local counts severe
    counts=$(jq -r '[.matches[].vulnerability.severity] as $s
        | ["Critical", "High", "Medium", "Low", "Negligible", "Unknown"]
        | map(. as $level | {level: $level, n: ($s | map(select(. == $level)) | length)})
        | map(select(.n > 0) | "\(.n) \(.level)")
        | if length == 0 then "no findings" else join(", ") end' "${report}")
    severe=$(jq '[.matches[] | select(.vulnerability.severity == "Critical" or .vulnerability.severity == "High")] | length' "${report}")

    if [[ "${severe}" -eq 0 ]]; then
        ok "${label}: ${counts}"
    else
        fail "${label}: ${counts}"
        jq -r '[.matches[] | select(.vulnerability.severity == "Critical" or .vulnerability.severity == "High")
                | "\(.vulnerability.severity)\t\(.vulnerability.id)\t\(.artifact.name) \(.artifact.version)\t\(
                    if (.vulnerability.fix.versions | length) > 0 then "fixed in " + (.vulnerability.fix.versions | join(", ")) else "no fix" end)"]
            | unique | .[]' "${report}" | column -t -s $'\t' | sed 's/^/         /'
    fi
}

# ==============================================================================
# EDS container
# ==============================================================================
function runContainer() {
    if isContainerRunning; then
        echo "The EDS container is already running."
        return 0
    fi

    if ! imageExists; then
        if [[ ! -f "${appJar}" ]]; then
            echo "${appJar#"${scriptDir}"/} not found."
            echo "Run '${script} build --containers' first."
            return 1
        fi
        echo "Building the EDS container image ${containerImage}..."
        buildImage > /dev/null || return 1
    fi

    # A stopped container is replaced, so the current image and configuration
    # are always used. With H2 nothing is lost, with PostgreSQL all data is in
    # the database.
    podman rm "${containerName}" > /dev/null 2>&1

    mkdir -p "${logsDir}"

    # The "eds" user of the image (UID/GID 1001) is mapped to the host user,
    # so the logs are owned by the host user. The proxy settings of the host
    # are not passed on, as EDS only connects to its database and the
    # MasterKey URL. The :Z and :z options relabel the mounts for SELinux, and
    # are ignored without it.
    local args=(run -d --name "${containerName}" --http-proxy=false
        --userns=keep-id:uid=1001,gid=1001
        -p "${PORT}:8080" -p 8443:8443
        -v "${logsDir}:/app/logs:Z"
        -e TZ=Europe/Berlin
        -e QUARKUS_LOG_FILE_ENABLED=true
        -e QUARKUS_LOG_FILE_PATH=/app/logs/eds.log
        -e QUARKUS_LOG_FILE_ROTATION_MAX_FILE_SIZE=10M
        -e QUARKUS_LOG_FILE_ROTATION_MAX_BACKUP_INDEX=5)

    # The configuration file first, so the environment takes precedence.
    local certsDir="${EDS_CERTS_DIR:-}"
    if [[ -f "${envFile}" ]]; then
        args+=(--env-file "${envFile}")
        [[ -z "${certsDir}" ]] && certsDir=$(sed -n 's/^EDS_CERTS_DIR=//p' "${envFile}" | tail -n 1)
    fi
    # Passed by name, so the values, like the password, are not shown in the
    # process list.
    local variable
    for variable in EDS_DB_URL EDS_DB_USERNAME EDS_DB_PASSWORD EDS_DB_RETRY_INTERVAL \
                    EDS_TLS_CERT EDS_TLS_KEY EDS_INSECURE_REQUESTS; do
        [[ -n "${!variable+set}" ]] && args+=(-e "${variable}")
    done
    [[ -n "${certsDir}" ]] && args+=(-v "${certsDir}:/app/certs:ro,z")

    echo "Starting the EDS container..."
    podman "${args[@]}" "${containerImage}" > /dev/null || return 1

    waitForEds
}

function waitForEds() {
    echo -n "Waiting for EDS to start "
    local retries=0
    local maxRetries=60

    while [[ ${retries} -lt ${maxRetries} ]]; do
        local version
        version=$(edsVersion)
        if [[ -n "${version}" ]]; then
            echo
            echo "EDS ${version} is ready at: http://localhost:${PORT}/eds"
            return 0
        fi
        echo -n "."
        sleep 1
        retries=$((retries + 1))
    done

    echo
    echo "Error: EDS did not start within ${maxRetries} seconds."
    echo "Check the logs with: ${script} check logs"
    return 1
}

function stopContainer() {
    local remove=false
    for option in "$@"; do
        case "${option}" in
            --remove) remove=true ;;
            *) usageError "Unknown option: ${option}"; return 1 ;;
        esac
    done

    if [[ "${remove}" == true ]]; then
        echo "Stopping and removing the EDS container and image..."
        podman rm -f "${containerName}" > /dev/null 2>&1
        podman rmi "${containerImage}" > /dev/null 2>&1
        return 0
    elif isContainerRunning; then
        echo "Stopping the EDS container..."
        podman stop "${containerName}" > /dev/null
    else
        echo "The EDS container is not running."
    fi
}

# ==============================================================================
# FitNesse
# ==============================================================================
function runFitnesse() {
    if [[ -n "$(fitnessePid)" ]]; then
        echo "FitNesse is already running at: http://localhost:${FITNESSE_PORT}/EDS"
        return 0
    fi

    if [[ ! -f "${fitnesseJar}" ]]; then
        echo "The FitNesse standalone JAR is missing."
        echo "Please download it from https://fitnesse.org and save it as ${fitnesseJar#"${scriptDir}"/}"
        return 1
    fi

    if [[ ! -f "${fitnesseFixtures}" ]]; then
        echo "The EDS FitNesse fixtures are missing, run '${script} build' first."
        return 1
    fi

    # The tests expect a freshly started EDS, as the MasterKey is changed.
    if [[ -z "$(edsVersion)" ]]; then
        echo "Note: EDS is not responding at port ${PORT}, start it with '${script} run container'."
    fi

    echo "Starting FitNesse..."
    (cd "${fitnesseDir}" && nohup java -jar "${fitnesseJar}" -p "${FITNESSE_PORT}" > "${fitnesseLog}" 2>&1 &)

    local retries=0
    while [[ ${retries} -lt 30 ]]; do
        if curl -s -o /dev/null "http://localhost:${FITNESSE_PORT}/"; then
            echo "FitNesse is ready at: http://localhost:${FITNESSE_PORT}/EDS"
            return 0
        fi
        sleep 1
        retries=$((retries + 1))
    done

    echo "Error: FitNesse did not start within 30 seconds, see ${fitnesseLog#"${scriptDir}"/}"
    return 1
}

function stopFitnesse() {
    local pid
    pid=$(fitnessePid)
    if [[ -z "${pid}" ]]; then
        echo "FitNesse is not running."
        return 0
    fi

    echo "Stopping FitNesse..."
    # shellcheck disable=SC2086 # Multiple PIDs must be split
    kill ${pid}
}

# ==============================================================================
# Check
# ==============================================================================
function checkStatus() {
    echo "EDS Status"

    section "Build"
    if [[ -f "${appJar}" ]]; then
        ok "Runnable JAR: ${appJar#"${scriptDir}"/}"
    else
        echo "  [--]   Runnable JAR not found, run '${script} build'"
    fi
    if imageExists; then
        ok "Container image: ${containerImage}"
    else
        echo "  [--]   Container image not found, run '${script} build --containers'"
    fi

    section "EDS container"
    if isContainerRunning; then
        # The health endpoints are served on the management port 9000, which
        # is not published to the host, so the check runs in the container.
        local health version
        health=$(podman exec "${containerName}" wget -q -T 4 -O - "http://localhost:9000/q/health/ready" 2>/dev/null)
        if echo "${health}" | grep -qE '"status":\s*"UP"'; then
            ok "Health: UP"
        else
            echo "  [FAIL] Health: DOWN"
        fi
        version=$(edsVersion)
        if [[ -n "${version}" ]]; then
            ok "EDS ${version} at: http://localhost:${PORT}/eds"
        else
            echo "  [FAIL] EDS API is not responding at port ${PORT}"
        fi
    elif containerExists; then
        echo "  [--]   Stopped"
    else
        echo "  [--]   Not created"
    fi

    section "FitNesse"
    if [[ -n "$(fitnessePid)" ]]; then
        ok "Running at: http://localhost:${FITNESSE_PORT}/EDS"
    else
        echo "  [--]   Not running"
    fi
    echo
}

function checkLogs() {
    if ! containerExists; then
        echo "The EDS container does not exist, start it with '${script} run container'."
        return 1
    fi

    echo "Following the EDS container logs (Ctrl+C to exit)..."
    podman logs -f "${containerName}"
}

# ==============================================================================
# Main
# ==============================================================================
command="${1:-}"
target="${2:-}"
shift 2 2>/dev/null || shift $#

case "${command} ${target}" in
    "build "*)
        # "build" has no target, only options
        [[ -n "${target}" ]] && set -- "${target}" "$@"
        doBuild "$@"
        ;;
    "run container")
        runContainer
        ;;
    "run fitnesse")
        runFitnesse
        ;;
    "stop container")
        stopContainer "$@"
        ;;
    "stop fitnesse")
        stopFitnesse
        ;;
    "check status")
        checkStatus
        ;;
    "check logs")
        checkLogs
        ;;
    " ")
        showHelp
        ;;
    *)
        showHelp
        usageError "Unknown command: ${command} ${target}"
        ;;
esac
