#!/bin/sh
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# Payara Control Script
# -----------------------------------------------------------------------------
# Configuration settings, please only modify this section
# -----------------------------------------------------------------------------
readonly dbHost="localhost"
readonly dbPort="5432"
readonly dbUser="cws_user"
readonly dbPassword="cws"
readonly dbName="cws"
readonly domain="domain1"
readonly payara=${GLASSFISH_HOME}

# Java & JBoss (Payara) settings
export JAVA_OPTS="${JAVA_OPTS} -Xms1303m -Xmx1303m -Djava.net.preferIPv4Stack=true"
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-


# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# Run the Control Script
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
if [ "${payara}" = "" ]; then
    echo "Script requires that the system variable \$GLASSFISH_HOME is defined."
    echo
    exit
fi

action=${1}
if [ "${action}" = "configure" ]; then
    netstat -ant | grep ":4848 " > /dev/null
    if [ "$?" -ne "0" ]; then
        echo "Starting Payara ..."
        ${payara}/glassfish/bin/asadmin start-domain ${domain}
    fi

    # CWS requires a data, currently only scripts for PostgreSQL exists, so
    # this is the one being attempted to create here.
    psql -h ${dbHost} -p ${dbPort} -l | grep cws > /dev/null
    if [ $? -eq 1 ]; then
        psql -h ${dbHost} -p ${dbPort} postgres -f  `dirname $0`/../postgresql/01-install.sql
    fi

    echo "Configuring Payara for CWS"
    ${payara}/glassfish/bin/asadmin add-library `dirname $0`/../lib/postgresql-42.2.5.jar
    ${payara}/glassfish/bin/asadmin create-jdbc-connection-pool --datasourceclassname org.postgresql.xa.PGXADataSource --restype javax.sql.XADataSource --property "User=${dbUser}:Password=${dbPassword}:URL=jdbc\:postgresql\://${dbHost}/${dbName}" cwsPool
    ${payara}/glassfish/bin/asadmin create-jdbc-resource --connectionpoolid cwsPool datasources/cwsDS
elif [ "${action}" = "start" ]; then
    echo "Starting Payara ..."
    ${payara}/glassfish/bin/asadmin start-domain ${domain}
elif [ "${action}" = "stop" ]; then
    echo "Stopping Payara ..."
    ${payara}/glassfish/bin/asadmin stop-domain ${domain}
elif [ "${action}" = "deploy" ]; then
    echo "Deploying CWS"
    ${payara}/glassfish/bin/asadmin deploy --force `dirname $0`/../payara/cws.war
elif [ "${action}" = "undeploy" ]; then
    echo "Undeploying CWS"
    ${payara}/glassfish/bin/asadmin undeploy cws
elif [ "${action}" = "log" ]; then
    tail -f ${payara}/glassfish/domains/${domain}/logs/server.log
else
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
fi
