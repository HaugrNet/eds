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

# Simple Bash script to map most of the Management features in EDS, so it is
# possible to use scripting to quickly and easily setup a new EDS system.
# Note, to help read JSON, the script requires that "jq" is installed.

# Please configure this to the correct value.
server="http://localhost:8080/eds"

# ==============================================================================
# INTERNAL FUNCTION :: Invoke Curl to run a request against EDS and inspect it
# ------------------------------------------------------------------------------
# Parameters:
#   1) UserName or AccountName - of Session Token
#   2) Password / Passphrase for account - undefined if using Session Token
# Output:
#   Return Json snippet with credentials for EDS request
# ==============================================================================
function __credentials() {
    if [[ $# -eq 1 ]]; then
        echo "\"accountName\":null,\"credential\":\"$(echo -n "${1}" | base64)\",\"credentialType\":\"SESSION\""
    else
        echo "\"accountName\":\"${1}\",\"credential\":\"$(echo -n "${2}" | base64)\",\"credentialType\":\"PASSPHRASE\""
    fi
}

# ==============================================================================
# INTERNAL FUNCTION :: Invoke Curl to run a request against EDS and inspect it
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
# EDS HELP :: List the EDS functionality provided by this library
# ------------------------------------------------------------------------------
# Output:
#   Help text outlining functions and parameters
# ==============================================================================
function edsHelp() {
    echo "General functionality to communicate with EDS (CryptoStore)"
    echo
    echo "Library contain the following functionality"
    echo
    echo "  General Functionality"
    echo "    edsVersion"
    echo "    edsSettings [ <Token> | <Account> <Password> ]"
    echo "    edsLogin <Account> <Password> <Token>"
    echo "    edsLogout [ <Token> | <Account> <Password> ]"
    echo "    edsAuthenticate [ <Token> | <Account> <Password> ]"
    echo
    echo "  Account Management"
    echo "    edsCreateAccount [ <Token> | <Account> <Password> ] <NewAccountName> <NewPassword> [ ADMIN | STANDARD ]"
    echo "    edsDeleteAccount [ <Token> | <Account> <Password> ] <AccountId>"
    echo "    edsListAccounts [ <Token> | <Account> <Password> ]"
    echo
    echo "  Circle Management"
    echo "    edsCreateCircle [ <Token> | <Account> <Password> ] <CircleName>"
    echo "    edsDeleteCircle [ <Token> | <Account> <Password> ] <CircleId>"
    echo "    edsListCircles [ <Token> | <Account> <Password> ]"
    echo
    echo "  Trustee Management"
    echo "    edsAddTrustee [ <Token> | <Account> <Password> ] <CircleId> <AccountId> <TrustLevel>"
    echo "    edsRemoveTrustee [ <Token> | <Account> <Password> ] <CircleId> <AccountId>"
    echo "    edsListTrustees [ <Token> | <Account> <Password> ] <CircleId>"
}

# ==============================================================================
# EDS REQUEST :: Version - retrieves the version of the running EDS instance
# ------------------------------------------------------------------------------
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The version of the running EDS instance
#   Return code 1 -> The error message from the server
# ==============================================================================
function edsVersion() {
    __invokeCurl "version" "version"
}

# ==============================================================================
# EDS REQUEST :: Login - assigns a Session Token to an account temporarily
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
function edsLogin() {
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
# EDS REQUEST :: Logout - remove an assigned Session token from an account
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
function edsLogout() {
    if [[ $# -eq 1 ]]; then
        credentials="$(__credentials "${1}")"
    elif [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
    else
        echo "Usage: edsLogout [ <Token> | <Account> <Password> ]"
        return 2
    fi

    __invokeCurl "members/logout" "returnMessage" "{${credentials}}"
}

# ==============================================================================
# EDS REQUEST :: Authenticated - verify if the credentials are valid
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
function edsAuthenticate() {
    if [[ $# -eq 1 ]]; then
        credentials="$(__credentials "${1}")"
    elif [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
    else
        echo "Usage: edsAuthenticate [ <Token> | <Account> <Password> ]"
        return 2
    fi

    __invokeCurl "authenticated" "returnMessage" "{${credentials}}"
}

# ==============================================================================
# EDS REQUEST :: Settings - retrieves the settings for the running EDS instance
# ------------------------------------------------------------------------------
# Parameters:
#   Token | AccountName + Password
# Return:
#   0 if everything was ok, otherwise 1
# Output:
#   Return code 0 -> The settings for the running EDS instance
#   Return code 1 -> The error message from the server
#   Return code 2 -> Missing arguments to function
# ==============================================================================
function edsSettings() {
    if [[ $# -eq 1 ]]; then
        credentials="$(__credentials "${1}")"
    elif [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
    else
        echo "Usage: edsSettings [ <Token> | <Account> <Password> ]"
        return 2
    fi

    __invokeCurl "settings" "settings" "{${credentials}}"
}

# ==============================================================================
# EDS REQUEST :: Creates a new Account in EDS
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
function edsCreateAccount() {
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
        echo "Usage: edsCreateAccount [ <Token> | <Account> <Password> ] <NewAccountName> <NewPassword> [ ADMIN | STANDARD ]"
        return 2
    fi

    __invokeCurl "members/createMember" "memberId" "{${credentials},${newAccount},${newPassword},${accountType}}"
}

# ==============================================================================
# EDS REQUEST :: Removes an Account in EDS
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
function edsDeleteAccount() {
    if [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}")"
        accountId="\"memberId\":\"${2//\"/}\""
    elif [[ $# -eq 3 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        accountId="\"memberId\":\"${3//\"/}\""
    else
        echo "Usage: edsDeleteAccount [ <Token> | <Account> <Password> ] <AccountId>"
        return 2
    fi

    __invokeCurl "members/deleteMember" "returnMessage" "{${credentials},${accountId}}"
}

# ==============================================================================
# EDS REQUEST :: Shows a list of Accounts (Account Name & Id) from the system
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
function edsListAccounts() {
    if [[ $# -eq 1 ]]; then
        credentials="$(__credentials "${1}")"
    elif [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
    else
        echo "Usage: edsListAccounts [ <Token> | <Account> <Password> ]"
        return 2
    fi

    __invokeCurl "members/fetchMembers" "members" "{${credentials}}"
}

# ==============================================================================
# EDS REQUEST :: Creates a new Circle in EDS
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
function edsCreateCircle() {
    if [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}")"
        circleId="\"circleName\":\"${2}\""
    elif [[ $# -eq 3 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        circleId="\"circleName\":\"${3}\""
    else
        echo "Usage: edsCreateCircle [ <Token> | <Account> <Password> ] <CircleName>"
        return 2
    fi

    __invokeCurl "circles/createCircle" "circleId" "{${credentials},${circleId}}"
}

# ==============================================================================
# EDS REQUEST :: Deletes a Circle in EDS
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
function edsDeleteCircle() {
    if [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}")"
        circleId="\"circleId\":\"${2}\""
    elif [[ $# -eq 3 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        circleId="\"circleId\":\"${3}\""
    else
        echo "Usage: edsDeleteCircle [ <Token> | <Account> <Password> ] <CircleId>"
        return 2
    fi

    __invokeCurl "circles/deleteCircle" "returnMessage" "{${credentials},${circleId}}"
}

# ==============================================================================
# EDS REQUEST :: Shows a list of Circles from the system
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
function edsListCircles() {
    if [[ $# -eq 1 ]]; then
        credentials="$(__credentials "${1}")"
    elif [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
    else
        echo "Usage: edsListCircles [ <Token> | <Account> <Password> ]"
        return 2
    fi

    __invokeCurl "circles/fetchCircles" "circles" "{${credentials}}"
}

# ==============================================================================
# EDS REQUEST :: Adds a Trustee to a Circle
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
function edsAddTrustee() {
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
        echo "Usage: edsAddTrustee [ <Token> | <Account> <Password> ] <CircleId> <AccountId> [ READ | WRITE | ADMIN ]"
        return 2
    fi

    __invokeCurl "trustees/addTrustee" "returnMessage" "{${credentials},${circleId},${memberId},${trustLevel}}"
}

# ==============================================================================
# EDS REQUEST :: Removes a Trustee from a Circle
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
function edsRemoveTrustee() {
    if [[ $# -eq 3 ]]; then
        credentials="$(__credentials "${1}")"
        circleId="\"circleId\":\"${2//\"/}\""
        memberId="\"memberId\":\"${3//\"/}\""
    elif [[ $# -eq 4 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        circleId="\"circleId\":\"${3//\"/}\""
        memberId="\"memberId\":\"${4//\"/}\""
    else
        echo "Usage: edsRemoveTrustee [ <Token> | <Account> <Password> ] <CircleId> <AccountId>"
        return 2
    fi

    __invokeCurl "trustees/removeTrustee" "returnMessage" "{${credentials},${circleId},${memberId}}"
}

# ==============================================================================
# EDS REQUEST :: Shows a list of Trustees (AccountId & TrustLevel) for a Circle
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
function edsListTrustees() {
    if [[ $# -eq 2 ]]; then
        credentials="$(__credentials "${1}")"
        circleId="\"circleId\":\"${2}\""
    elif [[ $# -eq 3 ]]; then
        credentials="$(__credentials "${1}" "${2}")"
        circleId="\"circleId\":\"${3}\""
    else
        echo "Usage: edsListTrustees [ <Token> | <Account> <Password> ] <CircleId>"
        return 2
    fi

    __invokeCurl "trustees/fetchTrustees" "members" "{${credentials},${circleId}}"
}
