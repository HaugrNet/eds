#!/bin/sh
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# Payara Control Script
# -----------------------------------------------------------------------------
# Configuration settings, please only modify this section
# -----------------------------------------------------------------------------
readonly dbUser="cws_user"
readonly dbPassword="cws"
readonly dbHost="localhost"
readonly dbName="cws"
readonly domain="domain1"
readonly cwsWar="`dirname $0`/../payara/cws.war"
# Mapping the GLASSFISH_HOME to an internal variable, to have a shorthand for it
readonly payara=${GLASSFISH_HOME}

# Java & JBoss (Payara) settings
export JAVA_OPTS="${JAVA_OPTS} -Xms1303m -Xmx1303m -Djava.net.preferIPv4Stack=true"
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-


# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# Show Help - Displays the help, to use this script
# -----------------------------------------------------------------------------
# Param  -> Void
# Return -> Void
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
show_help () {
    echo "Payara Control script"
    echo "Usage: `basename $0` [Action]"
    echo
    echo "  The Action must be one of the following:"
    echo "    configure Attempts to configure a CWS Payara instance"
    echo "    start     Attempts to start a CWS Payara instance"
    echo "    stop      Attempts to stop the running CWS Payara instance"
    echo "    deploy    Deploy the latest CWS snapshot to Payara"
    echo "    undeploy  Undeploy the currently deployed CWS snapshot"
    echo "    log       Tail on the Server Log"
    echo
}


# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# doConfigure - Attempts to configure Payara
# -----------------------------------------------------------------------------
# Param  -> Void
# Return -> Void
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
doConfigure () {
    echo "Configuring running Payara instance for CWS"
    netstat -ant | grep ":4848 " > /dev/null
    if [ "$?" -ne "0" ]; then
        doStart
    fi
    ${payara}/glassfish/bin/asadmin add-library `dirname $0`/../postgresql-42.2.4.jar
    ${payara}/glassfish/bin/asadmin create-jdbc-connection-pool --datasourceclassname org.postgresql.xa.PGXADataSource --restype javax.sql.XADataSource --property "User=${dbUser}:Password=${dbPassword}:URL=jdbc\:postgresql\://${dbHost}/${dbName}" cwsPool
    ${payara}/glassfish/bin/asadmin create-jdbc-resource --connectionpoolid cwsPool datasources/cwsDS
}


# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# doStart - Attempts to start Payara
# -----------------------------------------------------------------------------
# Param  -> Void
# Return -> Void
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
doStart () {
    echo "Starting Payara ..."
    ${payara}/glassfish/bin/asadmin start-domain ${domain}
}


# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# doStop - Attempts to stop Payara
# -----------------------------------------------------------------------------
# Param  -> Void
# Return -> Void
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
doStop () {
    echo "Stopping Payara ..."
    ${payara}/glassfish/bin/asadmin stop-domain ${domain}
}


# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# doDeploy - Attempts to deploy the latest snapshot to Payara
# -----------------------------------------------------------------------------
# Param  -> Void
# Return -> Void
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
doDeploy () {
    echo "Deploying CWS"
    ${payara}/glassfish/bin/asadmin deploy --force ${cwsWar}
}


# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# doUndeploy - Undeploy's current deployed snapshot from Payara
# -----------------------------------------------------------------------------
# Param  -> Void
# Return -> Void
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
doUndeploy () {
    echo "Undeploying CWS"
    ${payara}/glassfish/bin/asadmin undeploy cws
}


# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# doLog - Tail on the Payara Server log
# -----------------------------------------------------------------------------
# Param  -> Void
# Return -> Void
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
doLog () {
    tail -f ${payara}/glassfish/domains/${domain}/logs/server.log
}


# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# Main part, please don't touch!
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
if [ -z "${payara}" ]; then
    echo "Script requires that the system variable \$GLASSFISH_HOME is defined."
    echo
    exit
fi

action=${1}
if [ "${action}" = "configure" ]; then
    doConfigure
elif [ "${action}" = "start" ]; then
    doStart
elif [ "${action}" = "stop" ]; then
    doStop
elif [ "${action}" = "deploy" ]; then
    doDeploy
elif [ "${action}" = "undeploy" ]; then
    doUndeploy
elif [ "${action}" = "log" ]; then
    doLog
else
    show_help
fi
