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
if [ "${action}" = "configure" ]; then
    netstat -ant | grep ":9990 " > /dev/null
    if [ "$?" -ne "0" ]; then
        echo "Starting WildFly ..."
        ${wildfly}/bin/standalone.sh -Djboss.node.name=cws &
        # The startup is not backgrounded automatically, it must be forced into
        # the background, but we cannot configure WildFly until it is completed,
        # so a simple sleep is added, not optimal - but it'll do the job
        sleep 10
    fi

    # CWS requires a data, currently only scripts for PostgreSQL exists, so
    # this is the one being attempted to create here.
    psql -l | grep cws > /dev/null
    if [ $? -eq 1 ]; then
        psql postgres -f  `dirname $0`/../postgresql/01-install.sql
    fi

    echo "Configuring WildFly for CWS"
    mkdir -p "${wildfly}/modules/org/postgresql/main"
    cp `dirname $0`/../lib/postgresql-42.2.4.jar ${wildfly}/modules/org/postgresql/main
    cp `dirname $0`/../wildfly/module.xml ${wildfly}/modules/org/postgresql/main
    ${wildfly}/bin/jboss-cli.sh --connect --command="/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-xa-datasource-class-name=org.postgresql.xa.PGXADataSource)" 2>/dev/null
    ${wildfly}/bin/jboss-cli.sh --connect --command="data-source add --name=cwsDS --driver-name=postgresql --jndi-name=java:/datasources/cwsDS --connection-url=jdbc:postgresql://${dbHost}:${dbPort}/${dbName} --user-name=${dbUser} --password=${dbPassword} --use-ccm=false --max-pool-size=25 --blocking-timeout-wait-millis=5000 --enabled=true" 2>/dev/null
    echo "WildFly has been configured"
elif [ "${action}" = "start" ]; then
    echo "Starting WildFly ..."
    ${wildfly}/bin/standalone.sh -Djboss.node.name=cws &
elif [ "${action}" = "stop" ]; then
    echo "Stopping WildFly ..."
    ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost:9990 --command="shutdown"
elif [ "${action}" = "deploy" ]; then
    echo "Deploying CWS"
    ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost:9990 --command="deploy `dirname $0`/../wildfly/cws.war --force"
elif [ "${action}" = "undeploy" ]; then
    echo "Undeploying CWS"
    ${wildfly}/bin/jboss-cli.sh --connect --controller=localhost:9990 --command="undeploy cws.war"
elif [ "${action}" = "log" ]; then
    tail -f ${wildfly}/standalone/log/server.log
else
    echo "WildFly Control script"
    echo "Usage: `basename $0` [Action]"
    echo
    echo "  The Action must be one of the following:"
    echo "    configure Attempts to configure a CWS WildFly instance"
    echo "    start     Attempts to start a CWS WildFly instance"
    echo "    stop      Attempts to stop the running CWS WildFly instance"
    echo "    deploy    Deploy the latest CWS snapshot to WildFly"
    echo "    undeploy  Undeploy the currently deployed CWS snapshot"
    echo "    log       Tail on the Server Log"
    echo
fi
