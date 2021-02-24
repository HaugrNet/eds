#!/bin/bash

# Simple Bash script to map most of the Management features in CWS, so it is
# possible to use scripting to quickly and easily setup a new CWS system.
# Note, to help read JSON, the script requires that "jq" is installed.

# Please configure this to the correct value.
server="http://localhost:8080/cws"

# ==============================================================================
# INTERNAL FUNCTION :: Invoke Curl to run a request against CWS and inspect it
# ------------------------------------------------------------------------------
# Parameters:
#   1) UserName or AccountName - of Session Token
#   2) Password / Passphrase for account - undefined if using Session Token
# Output:
#   Return Json snippet with credentials for CWS request
# ==============================================================================
function __credentials() {
    if [[ $# -eq 1 ]]; then
        echo "\"accountName\":null,\"credential\":\"$(echo -n "${1}" | base64)\",\"credentialType\":\"SESSION\""
    else
        echo "\"accountName\":\"${1}\",\"credential\":\"$(echo -n "${2}" | base64)\",\"credentialType\":\"PASSPHRASE\""
    fi
}

# ==============================================================================
# INTERNAL FUNCTION :: Invoke Curl to run a request against CWS and inspect it
# ------------------------------------------------------------------------------
# Parameters:
#   1) Path to be appended to the base URL (Server)
#   2) Key to lookup in JSON response
#   3) The JSON response to process (optional, if request JSON is required)
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The value for the provided key
#   Return code 1 -> The error message from the server
# ==============================================================================
function __invokeCurl() {
    local path="${1}"
    local key="${2}"
    local json="${3}"

    # First, run the request, and save the resulting JSON as response
    if [[ "${json}" == "" ]]; then
        response=$(curl --silent --header "Content-Type: application/json" --request POST "${server}/${path}")
    else
        response=$(curl --silent --header "Content-Type: application/json" --request POST --data "${json}" "${server}/${path}")
    fi

    # Then, we can analyze the response using JQ to parse the JSON
    if [[ "$(echo "${response}" | jq -r ".returnCode")" == "200" ]]; then
        echo "${response}" | jq -r ".${key}"
        return 0
    else
        echo "${response}" | jq -r ".returnMessage"
        return 1
    fi
}

# ==============================================================================
# CWS HELP :: List the CWS functionality provided by this library
# ------------------------------------------------------------------------------
# Output:
#   Help text outlining functions and parameters
# ==============================================================================
function cwsHelp() {
    echo "General functionality to communicate with CWS (CryptoStore)"
    echo
    echo "Library contain the following functionality"
    echo
    echo "  General Functionality"
    echo "    cwsVersion"
    echo "    cwsSettings [ <Token> | <Account> <Password> ]"
    echo "    cwsLogin <Account> <Password> <Token>"
    echo "    cwsLogout [ <Token> | <Account> <Password> ]"
    echo "    cwsAuthenticate [ <Token> | <Account> <Password> ]"
    echo
    echo "  Account Management"
    echo "    cwsCreateAccount [ <Token> | <Account> <Password> ] <NewAccountName> <NewPassword> [ ADMIN | STANDARD ]"
    echo "    cwsDeleteAccount [ <Token> | <Account> <Password> ] <AccountId>"
    echo "    cwsListAccounts [ <Token> | <Account> <Password> ]"
    echo
    echo "  Circle Management"
    echo "    cwsCreateCircle [ <Token> | <Account> <Password> ] <CircleName>"
    echo "    cwsDeleteCircle [ <Token> | <Account> <Password> ] <CircleId>"
    echo "    cwsListCircles [ <Token> | <Account> <Password> ]"
    echo
    echo "  Trustee Management"
    echo "    cwsAddTrustee [ <Token> | <Account> <Password> ] <CircleId> <AccountId> <TrustLevel>"
    echo "    cwsRemoveTrustee [ <Token> | <Account> <Password> ] <CircleId> <AccountId>"
    echo "    cwsListTrustees [ <Token> | <Account> <Password> ] <CircleId>"
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
    __invokeCurl "version" "version"
}

# ==============================================================================
# CWS REQUEST :: Login - assigns a Session Token to an account temporarily
# ------------------------------------------------------------------------------
# Parameters:
#   AccountName for the account to log in
#   Password for the account
#   Token to be used as Session Authentication information
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The return message from the server
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsLogin() {
    if [[ $# -eq 3 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        token="\"newCredential\":\"$(echo -n "${3}" | base64)\""
        __invokeCurl "members/login" "returnMessage" "{${credentials},${token}}"
    else
        echo "Usage: <Account> <Password> <Token>"
        return 2
    fi
}

# ==============================================================================
# CWS REQUEST :: Logout - remove an assigned Session token from an account
# ------------------------------------------------------------------------------
# Parameters:
#   Token | AccountName + Password
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The return message from the server
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsLogout() {
    if [[ $# -eq 1 ]]; then
        credentials="$(__credentials "${1}")"
    elif [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
    else
        echo "Usage: cwsLogout [ <Token> | <Account> <Password> ]"
        return 2
    fi

    __invokeCurl "members/logout" "returnMessage" "{${credentials}}"
}

# ==============================================================================
# CWS REQUEST :: Authenticated - verify if the credentials are valid
# ------------------------------------------------------------------------------
# Parameters:
#   Token | AccountName + Password
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The return message from the server
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsAuthenticate() {
    if [[ $# -eq 1 ]]; then
        credentials="$(__credentials "${1}")"
    elif [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
    else
        echo "Usage: cwsAuthenticate [ <Token> | <Account> <Password> ]"
        return 2
    fi

    __invokeCurl "authenticated" "returnMessage" "{${credentials}}"
}

# ==============================================================================
# CWS REQUEST :: Settings - retrieves the settings for the running CWS instance
# ------------------------------------------------------------------------------
# Parameters:
#   Token | AccountName + Password
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The settings for the running CWS instance
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsSettings() {
    if [[ $# -eq 1 ]]; then
        credentials="$(__credentials "${1}")"
    elif [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
    else
        echo "Usage: cwsSettings [ <Token> | <Account> <Password> ]"
        return 2
    fi

    __invokeCurl "settings" "settings" "{${credentials}}"
}

# ==============================================================================
# CWS REQUEST :: Creates a new Account in CWS
# ------------------------------------------------------------------------------
# Parameters:
#   Token | AccountName + Password
#   Account Name for the new account to be created
#   PassPhrase for the new account to be created
#   Account Type for the new account to be created ADMIN or STANDARD
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The new AccountId
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsCreateAccount() {
    if [[ $# -eq 4 ]]; then
        credentials="$(__credentials "${1}")"
        newAccount="\"newAccountName\":\"${2}\""
        newPassword="\"newCredential\":\"$(echo -n "${3}" | base64)\""
        accountType="\"memberRole\":\"${4}\""
    elif [[ $# -eq 5 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        newAccount="\"newAccountName\":\"${3}\""
        newPassword="\"newCredential\":\"$(echo -n "${4}" | base64)\""
        accountType="\"memberRole\":\"${5}\""
    else
        echo "Usage: cwsCreateAccount [ <Token> | <Account> <Password> ] <NewAccountName> <NewPassword> [ ADMIN | STANDARD ]"
        return 2
    fi

    __invokeCurl "members/createMember" "memberId" "{${credentials},${newAccount},${newPassword},${accountType}}"
}

# ==============================================================================
# CWS REQUEST :: Removes an Account in CWS
# ------------------------------------------------------------------------------
# Parameters:
#   Session Token | Account Name + Password
#   AccountId of the account to be deleted
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The return message from the server
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsDeleteAccount() {
    if [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}")"
        accountId="\"memberId\":\"${2//\"/}\""
    elif [[ $# -eq 3 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        accountId="\"memberId\":\"${3//\"/}\""
    else
        echo "Usage: cwsDeleteAccount [ <Token> | <Account> <Password> ] <AccountId>"
        return 2
    fi

    __invokeCurl "members/deleteMember" "returnMessage" "{${credentials},${accountId}}"
}

# ==============================================================================
# CWS REQUEST :: Shows a list of Accounts (Account Name & Id) from the system
# ------------------------------------------------------------------------------
# Parameters:
#   Session Token | Account Name + Password
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> List of Accounts
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsListAccounts() {
    if [[ $# -eq 1 ]]; then
        credentials="$(__credentials "${1}")"
    elif [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
    else
        echo "Usage: cwsListAccounts [ <Token> | <Account> <Password> ]"
        return 2
    fi

    __invokeCurl "members/fetchMembers" "members" "{${credentials}}"
}

# ==============================================================================
# CWS REQUEST :: Creates a new Circle in CWS
# ------------------------------------------------------------------------------
# Parameters:
#   Session Token | Account Name + Password
#   Name for the new Circle to create
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The new CircleId
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsCreateCircle() {
    if [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}")"
        circleId="\"circleName\":\"${2}\""
    elif [[ $# -eq 3 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        circleId="\"circleName\":\"${3}\""
    else
        echo "Usage: cwsCreateCircle [ <Token> | <Account> <Password> ] <CircleName>"
        return 2
    fi

    __invokeCurl "circles/createCircle" "circleId" "{${credentials},${circleId}}"
}

# ==============================================================================
# CWS REQUEST :: Deletes a Circle in CWS
# ------------------------------------------------------------------------------
# Parameters:
#   Session Token | Account Name + Password
#   CircleId for the Circle to delete
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The return message from the server
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsDeleteCircle() {
    if [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}")"
        circleId="\"circleId\":\"${2}\""
    elif [[ $# -eq 3 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        circleId="\"circleId\":\"${3}\""
    else
        echo "Usage: cwsDeleteCircle [ <Token> | <Account> <Password> ] <CircleId>"
        return 2
    fi

    __invokeCurl "circles/deleteCircle" "returnMessage" "{${credentials},${circleId}}"
}

# ==============================================================================
# CWS REQUEST :: Shows a list of Circles from the system
# ------------------------------------------------------------------------------
# Parameters:
#   Session Token | Account Name + Password
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> List of Circles
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsListCircles() {
    if [[ $# -eq 1 ]]; then
        credentials="$(__credentials "${1}")"
    elif [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
    else
        echo "Usage: cwsListCircles [ <Token> | <Account> <Password> ]"
        return 2
    fi

    __invokeCurl "circles/fetchCircles" "circles" "{${credentials}}"
}

# ==============================================================================
# CWS REQUEST :: Adds a Trustee to a Circle
# ------------------------------------------------------------------------------
# Parameters:
#   Session Token | Account Name + Password
#   CircleId for the Circle to add a Trustee too
#   AccountId of the user to add as Trustee to the Circle
#   TrustLevel for the new Trustee in the Circle [ READ | WRITE | ADMIN ]
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The return message from the server
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsAddTrustee() {
    if [[ $# -eq 4 ]]; then
        credentials="$(__credentials "${1}")"
        circleId="\"circleId\":\"${2//\"/}\""
        memberId="\"memberId\":\"${3//\"/}\""
        trustLevel="\"trustLevel\":\"${4//\"/}\""
    elif [[ $# -eq 5 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        circleId="\"circleId\":\"${3//\"/}\""
        memberId="\"memberId\":\"${4//\"/}\""
        trustLevel="\"trustLevel\":\"${5//\"/}\""
    else
        echo "Usage: cwsAddTrustee [ <Token> | <Account> <Password> ] <CircleId> <AccountId> [ READ | WRITE | ADMIN ]"
        return 2
    fi

    __invokeCurl "trustees/addTrustee" "returnMessage" "{${credentials},${circleId},${memberId},${trustLevel}}"
}

# ==============================================================================
# CWS REQUEST :: Removes a Trustee from a Circle
# ------------------------------------------------------------------------------
# Parameters:
#   Session Token | Account Name + Password
#   CircleId for the Circle to remove a Trustee from
#   AccountId of the Trustee to remove from the Circle
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The return message from the server
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsRemoveTrustee() {
    if [[ $# -eq 3 ]]; then
        credentials="$(__credentials "${1}")"
        circleId="\"circleId\":\"${2//\"/}\""
        memberId="\"memberId\":\"${3//\"/}\""
    elif [[ $# -eq 4 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        circleId="\"circleId\":\"${3//\"/}\""
        memberId="\"memberId\":\"${4//\"/}\""
    else
        echo "Usage: cwsRemoveTrustee [ <Token> | <Account> <Password> ] <CircleId> <AccountId>"
        return 2
    fi

    __invokeCurl "trustees/removeTrustee" "returnMessage" "{${credentials},${circleId},${memberId}}"
}

# ==============================================================================
# CWS REQUEST :: Shows a list of Trustees (AccountId & TrustLevel) for a Circle
# ------------------------------------------------------------------------------
# Parameters:
#   Session Token | Account Name + Password
#   CircleId of the Circle to list Trustees for
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> List of Trustees for for the given Circle
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function cwsListTrustees() {
    if [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}")"
        circleId="\"circleId\":\"${2}\""
    elif [[ $# -eq 3 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        circleId="\"circleId\":\"${3}\""
    else
        echo "Usage: cwsListTrustees [ <Token> | <Account> <Password> ] <CircleId>"
        return 2
    fi

    __invokeCurl "trustees/fetchTrustees" "members" "{${credentials},${circleId}}"
}
