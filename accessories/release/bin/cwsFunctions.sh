#!/bin/bash

# Simple Bash script to map most of the Management features in CWS, so it is
# possible to use scripting to quickly and easily setup a new CWS system.
# Note, to help read JSON, the script requires that "jq" is installed.

# Please configure this to the correct value.
server="http://localhost:8080/cws"


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
function __inspectResponse() {
    json="${1}"
    key="${2}"

    if [[ "$(echo "${json}" | jq ".returnCode")" == "200" ]]; then
        echo "${json}" | jq ".${key}"
        return 0
    else
        echo "${json}" | jq ".returnMessage"
        return 1
    fi
}

# ==============================================================================
# CWS REQUEST :: Version - retrieves the version of the running CWS instance
# ------------------------------------------------------------------------------
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The version of the running CWS instance
#   Return code 1 -> The error message from the server
# ==============================================================================
function cwsVersion() {
    response=$(curl --silent --header "Content-Type: application/json" --request POST "${server}/version")
    __inspectResponse "${response}" "version"
}

# ==============================================================================
# CWS REQUEST :: Settings - retrieves the settings for the running CWS instance
# ------------------------------------------------------------------------------
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The settings for the running CWS instance
#   Return code 1 -> The error message from the server
# ==============================================================================
function cwsSettings() {
    username="\"accountName\":\"${1}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    requestJson="{${username},${password}}"

    echo "Request Json: '${requestJson}'"
    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "${requestJson}" "${server}/settings")
    __inspectResponse "${response}" "settings"
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
function cwsCreateAccount() {
    username="\"accountName\":\"${1}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    newAccount="\"newAccountName\":\"${3}\""
    newPassword="\"newCredential\":\"$(echo -n "${4}" | base64)\""
    accountType="\"memberRole\":\"${5}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${newAccount},${newPassword},${accountType}}" "${server}/members/createMember")
    __inspectResponse "${response}" "memberId"
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
function cwsDeleteAccount() {
    username="\"accountName\":\"${1}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    accountId="\"memberId\":\"${3//\"/}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${accountId}}" "${server}/members/deleteMember")
    __inspectResponse "${response}" "returnMessage"
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
function cwsListAccounts() {
    username="\"accountName\":\"${1}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password}}" "${server}/members/fetchMembers")
    __inspectListResponse "${response}" "accountName" "memberId"
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
function cwsCreateCircle() {
    username="\"accountName\":\"${1}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    circleId="\"circleName\":\"${3}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${circleId}}" "${server}/circles/createCircle")
    __inspectResponse "${response}" "circleId"
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
function cwsDeleteCircle() {
    username="\"accountName\":\"${1}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    circleId="\"circleId\":\"${3}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${circleId}}" "${server}/circles/deleteCircle")
    __inspectResponse "${response}" "returnMessage"
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
function cwsListCircles() {
    username="\"accountName\":\"${1}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""

    #echo "Request JSON: $(echo "{${username},${password}}" | jq)"
    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password}}" "${server}/circles/fetchCircles")
    __inspectResponse "${response}"
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
function cwsAddTrustee() {
    username="\"accountName\":\"${1}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    circleId="\"circleId\":\"${3//\"/}\""
    memberId="\"memberId\":\"${4//\"/}\""
    trustLevel="\"trustLevel\":\"${5//\"/}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${circleId},${memberId},${trustLevel}}" "${server}/trustees/addTrustee")
    __inspectResponse "${response}" "returnMessage"
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
function cwsRemoveTrustee() {
    username="\"accountName\":\"${1}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    circleId="\"circleId\":\"${3//\"/}\""
    memberId="\"memberId\":\"${4//\"/}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${circleId},${memberId}}" "${server}/trustees/removeTrustee")
    __inspectResponse "${response}" "returnMessage"
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
function cwsListTrustees() {
    username="\"accountName\":\"${1}\""
    password="\"credential\":\"$(echo -n "${2}" | base64)\""
    circleId="\"circleId\":\"${3}\""

    response=$(curl --silent --header "Content-Type: application/json" --request POST --data "{${username},${password},${circleId}}" "${server}/trustees/fetchTrustees")
    __inspectResponse "${response}" "members"
}
