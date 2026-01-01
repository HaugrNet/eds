#!/bin/bash

#
# EDS, Encrypted Data Share - open source Cryptographic Sharing system.
# Copyright (c) 2016-2026, haugr.net
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

# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# WildFly Control Script
# -----------------------------------------------------------------------------
# Configuration settings, please only modify this section
# -----------------------------------------------------------------------------
readonly dbHost="localhost"
readonly dbPort="5432"
readonly dbUser="eds_user"
readonly dbPassword="eds"
readonly dbName="eds"
# Maximal file size for uploading to WildFly - 25 MB (25 * 1024 * 1024)
readonly maxPostSize="26214400"
readonly wildfly=${JBOSS_HOME}
# Hidden feature, if set this port is used to start JBoss/WildFly in debug mode
readonly debugPort=${DEBUG_PORT}
readonly psqlVersion="42.7.8"

# Java & JBoss (WildFly) settings
export JAVA_OPTS="${JAVA_OPTS} -Xms1303m -Xmx1303m -Djava.net.preferIPv4Stack=true"
readonly proxy="${http_proxy:-}"
if [[ "${proxy}" != "" ]]; then
    readonly tmp=${proxy//:[[:digit:]]*/}
    readonly host=${tmp:7}
    readonly port=${proxy//[^0-9]/}
    export JAVA_OPTS="${JAVA_OPTS} -Dhttp.proxyHost=${host} -Dhttp.proxyPort=${port} -Dhttps.proxyHost=${host} -Dhttps.proxyPort=${port}"
fi
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# Run the Control Script
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
if [[ "${wildfly}" == "" ]]; then
    echo "Script requires that the system variable \$JBOSS_HOME is defined."
    echo
    exit
fi

# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# Run the Control Script
# -----------------------------------------------------------------------------
# Param $1: Command to execute in the JBoss environment
# Return Return value from the JBoss Client
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
function runJbossCli() {
    bash "${wildfly}/bin/jboss-cli.sh" --connect --controller=localhost --command="${1}" > /dev/null 2>&1
}

# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
# Main part
# -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
action="${1}"
if [[ "${action}" == "configure" ]]; then
    if (runJbossCli "read-attribute server-state"); then
        if [[ ! -e ${wildfly}/modules/org/postgresql/main/module.xml ]]; then
            echo "Configuring WildFly for EDS"
            wget -q -P /tmp/ "https://jdbc.postgresql.org/download/postgresql-${psqlVersion}.jar"
            runJbossCli "module add --name=org.postgresql --resources=/tmp/postgresql-${psqlVersion}.jar --dependencies=javaee.api"
            runJbossCli "/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-xa-datasource-class-name=org.postgresql.xa.PGXADataSource)"
            runJbossCli "data-source add --name=edsDS --driver-name=postgresql --jndi-name=java:/datasources/edsDS --connection-url=jdbc:postgresql://${dbHost}:${dbPort}/${dbName} --user-name=${dbUser} --password=${dbPassword} --use-ccm=false --max-pool-size=25 --blocking-timeout-wait-millis=5000 --enabled=true"
            runJbossCli "/subsystem=undertow/server=default-server/http-listener=default/:write-attribute(name=max-post-size,value=${maxPostSize})"
            runJbossCli "/subsystem=logging/logger=net.haugr.eds:add"
            rm -f "/tmp/postgresql-${psqlVersion}.jar"
            echo "Restarting WildFly ..."
            runJbossCli "reload"
            echo "WildFly has been configured"
        else
            echo "WildFly have already been configured"
        fi
    else
        echo "WildFly is not running, please start WildFly before configuring it"
    fi
elif [[ "${action}" == "status" ]]; then
    if (runJbossCli "read-attribute server-state"); then
        if (runJbossCli "deployment-info --name=eds.war"); then
            echo "WildFly is running with EDS deployed"
        else
            echo "WildFly is running"
        fi
    else
        echo "WildFly is not running"
    fi
elif [[ "${action}" == "start" ]]; then
    if (runJbossCli "read-attribute server-state"); then
        echo "WildFly is already running"
    else
        echo "Starting WildFly ..."
        if [[ "${debugPort}" == "" ]]; then
            "${wildfly}/bin/standalone.sh" -c standalone-full.xml -b 0.0.0.0 -Djboss.node.name=eds &
        else
            "${wildfly}/bin/standalone.sh" -c standalone-full.xml -b 0.0.0.0 -Djboss.node.name=eds --debug "${debugPort}" &
        fi
    fi
elif [[ "${action}" == "stop" ]]; then
    if (runJbossCli "read-attribute server-state"); then
        echo "Stopping WildFly ..."
        runJbossCli "shutdown"
    else
        echo "WildFly is not running"
    fi
elif [[ "${action}" == "deploy" ]]; then
    if (runJbossCli "read-attribute server-state"); then
        echo "Deploying EDS"
        runJbossCli "deploy $(dirname "${0}")/../wildfly/eds.war --force"
    else
        echo "WildFly is not running"
    fi
elif [[ "${action}" == "undeploy" ]]; then
    if (runJbossCli "read-attribute server-state"); then
        if (runJbossCli "deployment-info --name=eds.war"); then
            echo "Undeploying EDS"
            runJbossCli "undeploy eds.war"
        else
            echo "EDS was not deployed"
        fi
    else
        echo "WildFly is not running"
    fi
elif [[ "${action}" == "log" ]]; then
    tail -f "${wildfly}/standalone/log/server.log"
else
    echo "WildFly Control script"
    echo "Usage: $(basename "${0}") [Action]"
    echo
    echo "  The Action must be one of the following:"
    echo "    configure Attempts to configure a EDS WildFly instance"
    echo "    start     Attempts to start a EDS WildFly instance"
    echo "    stop      Attempts to stop the running EDS WildFly instance"
    echo "    status    Check the current status of WildFly"
    echo "    deploy    Deploy the latest EDS snapshot to WildFly"
    echo "    undeploy  Undeploy the currently deployed EDS snapshot"
    echo "    log       Tail on the Server Log"
    echo
fi
