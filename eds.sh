#!/bin/bash
# ==============================================================================
# EDS - Encrypted Data Share Control Script
# ------------------------------------------------------------------------------
# Builds EDS (the server) and the stone alone, and controls the EDS
# container: start, stop, restart, status and logs. The container is built and run with rootless Podman. Run without
# arguments for the help.
# ==============================================================================

# Configuration
scriptDir="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
readonly scriptDir
readonly podmanDir="${scriptDir}/accessories/podman"
readonly containerFile="${podmanDir}/Containerfile"
readonly envFile="${podmanDir}/eds.env"
readonly masterKeyFile="${EDS_MASTERKEY_FILE:-${podmanDir}/masterkey}"
readonly masterKeyMount="/run/secrets/eds-masterkey"
readonly logsDir="${podmanDir}/logs"
readonly targetDir="${scriptDir}/target"
readonly appJar="${scriptDir}/eds-quarkus/target/eds-rune.jar"
readonly builderFile="${scriptDir}/build.container"
readonly builderImage="eds-builder:local"
readonly containerImage="eds-quarkus:local"
readonly containerName="eds-quarkus"
readonly containerSbom="${targetDir}/container-eds-quarkus-cyclonedx.json"
# The stone alone: the server built as a JAR and an image of its own (the
# Maven property stone), which a host runs.
readonly stoneJar="${scriptDir}/eds-quarkus/target/eds-stone.jar"
readonly stoneImage="eds-stone:local"
readonly stoneSbom="${targetDir}/container-eds-stone-cyclonedx.json"
readonly fitnesseDir="${scriptDir}/eds-testing"
readonly fitnesseJar="${fitnesseDir}/fitnesse-standalone.jar"
readonly fitnesseFixtures="${fitnesseDir}/target/eds-testing.jar"
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
    build [what...] [options]    Build everything, or only what is named;
                                 the tests are skipped
      eds                        The server: all Maven modules, and the
                                 container image that serves it
      stone                      The stone alone: a JAR and a container
                                 image of their own, which a host runs

        --test                   Also run the tests of what is built; with
                                 eds, SonarQube then analyses the code and
                                 the build fails unless its quality gate
                                 passes (needs SONAR_HOST_URL, SONAR_TOKEN)
        --no-sonar               Run the tests without SonarQube
        --sbom                   Also generate the SBOMs and scan them for
                                 vulnerabilities, fails on High or Critical
                                 findings, like the CI Build (needs eds)
        --podman                 Run the Maven build in the Podman build
                                 container (build.container), instead of
                                 with the local Java; the other steps run
                                 on the host

  Control the EDS container, which serves the API, or the stone alone:
    start [stone] [--masterkey]  Start it, and wait until EDS is ready; the
                                 image is built first, if it is missing or
                                 not built from the present JAR
                                 Optionally, start the stone alone ('eds'
                                 names the other)
                                 Optionally, start it with the MasterKey file
                                 as its source, so EDS is unlocked at startup
    stop [--remove]              Stop it, optionally also remove the container
                                 and both images
    restart                      Stop and start it, with the options of start;
                                 it stays what it was, unless 'stone' or
                                 'eds' is named
    status                       Status of the build, the container, EDS
                                 and FitNesse
    logs                         Follow the logs of the container

  MasterKey:
    masterkey generate [--force] Create a random MasterKey secret in the
                                 MasterKey file (only readable by you).
                                 Optionally, replace an existing file; data
                                 protected by the old MasterKey can no longer
                                 be opened
    masterkey load               Send the MasterKey file to a running, locked
                                 EDS, as the System Administrator: asks for
                                 its account name and passphrase, or takes
                                 EDS_ADMIN_ACCOUNT and EDS_ADMIN_PASSPHRASE.
                                 On a new EDS, the name given becomes it

  FitNesse:
    fitnesse start               Start the FitNesse test suite in the
                                 background
    fitnesse stop                Stop it

  Install:
    install [--remove]           Put eds.sh on the PATH, in ~/.local/bin,
                                 with its bash completion.
                                 ~/.bashrc is only changed if ~/.local/bin
                                 isn't on the PATH or bash-completion is
                                 missing. Optionally, remove all of it again
    completion                   Print the bash completion of this script,
                                 for: source <(/path/to/eds.sh completion)

  Examples:
    ${script} build --test            Everything, with the tests and
                                      SonarQube
    ${script} build stone             The stone alone and its container image
    ${script} build eds && ${script} restart
    ${script} build stone && ${script} restart stone

  The EDS container uses the in-memory H2 database, unless EDS_DB_URL is set
  for PostgreSQL, either in the environment or in accessories/podman/eds.env,
  see RUNNING.md. EDS is reachable at port ${PORT} (EDS_PORT), FitNesse at
  port ${FITNESSE_PORT} (FITNESSE_PORT). The MasterKey file is
  ${masterKeyFile#"${scriptDir}"/} (EDS_MASTERKEY_FILE). Keep a safe copy:
  without it, the stored data can't be read.

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

# There are two images, each from its own JAR: "eds" is EDS, and "stone" the
# stone alone. The functions below take one of the two words.
function imageOf() {
    if [[ "${1}" == stone ]]; then echo "${stoneImage}"; else echo "${containerImage}"; fi
}

function jarOf() {
    if [[ "${1}" == stone ]]; then echo "${stoneJar}"; else echo "${appJar}"; fi
}

function kindName() {
    if [[ "${1}" == stone ]]; then echo "the stone alone"; else echo "EDS"; fi
}

# What the EDS container was created from: stone or eds
function containerKind() {
    local image
    image=$(podman container inspect -f '{{.ImageName}}' "${containerName}" 2>/dev/null)
    if [[ "${image}" == *"${stoneImage}" ]]; then echo stone; else echo eds; fi
}

function imageExists() {
    podman image exists "$(imageOf "${1}")" 2>/dev/null
}

# Label of the image with the modification time of the JAR it was built
# from, to tell if the image holds the latest build.
readonly jarLabel="net.haugr.eds.jar-modified"

# Builds a container image from its uber-jar. The Docker format is used, as
# the OCI format doesn't support the HEALTHCHECK instruction.
function buildImage() {
    local jar
    jar=$(jarOf "${1}")
    podman build -q --format docker -f "${containerFile}" -t "$(imageOf "${1}")" \
        --build-arg "JAR=$(basename "${jar}")" \
        --label "${jarLabel}=$(stat -c %Y "${jar}")" "$(dirname "${jar}")"
}

# Analyses the code just built and tested with SonarQube, and waits for
# the quality gate: anything new it finds fails the build. The scanner reads
# SONAR_HOST_URL and SONAR_TOKEN from the environment. Its engine is a JVM of
# its own, which doesn't take Maven's proxy, so the proxy of the environment
# is handed to it.
function sonarScan() {
    local mvn=(mvn)
    [[ -x "${scriptDir}/mvnw" ]] && mvn=("${scriptDir}/mvnw")
    local args=(-B -ntp -Dsonar.qualitygate.wait=true)
    local proxy="${HTTPS_PROXY:-${https_proxy:-}}"
    if [[ -n "${proxy}" ]]; then
        proxy="${proxy#*://}"
        proxy="${proxy%%/*}"
        proxy="${proxy##*@}"
        args+=("-Dsonar.scanner.proxyHost=${proxy%:*}" "-Dsonar.scanner.proxyPort=${proxy##*:}")
    fi
    "${mvn[@]}" "${args[@]}" sonar:sonar
}

# A new image takes the tag from the one before, which is then left without
# a name. Those are removed, but only images of EDS (they carry the label of
# the JAR), so images of other projects stay; one a container still uses is
# kept by Podman.
function pruneImages() {
    podman image prune -f --filter "label=${jarLabel}"
}

# True if the image was built from another JAR than the present one, so it
# doesn't hold what was last built.
function imageOutdated() {
    local jar modified
    jar=$(jarOf "${1}")
    [[ -f "${jar}" ]] || return 1
    modified=$(podman image inspect -f "{{index .Config.Labels \"${jarLabel}\"}}" "$(imageOf "${1}")" 2>/dev/null) || return 1
    [[ "${modified}" != "$(stat -c %Y "${jar}")" ]]
}

function fitnessePid() {
    pgrep -f "fitnesse-standalone.jar -p ${FITNESSE_PORT}"
}

# Prints the version of the running EDS, or nothing if it doesn't respond
function edsVersion() {
    curl -s -X POST -H "Content-Type: application/json" \
        "http://localhost:${PORT}/api/v1/version" -d '{}' 2>/dev/null \
        | grep -o '"version":"[^"]*"' | cut -d'"' -f4
}

# ==============================================================================
# Build
# ==============================================================================
function doBuild() {
    local withTest=false withSbom=false withPodman=false withSonar=true
    local buildEds=false buildStone=false named=false
    for option in "$@"; do
        case "${option}" in
            eds) buildEds=true; named=true ;;
            stone) buildStone=true; named=true ;;
            all) buildEds=true; buildStone=true; named=true ;;
            --test) withTest=true ;;
            --sbom) withSbom=true ;;
            --no-sonar) withSonar=false ;;
            --podman) withPodman=true ;;
            *) usageError "Unknown build option: ${option}"; return 1 ;;
        esac
    done

    if [[ "${named}" != true ]]; then
        buildEds=true
        buildStone=true
    fi
    # The SBOM of the project is aggregated over all Maven modules.
    if [[ "${withSbom}" == true && "${buildEds}" != true ]]; then
        usageError "--sbom needs the build of eds."
        return 1
    fi

    # SonarQube reads the coverage of the tests of all modules together, so
    # it only runs with the tests of eds.
    [[ "${withTest}" == true && "${buildEds}" == true ]] || withSonar=false
    if [[ "${withSonar}" == true && ( -z "${SONAR_HOST_URL:-}" || -z "${SONAR_TOKEN:-}" ) ]]; then
        usageError "SonarQube needs SONAR_HOST_URL and SONAR_TOKEN in the environment, or --no-sonar."
        return 1
    fi

    local parts=()
    [[ "${buildEds}" == true ]] && parts+=(EDS)
    [[ "${buildStone}" == true ]] && parts+=("the stone alone")
    local what
    what=$(IFS=,; echo "${parts[*]}")
    what="${what//,/, }"

    failures=()
    logDir=$(mktemp -d -t eds-build-XXXXXX)
    local start=${SECONDS}

    # The images which are built: of EDS, and of the stone alone
    local images=()
    [[ "${buildEds}" == true ]] && images+=(eds)
    [[ "${buildStone}" == true ]] && images+=(stone)

    local tools=(java)
    [[ "${withPodman}" == true ]] && tools=(podman)
    [[ ${#images[@]} -gt 0 && "${withPodman}" != true ]] && tools+=(podman)
    [[ "${withSbom}" == true ]] && tools+=(grype jq syft)
    # SonarQube runs with the local Java, also after a build with --podman
    [[ "${withSonar}" == true && "${withPodman}" == true ]] && tools+=(java)

    echo "EDS Build: ${what}"
    section "Build"
    requireTools "${tools[@]}" || { summary; return 1; }

    local mvn=(mvn)
    [[ -x "${scriptDir}/mvnw" ]] && mvn=("${scriptDir}/mvnw")
    local mavenArgs=(-B -q -ntp)
    # The stone alone is a build of its own, after the others
    local others=()
    [[ "${buildEds}" == true ]] && others+=(EDS)
    local description
    description="Maven build of $(IFS=,; echo "${others[*]}")"
    description="${description//,/, }"
    if [[ "${withPodman}" == true ]]; then
        prepareBuilder || { summary; return 1; }
        # Rootless Podman maps the container root to the current user, so
        # the build output on the host is owned by the current user. SELinux
        # labelling is disabled, rather than relabelling the project and the
        # Maven repository, which are also used outside the container.
        mvn=(podman run --rm --security-opt label=disable -v "${scriptDir}:/app" -v "${HOME}/.m2:/root/.m2" -w /app "${builderImage}" ./mvnw)
        description+=" in ${builderImage}"
    fi

    # EDS is all the Maven modules. The stone alone is eds-quarkus built once
    # more (-Dstone), in a folder of its own, so both JARs are kept. It is
    # only packaged. The API and the core are built with it, unless the build
    # before it did so.
    local stoneArgs=(-B -q -ntp -Dstone -pl eds-quarkus)
    local stoneDescription="Maven build of the stone alone"
    [[ "${buildEds}" == true ]] || stoneArgs+=(-am)
    if [[ "${withTest}" == true ]]; then
        description+=", with tests"
        stoneDescription+=", with tests"
    else
        mavenArgs+=(-DskipTests)
        stoneArgs+=(-DskipTests)
    fi
    if [[ "${withPodman}" == true ]]; then
        stoneDescription+=" in ${builderImage}"
    fi
    if [[ "${withSbom}" == true ]]; then
        # The CycloneDX plugin is skipped when Maven runs offline
        mavenArgs+=(-Psbom)
        description+=" (-Psbom)"
    fi

    if [[ ${#others[@]} -gt 0 ]]; then
        runStep "${description}" maven "${mvn[@]}" "${mavenArgs[@]}" clean install || { summary; return 1; }
    fi
    # Before the stone alone, whose clean build of eds-quarkus removes the
    # merged coverage report
    if [[ "${withSonar}" == true ]]; then
        runStep "SonarQube: analysis and quality gate" sonar sonarScan || { summary; return 1; }
    fi
    if [[ "${buildStone}" == true ]]; then
        runStep "${stoneDescription}" maven-stone "${mvn[@]}" "${stoneArgs[@]}" clean package || { summary; return 1; }
    fi

    # The images of EDS and of the stone alone
    local kind
    for kind in "${images[@]}"; do
        runStep "Container image $(imageOf "${kind}")" "container-${kind}" buildImage "${kind}" || { summary; return 1; }
    done
    if [[ ${#images[@]} -gt 0 ]]; then
        runStep "Earlier images of EDS removed" prune-images pruneImages || { summary; return 1; }
    fi

    if [[ "${withSbom}" == true ]]; then
        sbomChecks "${images[@]}"
    fi

    section "Artifacts"
    if [[ "${buildEds}" == true ]]; then
        echo "  EDS JAR:          ${appJar#"${scriptDir}"/}"
        echo "  Container image:  ${containerImage}"
    fi
    if [[ "${buildStone}" == true ]]; then
        echo "  Stone JAR:        ${stoneJar#"${scriptDir}"/}"
        echo "  Container image:  ${stoneImage}"
    fi
    if isContainerRunning && [[ " ${images[*]} " == *" $(containerKind) "* ]]; then
        echo
        echo "The running EDS container still uses the old image, '${script} restart' replaces it."
    fi

    echo
    echo "Completed in $(elapsed "${start}")."
    summary "Build succeeded."
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
# Validates the Software Bill of Materials (SBOM) from the Maven build and for
# the container images, and scans them for
# vulnerabilities with Grype, the same way as the CI Build does. High or
# Critical findings fail the check, reviewed findings can be suppressed in
# .grype.yaml.
# ==============================================================================
function sbomChecks() {
    # $@ = the images which were built: eds, stone, both or none
    local kinds=("$@")
    local candidates=("${targetDir}"/eds-*-cyclonedx.json)
    local mavenSbom=""
    [[ -e "${candidates[0]}" ]] && mavenSbom="${candidates[0]}"

    local kind image sbom label
    for kind in "${kinds[@]}"; do
        # Syft reads the image from an archive, as it doesn't need a running
        # container engine that way. The archive is removed with the logs.
        image=$(imageOf "${kind}")
        local archive="${logDir}/eds-${kind}.tar"
        runStep "Image archive for ${image}" "save-${kind}" \
            podman save -q --format oci-archive -o "${archive}" "${image}" &&
        runStep "Syft SBOM for ${image}" "syft-${kind}" \
            syft scan "oci-archive:${archive}" -q -o "cyclonedx-json=$(sbomOf "${kind}")"
    done

    section "SBOMs"
    sbomValidate "Maven" "${mavenSbom:-${targetDir}/eds-<version>-cyclonedx.json}" "1.6"
    for kind in "${kinds[@]}"; do
        sbom=$(sbomOf "${kind}")
        label=$(sbomLabel "${kind}")
        sbomValidate "${label}" "${sbom}" ""
        # The image is built from the uber-jar, so it must also contain the
        # Java libraries, not only the operating system packages and JRE.
        if [[ -s "${sbom}" && "$(sbomCountMaven "${sbom}")" -eq 0 ]]; then
            fail "${label}: no Java libraries found, the uber-jar was not detected"
        fi
    done

    section "Vulnerabilities (fails on High or Critical)"
    sbomScan "Maven" "${mavenSbom}" "grype-report.json"
    for kind in "${kinds[@]}"; do
        if [[ "${kind}" == stone ]]; then
            sbomScan "$(sbomLabel stone)" "${stoneSbom}" "grype-report-eds-stone-container.json"
        else
            sbomScan "$(sbomLabel eds)" "${containerSbom}" "grype-report-eds-container.json"
        fi
    done
}

function sbomOf() {
    if [[ "${1}" == stone ]]; then echo "${stoneSbom}"; else echo "${containerSbom}"; fi
}

function sbomLabel() {
    if [[ "${1}" == stone ]]; then echo "Stone image"; else echo "EDS image"; fi
}

# Counts the components with a Maven package URL in an SBOM
function sbomCountMaven() {
    sbomCount "${1}" maven
}

# Counts the components with a package URL of one type in an SBOM
#   $1 = SBOM file, $2 = package URL type (maven, npm, ...)
function sbomCount() {
    jq --arg type "pkg:${2}/" '[.components[]? | select(.purl? // "" | startswith($type))] | length' "${1}"
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
        local npm
        npm=$(sbomCount "${file}" npm)
        if [[ "${npm}" -gt 0 ]]; then
            ok "${label}: CycloneDX ${actualSpec}, ${components} components (${npm} npm)"
        else
            ok "${label}: CycloneDX ${actualSpec}, ${components} components ($(sbomCountMaven "${file}") Maven)"
        fi
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
function startContainer() {
    local withMasterKey=false kind=eds
    for option in "$@"; do
        case "${option}" in
            --masterkey) withMasterKey=true ;;
            stone | eds) kind="${option}" ;;
            *) usageError "Unknown option: ${option}"; return 1 ;;
        esac
    done
    local image jar
    image=$(imageOf "${kind}")
    jar=$(jarOf "${kind}")
    if [[ "${withMasterKey}" == true && ! -s "${masterKeyFile}" ]]; then
        echo "The MasterKey file ${masterKeyFile#"${scriptDir}"/} doesn't exist."
        echo "Create one with '${script} masterkey generate'."
        return 1
    fi

    if isContainerRunning; then
        local running
        running=$(containerKind)
        echo "The EDS container is already running: $(kindName "${running}")."
        if [[ "${running}" != "${kind}" ]]; then
            echo "'${script} restart ${kind}' replaces it with $(kindName "${kind}")."
        fi
        return 0
    fi

    # The image is built here too, if the JAR was built without it
    # ('${script} build eds'), so the container always runs the latest build.
    if ! imageExists "${kind}" || imageOutdated "${kind}"; then
        if [[ ! -f "${jar}" ]]; then
            echo "${jar#"${scriptDir}"/} not found."
            if [[ "${kind}" == stone ]]; then
                echo "Run '${script} build stone' first."
            else
                echo "Run '${script} build' first."
            fi
            return 1
        fi
        echo "Building the EDS container image ${image}..."
        buildImage "${kind}" > /dev/null || return 1
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
    # The MasterKey file is mounted read-only and used as the MasterKey
    # source; the secret itself is never part of the command line.
    if [[ "${withMasterKey}" == true ]]; then
        args+=(-v "${masterKeyFile}:${masterKeyMount}:ro,z" -e "EDS_MASTERKEY_URL=file://${masterKeyMount}")
    fi

    echo "Starting the EDS container: $(kindName "${kind}")..."
    podman "${args[@]}" "${image}" > /dev/null || return 1

    waitForEds "${kind}"
}

# Where the container is reached
function servedAt() {
    if [[ "${1}" == stone ]]; then
        echo "the API at http://localhost:${PORT}/api/v1 (the stone alone)"
    else
        echo "the API at http://localhost:${PORT}/api/v1"
    fi
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
            echo "EDS ${version} is ready: $(servedAt "${1:-eds}")"
            return 0
        fi
        echo -n "."
        sleep 1
        retries=$((retries + 1))
    done

    echo
    echo "Error: EDS did not start within ${maxRetries} seconds."
    echo "Check the logs with: ${script} logs"
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
        echo "Stopping and removing the EDS container and its images..."
        podman rm -f "${containerName}" > /dev/null 2>&1
        podman rmi "${containerImage}" "${stoneImage}" > /dev/null 2>&1
        return 0
    elif isContainerRunning; then
        echo "Stopping the EDS container..."
        podman stop "${containerName}" > /dev/null
    else
        echo "The EDS container is not running."
    fi
}

# Stops and starts the container, with the options of start. The container
# is replaced at the start, so a new image and configuration are used. It
# stays what it was, the stone alone or EDS, unless one is named.
function restartContainer() {
    local options=("$@")
    if containerExists && [[ " $* " != *" stone "* && " $* " != *" eds "* ]]; then
        options+=("$(containerKind)")
    fi
    stopContainer || return 1
    startContainer "${options[@]}"
}

# ==============================================================================
# FitNesse
# ==============================================================================
function startFitnesse() {
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
        echo "The EDS FitNesse fixtures are missing, run '${script} build eds' first."
        return 1
    fi

    # The tests expect a freshly started EDS, as the MasterKey is changed.
    if [[ -z "$(edsVersion)" ]]; then
        echo "Note: EDS is not responding at port ${PORT}, start it with '${script} start'."
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
# MasterKey
# ==============================================================================

# Creates a random MasterKey secret: 48 random bytes, Base64 encoded (64
# characters, 384 bits), well above the entropy EDS requires. EDS uses the
# bytes of the file as they are, so no newline is written.
function generateMasterKey() {
    local force=false
    for option in "$@"; do
        case "${option}" in
            --force) force=true ;;
            *) usageError "Unknown option: ${option}"; return 1 ;;
        esac
    done

    if [[ -e "${masterKeyFile}" && "${force}" != true ]]; then
        echo "The MasterKey file ${masterKeyFile#"${scriptDir}"/} already exists, it is kept."
        echo "Replacing it makes the data protected by it unreadable; use --force if that is intended."
        return 1
    fi

    local secret
    secret=$(head -c 48 /dev/urandom | base64 -w0) || return 1
    mkdir -p "$(dirname "${masterKeyFile}")"
    (umask 077 && printf '%s' "${secret}" > "${masterKeyFile}") || return 1
    chmod 600 "${masterKeyFile}"
    echo "Created the MasterKey file ${masterKeyFile#"${scriptDir}"/} (readable only by you)."
    echo "Keep a safe copy of it. Start EDS with it: '${script} start --masterkey',"
    echo "or send it to a running EDS: '${script} masterkey load'."
}

# Sends the MasterKey file to a running EDS with the MasterKey request. On a
# new EDS, it becomes the first MasterKey, and the admin account is created
# with the given passphrase; later, it unlocks EDS after a start without a
# source. The secret and the passphrase are sent via stdin, so they don't
# show in the process list.
function loadMasterKey() {
    if [[ ! -s "${masterKeyFile}" ]]; then
        echo "The MasterKey file ${masterKeyFile#"${scriptDir}"/} doesn't exist."
        echo "Create one with '${script} masterkey generate'."
        return 1
    fi
    if [[ -z "$(edsVersion)" ]]; then
        echo "EDS is not responding at port ${PORT}, start it first."
        return 1
    fi

    # A stone sets no name for its System Administrator: on a new stone the
    # name given here becomes it, later it is the name it has.
    local account="${EDS_ADMIN_ACCOUNT:-}"
    if [[ -z "${account}" ]]; then
        read -r -p "Account name of the System Administrator (a new stone gives it this name) [admin]: " account
        account="${account:-admin}"
    fi
    if [[ "${account}" == *'"'* || "${account}" == *'\'* ]]; then
        echo "The account name can't hold a quote or a backslash here."
        return 1
    fi
    local passphrase="${EDS_ADMIN_PASSPHRASE:-}"
    if [[ -z "${passphrase}" ]]; then
        read -r -s -p "Passphrase of ${account}: " passphrase
        echo
    fi
    if [[ -z "${passphrase}" ]]; then
        echo "No passphrase given."
        return 1
    fi

    local response returnCode message
    response=$(printf '{"accountName":"%s","credential":"%s","secret":"%s"}' "${account}"             "$(printf '%s' "${passphrase}" | base64 -w0)" "$(base64 -w0 < "${masterKeyFile}")"         | curl -s -X POST -H "Content-Type: application/json" --data-binary @- "http://localhost:${PORT}/api/v1/masterKey")
    returnCode=$(echo "${response}" | grep -o '"returnCode":[0-9]*' | cut -d: -f2)
    message=$(echo "${response}" | grep -o '"returnMessage":"[^"]*"' | cut -d'"' -f4)

    if [[ "${returnCode}" == "200" ]]; then
        echo "${message:-MasterKey loaded.}"
        return 0
    fi
    echo "The MasterKey was not loaded: ${message:-no answer from EDS} (${returnCode:-?})"
    return 1
}

# ==============================================================================
# Status and logs
# ==============================================================================
function showStatus() {
    echo "EDS Status"

    section "Build"
    if [[ -f "${appJar}" ]]; then
        ok "EDS JAR: ${appJar#"${scriptDir}"/}"
    else
        echo "  [--]   EDS JAR not found, run '${script} build'"
    fi
    if ! imageExists eds; then
        echo "  [--]   Container image not found, run '${script} build eds'"
    elif imageOutdated eds; then
        echo "  [--]   Container image ${containerImage} was not built from the present EDS JAR, '${script} start' rebuilds it"
    else
        ok "Container image: ${containerImage}"
    fi
    if [[ -f "${stoneJar}" ]]; then
        ok "Stone JAR: ${stoneJar#"${scriptDir}"/}"
    else
        echo "  [--]   Stone JAR not found, run '${script} build stone'"
    fi
    if ! imageExists stone; then
        echo "  [--]   Container image of the stone alone not found, run '${script} build stone'"
    elif imageOutdated stone; then
        echo "  [--]   Container image ${stoneImage} was not built from the present Stone JAR, '${script} start stone' rebuilds it"
    else
        ok "Container image: ${stoneImage}"
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
            ok "EDS ${version}: $(servedAt "$(containerKind)")"
        else
            echo "  [FAIL] EDS API is not responding at port ${PORT}"
        fi
    elif containerExists; then
        echo "  [--]   Stopped, start it with '${script} start'"
    else
        echo "  [--]   Not created, start it with '${script} start'"
    fi

    section "FitNesse"
    if [[ -n "$(fitnessePid)" ]]; then
        ok "Running at: http://localhost:${FITNESSE_PORT}/EDS"
    else
        echo "  [--]   Not running"
    fi
    echo
}

function showLogs() {
    if ! containerExists; then
        echo "The EDS container does not exist, start it with '${script} start'."
        return 1
    fi

    echo "Following the EDS container logs (Ctrl+C to exit)..."
    podman logs -f "${containerName}"
}

# ==============================================================================
# Install
# ==============================================================================
readonly binDir="${HOME}/.local/bin"
readonly completionDir="${XDG_DATA_HOME:-${HOME}/.local/share}/bash-completion/completions"
readonly bashrcFile="${HOME}/.bashrc"
readonly bashrcBegin="# >>> eds.sh install >>>"
readonly bashrcEnd="# <<< eds.sh install <<<"

# Whether bash-completion is there, which loads the completion of a command
# from the user's completion directory when the command is first completed
function hasBashCompletion() {
    local file
    for file in /usr/share/bash-completion/bash_completion /etc/bash_completion \
        /opt/homebrew/etc/profile.d/bash_completion.sh /usr/local/etc/profile.d/bash_completion.sh; do
        [[ -r "${file}" ]] && return 0
    done
    return 1
}

# Removes the block of this script from ~/.bashrc
function removeBashrcBlock() {
    [[ -f "${bashrcFile}" ]] && grep -qF "${bashrcBegin}" "${bashrcFile}" || return 0
    sed -i.eds-backup "/^${bashrcBegin}\$/,/^${bashrcEnd}\$/d" "${bashrcFile}" && rm -f "${bashrcFile}.eds-backup"
}

# Puts eds.sh on the PATH, in ~/.local/bin, with its bash completion. eds.sh
# is linked, so it stays the script of this checkout. ~/.bashrc is only changed when ~/.local/bin isn't
# on the PATH, or bash-completion isn't there to load the completions.
function doInstall() {
    local remove=false
    for option in "$@"; do
        case "${option}" in
            --remove) remove=true ;;
            *) usageError "Unknown option: ${option}"; return 1 ;;
        esac
    done
    if [[ "${remove}" == true ]]; then
        uninstall
        return
    fi

    section "eds.sh"
    mkdir -p "${binDir}" "${completionDir}"
    ln -sfn "${scriptDir}/eds.sh" "${binDir}/eds.sh"
    ok "Linked as ${binDir}/eds.sh"
    showCompletion > "${completionDir}/eds.sh"
    ok "Bash completion: ${completionDir}/eds.sh"

    section "Shell"
    local lines=()
    [[ ":${PATH}:" == *":${binDir}:"* ]] \
        || lines+=("case \":\${PATH}:\" in *\":${binDir}:\"*) ;; *) export PATH=\"${binDir}:\${PATH}\" ;; esac")
    hasBashCompletion \
        || lines+=("[[ -r \"${completionDir}/eds.sh\" ]] && source \"${completionDir}/eds.sh\"")
    removeBashrcBlock
    if [[ ${#lines[@]} -eq 0 ]]; then
        ok "${binDir} is on the PATH, and bash-completion loads the completion"
    else
        {
            echo "${bashrcBegin}"
            echo "# Added by 'eds.sh install'; 'eds.sh install --remove' takes it out again."
            printf '%s\n' "${lines[@]}"
            echo "${bashrcEnd}"
        } >> "${bashrcFile}"
        local added=()
        [[ ":${PATH}:" == *":${binDir}:"* ]] || added+=("${binDir} on the PATH")
        hasBashCompletion || added+=("the completion")
        ok "Added to ${bashrcFile}: $(IFS=,; echo "${added[*]}" | sed 's/,/, /g')"
    fi
    echo
    echo "Installed. In a new shell, or after 'source ~/.bashrc', 'eds.sh' completes with Tab."
}

function uninstall() {
    section "Removing what '${script} install' put in place"
    rm -f "${binDir}/eds.sh" "${completionDir}/eds.sh"
    ok "eds.sh and its completion"
    removeBashrcBlock
    ok "The block in ${bashrcFile}, if there was one"
}

# ==============================================================================
# Bash completion
# ==============================================================================

# Prints the bash completion of the script, for: source <(./eds.sh completion)
# bash looks the script up by the name after the last slash, so it completes
# ./eds.sh and a full path as well. Words already given aren't offered again.
function showCompletion() {
    cat <<'EOF'
_eds_sh() {
    local cur="${COMP_WORDS[COMP_CWORD]}" words=() given w
    if [[ ${COMP_CWORD} -eq 1 ]]; then
        words=(build start stop restart status logs masterkey fitnesse install completion)
    else
        case "${COMP_WORDS[1]}" in
            build) words=(eds stone all --test --sbom --podman)
                   [[ " ${COMP_WORDS[*]} " == *" --test "* ]] && words+=(--no-sonar) ;;
            start | restart) words=(stone eds --masterkey)
                   [[ " ${COMP_WORDS[*]} " == *" stone "* || " ${COMP_WORDS[*]} " == *" eds "* ]] \
                       && words=(--masterkey) ;;
            stop | install) words=(--remove) ;;
            masterkey)
                if [[ ${COMP_CWORD} -eq 2 ]]; then words=(generate load)
                elif [[ "${COMP_WORDS[2]}" == generate ]]; then words=(--force); fi ;;
            fitnesse) [[ ${COMP_CWORD} -eq 2 ]] && words=(start stop) ;;
        esac
        given=" ${COMP_WORDS[*]:2:COMP_CWORD-2} "
        local left=()
        for w in "${words[@]}"; do
            [[ "${given}" == *" ${w} "* ]] || left+=("${w}")
        done
        words=("${left[@]}")
    fi
    mapfile -t COMPREPLY < <(compgen -W "${words[*]}" -- "${cur}")
}
complete -F _eds_sh eds.sh
EOF
}

# ==============================================================================
# Main
# ==============================================================================
command="${1:-}"
[[ $# -gt 0 ]] && shift

# Commands without options
function noOptions() {
    [[ $# -eq 0 ]] || usageError "Unknown option: ${1}"
}

case "${command}" in
    build)
        doBuild "$@"
        ;;
    start)
        startContainer "$@"
        ;;
    stop)
        stopContainer "$@"
        ;;
    restart)
        restartContainer "$@"
        ;;
    status)
        noOptions "$@" && showStatus
        ;;
    logs)
        noOptions "$@" && showLogs
        ;;
    masterkey)
        action="${1:-}"
        [[ $# -gt 0 ]] && shift
        case "${action}" in
            generate) generateMasterKey "$@" ;;
            load) noOptions "$@" && loadMasterKey ;;
            *) usageError "Unknown command: masterkey ${action}" ;;
        esac
        ;;
    fitnesse)
        action="${1:-}"
        [[ $# -gt 0 ]] && shift
        case "${action}" in
            start) noOptions "$@" && startFitnesse ;;
            stop) noOptions "$@" && stopFitnesse ;;
            *) usageError "Unknown command: fitnesse ${action}" ;;
        esac
        ;;
    completion)
        noOptions "$@" && showCompletion
        ;;
    install)
        doInstall "$@"
        ;;
    "")
        showHelp
        ;;
    *)
        usageError "Unknown command: ${command}"
        ;;
esac
