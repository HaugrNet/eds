#!/bin/bash
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# WildFly Control Script
# -----------------------------------------------------------------------------
# Configuration settings, please only modify this section
# -----------------------------------------------------------------------------
readonly dbHost="localhost"
readonly dbPort="5432"
readonly dbUser="cws_user"
readonly dbPassword="cws"
readonly dbName="cws"
readonly wildfly=${JBOSS_HOME}
# Hidden feature, if set this port is used to start JBoss/WildFly in debug mode
readonly debugPort=${DEBUG_PORT}

# Java & JBoss (WildFly) settings
export JAVA_OPTS="${JAVA_OPTS} -Xms1303m -Xmx1303m -Djava.net.preferIPv4Stack=true"
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-


# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# Run the Control Script
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
if [ "${wildfly}" = "" ]; then
    echo "Script requires that the system variable \$JBOSS_HOME is defined."
    echo
    exit
fi

action=${1}
if [[ "${action}" = "configure" ]]; then
    ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost --command="read-attribute server-state" >/dev/null
    if [[ $? -eq 0 ]]; then
        # CWS requires a database, currently only scripts for PostgreSQL exists, so
        # this is the one being attempted to create here.
        psql -h ${dbHost} -p ${dbPort} -l | grep cws > /dev/null
        if [[ $? -eq 1 ]]; then
            psql -h ${dbHost} -p ${dbPort} postgres -f  `dirname $0`/../postgresql/01-install.sql
        fi

        if [[ ! -e ${wildfly}/modules/org/postgresql/main/module.xml ]]; then
            echo "Configuring WildFly for CWS"
            mkdir -p "${wildfly}/modules/org/postgresql/main"
            cp `dirname $0`/../lib/postgresql-42.2.5.jar ${wildfly}/modules/org/postgresql/main
            cp `dirname $0`/../wildfly/module.xml ${wildfly}/modules/org/postgresql/main
            ${wildfly}/bin/jboss-cli.sh --connect --command="/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-xa-datasource-class-name=org.postgresql.xa.PGXADataSource)" 2>/dev/null
            ${wildfly}/bin/jboss-cli.sh --connect --command="data-source add --name=cwsDS --driver-name=postgresql --jndi-name=java:/datasources/cwsDS --connection-url=jdbc:postgresql://${dbHost}:${dbPort}/${dbName} --user-name=${dbUser} --password=${dbPassword} --use-ccm=false --max-pool-size=25 --blocking-timeout-wait-millis=5000 --enabled=true" 2>/dev/null
            echo "WildFly has been configured"
        else
            echo "WildFly have already been configured"
        fi
    else
        echo "WildFly is not running, please start WildFly before configuring it"
    fi
elif [[ "${action}" = "status" ]]; then
    ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost --command="read-attribute server-state" >/dev/null
    if [[ $? -eq 0 ]]; then
        ${wildfly}/bin/jboss-cli.sh --connect controller=localhost --command="deployment-info --name=cws.war" >/dev/null
        if [[ $? -eq 0 ]]; then
            echo "WildFly is running with CWS deployed"
        else
            echo "WildFly is running"
        fi
    else
        echo "WildFly is not running"
    fi
elif [[ "${action}" = "start" ]]; then
    ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost --command="read-attribute server-state" >/dev/null
    if [[ $? -eq 1 ]]; then
        echo "Starting WildFly ..."
        if [[ "${debugPort}" = "" ]]; then
            ${wildfly}/bin/standalone.sh -Djboss.node.name=cws &
        else
            ${wildfly}/bin/standalone.sh -Djboss.node.name=cws --debug ${debugPort} &
        fi
    else
        echo "WildFly is already running"
    fi
elif [[ "${action}" = "stop" ]]; then
    ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost --command="read-attribute server-state" >/dev/null
    if [[ $? -eq 0 ]]; then
        echo "Stopping WildFly ..."
        ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost --command="shutdown"
    else
        echo "WildFly is not running"
    fi
elif [[ "${action}" = "deploy" ]]; then
    ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost --command="read-attribute server-state" >/dev/null
    if [[ $? -eq 0 ]]; then
        echo "Deploying CWS"
        ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost --command="deploy `dirname $0`/../wildfly/cws.war --force"
    else
        echo "WildFly is not running"
    fi
elif [[ "${action}" = "undeploy" ]]; then
    ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost --command="read-attribute server-state" >/dev/null
    if [[ $? -eq 0 ]]; then
        ${wildfly}/bin/jboss-cli.sh --connect controller=localhost --command="deployment-info --name=cws.war" >/dev/null
        if [[ $? -eq 0 ]]; then
            echo "Undeploying CWS"
            ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost --command="undeploy cws.war"
        else
            echo "CWS was not deployed"
        fi
    else
        echo "WildFly is not running"
    fi
elif [[ "${action}" = "log" ]]; then
    tail -f ${wildfly}/standalone/log/server.log
else
    echo "WildFly Control script"
    echo "Usage: `basename $0` [Action]"
    echo
    echo "  The Action must be one of the following:"
    echo "    configure Attempts to configure a CWS WildFly instance"
    echo "    start     Attempts to start a CWS WildFly instance"
    echo "    stop      Attempts to stop the running CWS WildFly instance"
    echo "    status    Check the current status of WildFly"
    echo "    deploy    Deploy the latest CWS snapshot to WildFly"
    echo "    undeploy  Undeploy the currently deployed CWS snapshot"
    echo "    log       Tail on the Server Log"
    echo
fi
