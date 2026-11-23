#!/bin/bash
# Runs the click test of Rune (click.ts) against a stone of its own: the EDS
# JAR with Rune (eds-quarkus/target/eds-rune.jar, built before), started
# empty with the in-memory database and a fresh MasterKey on its own ports,
# and stopped afterwards. Its log and the screenshots of a failure are in
# eds-quarkus/target/click. Needs Chromium (CHROMIUM, default the snap).
#
#   eds-quarkus/src/main/webui/scripts/click/run.sh
set -u
webui="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
quarkus="$(cd "${webui}/../../.." && pwd)"
jar="${quarkus}/target/eds-rune.jar"
node="${quarkus}/.quinoa/node/node"
port=${CLICK_STONE_PORT:-8091}
management=$((port + 1000))
out="${quarkus}/target/click"

[[ -f "${jar}" ]] || { echo "${jar} is missing: build EDS first (./eds.sh build rune)."; exit 2; }
[[ -x "${node}" ]] || node=node
rm -rf "${out}"
mkdir -p "${out}"
# The snap of Chromium reads only plain folders of the home, the stone's
# MasterKey file is kept there with the profiles.
work="${HOME}/eds-click-profiles"
mkdir -p "${work}"
masterKey="${work}/masterkey-$$"
(umask 077 && head -c 32 /dev/urandom | base64 > "${masterKey}")

EDS_MASTERKEY_URL="file://${masterKey}" java \
    -Dquarkus.http.port="${port}" -Dquarkus.http.ssl-port=$((port + 400)) -Dquarkus.management.port="${management}" \
    -jar "${jar}" > "${out}/stone.log" 2>&1 &
stone=$!
trap 'kill ${stone} 2>/dev/null; wait ${stone} 2>/dev/null; rm -f "${masterKey}"' EXIT

for _ in $(seq 120); do
    curl -sf "http://localhost:${management}/q/health/ready" > /dev/null && break
    kill -0 ${stone} 2>/dev/null || { echo "The stone didn't start, see ${out}/stone.log"; exit 2; }
    sleep 0.5
done

CLICK_OUT="${out}" "${node}" "${webui}/scripts/click/click.ts" "http://localhost:${port}"
result=$?
# The stone mustn't have logged an error meanwhile.
if grep -q " ERROR " "${out}/stone.log"; then
    echo "The stone logged errors (${out}/stone.log):"
    grep " ERROR " "${out}/stone.log" | head -20
    result=1
fi
exit ${result}
