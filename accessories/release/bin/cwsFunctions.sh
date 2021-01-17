#!/bin/bash

# Simple Bash script to map most of the Management features in CWS, so it is
# possible to use scripting to quickly and easily setup a new CWS system.
# Note, to help read JSON, the script requires that "jq" is installed.

# Please configure this to the correct value.
server="http://localhost:8080/cws"

# ==============================================================================
# CWS REQUEST :: Version - retrieves the version of the running CWS instance
# ------------------------------------------------------------------------------
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The version of the running CWS instance
#   Return code 1 -> The error message from the server
# ==============================================================================
function version() {
    response=$(curl --silent --header "Content-Type: application/json" --request POST "${server}/api/version")
    inspectResponse "${response}" "version"
    return $?
}

# ==============================================================================
# CWS REQUEST :: Creates a new Account in CWS
# ------------------------------------------------------------------------------
# Parameters:
#   1) Account Name for the administrator invoking request
#   2) PassPhrase for the administrator invoking request
#   3) Account Name for the new account to be created
#   4) PassPhrase for the new account to be created
#   5) Account Type for the new account to be created
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The new AccountId
#   Return code 1 -> The error message from the server
# ==============================================================================
function createAccount() {
    username="\"accountName\":\"$1\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    newAccount="\"newAccountName\":\"${3}\""
    newPassword="\"newCredential\":\"$(echo -n "${4}" | base64)\""
    accountType="\"memberRole\":\"${5}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${newAccount},${newPassword},${accountType}}" "${server}/api/members/createMember")
    inspectResponse "${response}" "memberId"
    return $?
}

# ==============================================================================
# CWS REQUEST :: Removes an Account in CWS
# ------------------------------------------------------------------------------
# Parameters:
#   1) Account Name for the administrator invoking request
#   2) PassPhrase for the administrator invoking request
#   3) AccountId of the account to be deleted
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The Ok message from the server
#   Return code 1 -> The error message from the server
# ==============================================================================
function deleteAccount() {
    username="\"accountName\":\"$1\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    accountId="\"memberId\":\"${3//\"/}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${accountId}}" "${server}/api/members/deleteMember")
    inspectResponse "${response}" "returnMessage"
    return $?
}

# ==============================================================================
# CWS REQUEST :: Shows a list of Accounts (Account Name & Id) from the system
# ------------------------------------------------------------------------------
# Parameters:
#   1) Account Name for the administrator invoking request
#   2) PassPhrase for the administrator invoking request
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> List of Account Names & Id's
#   Return code 1 -> The error message from the server
# ==============================================================================
function listAccounts() {
    username="\"accountName\":\"$1\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password}}" "${server}/api/members/fetchMembers")
    inspectListResponse "${response}" "accountName" "memberId"
    return $?
}

# ==============================================================================
# CWS REQUEST :: Creates a new Circle in CWS
# ------------------------------------------------------------------------------
# Parameters:
#   1) Account Name for the user invoking request
#   2) PassPhrase for the user invoking request
#   3) Name for the new Circle to create
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The new CircleId
#   Return code 1 -> The error message from the server
# ==============================================================================
function createCircle() {
    username="\"accountName\":\"$1\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    circleId="\"circleName\":\"${3}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${circleId}}" "${server}/api/circles/createCircle")
    inspectResponse "${response}" "circleId"
    return $?
}

# ==============================================================================
# CWS REQUEST :: Deletes a Circle in CWS
# ------------------------------------------------------------------------------
# Parameters:
#   1) Account Name for the user invoking request
#   2) PassPhrase for the user invoking request
#   3) CircleId for the Circle to delete
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The Ok message from the server
#   Return code 1 -> The error message from the server
# ==============================================================================
function deleteCircle() {
    username="\"accountName\":\"$1\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    circleId="\"circleId\":\"${3}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${circleId}}" "${server}/api/circles/deleteCircle")
    inspectResponse "${response}" "returnMessage"
    return $?
}

# ==============================================================================
# CWS REQUEST :: Shows a list of Circles (Circle Name & Id) from the system
# ------------------------------------------------------------------------------
# Parameters:
#   1) Account Name for the administrator invoking request
#   2) PassPhrase for the administrator invoking request
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> List of Circle Names & Id's
#   Return code 1 -> The error message from the server
# ==============================================================================
function listCircles() {
    username="\"accountName\":\"$1\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password}}" "${server}/api/circles/fetchCircles")
    inspectListResponse "${response}" "circleName" "circleId"
    return $?
}

# ==============================================================================
# CWS REQUEST :: Adds a Trustee to a Circle
# ------------------------------------------------------------------------------
# Parameters:
#   1) Account Name for the user invoking request
#   2) PassPhrase for the user invoking request
#   3) CircleId for the Circle to add a Trustee too
#   4) AccountId of the user to add as Trustee to the Circle
#   5) TrustLevel for the new Trustee in the Circle [ READ | WRITE | ADMIN ]
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The Ok message from the server
#   Return code 1 -> The error message from the server
# ==============================================================================
function addTrustee() {
    username="\"accountName\":\"${1//\"/}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    circleId="\"circleId\":\"${3//\"/}\""
    memberId="\"memberId\":\"${4//\"/}\""
    trustLevel="\"trustLevel\":\"${5//\"/}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${circleId},${memberId},${trustLevel}}" "${server}/api/trustees/addTrustee")
    echo "$response"
    inspectResponse "${response}" "returnMessage"
    return $?
}

# ==============================================================================
# CWS REQUEST :: Removes a Trustee from a Circle
# ------------------------------------------------------------------------------
# Parameters:
#   1) Account Name for the user invoking request
#   2) PassPhrase for the user invoking request
#   3) CircleId for the Circle to remove a Trustee from
#   4) AccountId of the Trustee to remove from the Circle
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The Ok message from the server
#   Return code 1 -> The error message from the server
# ==============================================================================
function removeTrustee() {
    username="\"accountName\":\"${1//\"/}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    circleId="\"circleId\":\"${3//\"/}\""
    memberId="\"memberId\":\"${4//\"/}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${circleId},${memberId}}" "${server}/api/trustees/removeTrustee")
    inspectResponse "${response}" "returnMessage"
    return $?
}

# ==============================================================================
# CWS REQUEST :: Shows a list of Trustees (AccountId & TrustLevel) for a Circle
# ------------------------------------------------------------------------------
# Parameters:
#   1) Account Name for the administrator invoking request
#   2) PassPhrase for the administrator invoking request
#   3) Circle Id of the Circle to list Trustees for
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> List of Account Id's & their Circle TrustLevel
#   Return code 1 -> The error message from the server
# ==============================================================================
function listTrustees() {
    username="\"accountName\":\"$1\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    circleId="\"circleId\":\"${3}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${circleId}}" "${server}/api/trustees/fetchTrustees")
    inspectListResponse "${response}" "memberId" "trustLevel"
    return $?
}

# ==============================================================================
# INTERNAL FUNCTION :: Inspects the JSON List response from the CWS
# ------------------------------------------------------------------------------
# Parameters:
#   1) The JSON response to process
#   2) First Key to lookup in the JSON List
#   3) Second Key to lookup in the JSON List
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The value for the provided key
#   Return code 1 -> The error message from the server
# ==============================================================================
function inspectListResponse() {
    json=${1}
    key1=${2}
    key2=${3}
    returnCode=$(findKey "${json}" "returnCode")

    if [[ "${returnCode}" == "200" ]]; then
        raw=${json//[]/null}
        defaultIFS="${IFS}"
        IFS='['
        read -r -a hacked1 <<<"${raw}"
        IFS=']'
        read -r -a hacked2 <<<"${hacked1[1]}"
        IFS='{'
        read -r -a hacked3 <<<"${hacked2[0]}"
        IFS="${defaultIFS}"
        for i in "${hacked3[@]}"; do
            if [[ -n "${i}" ]]; then
                first=$(findKey "${i}" "${key1}")
                second=$(findKey "${i}" "${key2}")
                echo "${first//\"/} :: ${second//\"/}"
            fi
        done
        return 0
    else
        returnMessage=$(findKey "${json}" "returnMessage")
        echo "${returnMessage//\"/}"
        return 1
    fi
}

# ==============================================================================
# INTERNAL FUNCTION :: Inspects the JSON response from the CWS
# ------------------------------------------------------------------------------
# Parameters:
#   1) The JSON response to process
#   2) Key to lookup in JSON response
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The value for the provided key
#   Return code 1 -> The error message from the server
# ==============================================================================
function inspectResponse() {
    local json; json="${1}"
    local key; key="${2}"
    returnCode=$(findKey "${json}" "returnCode")

    if [[ "${returnCode}" == "200" ]]; then
        expected=$(findKey "${json}" "${key}")
        echo "${expected//\"/}"
        return 0
    else
        returnMessage=$(findKey "${json}" "returnMessage")
        echo "${returnMessage//\"/}"
        return 1
    fi
}

# ==============================================================================
# INTERNAL FUNCTION :: Finds and prints the value for a given Key
# ------------------------------------------------------------------------------
# Parameters:
#   1) The JSON response to process
#   2) Key to lookup in JSON response
# Output:
#   Value for the requested Key
# ------------------------------------------------------------------------------
# Requires that "jq" is installed !
# ==============================================================================
function findKey() {
    local json; json="${1}"
    local key; key="${2}"

    tmp=$(echo "${json}" | jq -c ".${key}")
    echo "${tmp//\"/}"
}
