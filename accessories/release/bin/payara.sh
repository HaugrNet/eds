#!/bin/sh

# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# Payara Control Script
# -----------------------------------------------------------------------------
# Configuration settings, please only modify this section
# -----------------------------------------------------------------------------
readonly dbHost="localhost"
readonly dbUser="cws_user"
readonly dbPassword="cws"
readonly dbName="cws"
readonly domain="domain1"
readonly payara="${GLASSFISH_HOME}"
readonly psqlVersion="42.2.19"

# Java & JBoss (Payara) settings
export JAVA_OPTS="${JAVA_OPTS} -Xms1303m -Xmx1303m -Djava.net.preferIPv4Stack=true"
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

runAsAdmin() {
    "${payara}/glassfish/bin/asadmin" "$@"
}

# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# Run the Control Script
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
if [ "${payara}" = "" ]; then
    echo "Script requires that the system variable \$GLASSFISH_HOME is defined."
    echo
    exit
fi

action="${1}"
if [ "${action}" = "configure" ]; then
    echo "Configuring Payara for CWS"
    wget -q -P /tmp/ "https://jdbc.postgresql.org/download/postgresql-${psqlVersion}.jar"
    runAsAdmin add-library "/tmp/postgresql-${psqlVersion}.jar"
    runAsAdmin create-jdbc-connection-pool --datasourceclassname org.postgresql.xa.PGXADataSource --restype javax.sql.XADataSource --property "User=${dbUser}:Password=${dbPassword}:URL=jdbc\:postgresql\://${dbHost}/${dbName}" cwsPool
    runAsAdmin create-jdbc-resource --connectionpoolid cwsPool datasources/cwsDS
elif [ "${action}" = "start" ]; then
    echo "Starting Payara ..."
    runAsAdmin start-domain ${domain}
elif [ "${action}" = "stop" ]; then
    echo "Stopping Payara ..."
    runAsAdmin stop-domain ${domain}
elif [ "${action}" = "deploy" ]; then
    echo "Deploying CWS"
    runAsAdmin deploy --force "$(dirname "${0}")/../payara/cws.war"
elif [ "${action}" = "undeploy" ]; then
    echo "Undeploying CWS"
    runAsAdmin undeploy cws
elif [ "${action}" = "log" ]; then
    tail -f "${payara}/glassfish/domains/${domain}/logs/server.log"
else
    echo "Payara Control script"
    echo "Usage: $(basename "${0}") [Action]"
    echo
    echo "  The Action must be one of the following:"
    echo "    configure  Attempts to configure a started CWS Payara instance"
    echo "    start      Attempts to start a CWS Payara instance"
    echo "    stop       Attempts to stop the running CWS Payara instance"
    echo "    deploy     Deploy the latest CWS snapshot to Payara"
    echo "    undeploy   Undeploy the currently deployed CWS snapshot"
    echo "    log        Tail on the Server Log"
    echo
fi
