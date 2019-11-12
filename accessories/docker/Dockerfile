# ==============================================================================
# Docker file for CWS
# ------------------------------------------------------------------------------

# Building it, based on Ubuntu 18.04 LTS Bionic Beaver
FROM ubuntu:18.04

# The build script handles the potential Proxy settings needed
COPY docker/apt.conf /etc/apt/apt.conf
# Since both wget & PostgreSQL are needed, we first have to update
# the packaging system, so it has the latest of everything
RUN apt update \
  && DEBIAN_FRONTEND=noninteractive apt upgrade -y \
  && DEBIAN_FRONTEND=noninteractive apt install -y tzdata wget postgresql

# PostgreSQL require TimeZone information
ENV TZ=Europe/Berlin
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# If a proxy is needed, the build script will generate this file correctly
COPY docker/wgetrc /etc/wgetrc

# Setup PostgreSQL
USER postgres
COPY release/postgresql/*.conf /var/lib/postgresql/10/main/
COPY release/postgresql/*.sql /var/lib/postgresql/
RUN /usr/lib/postgresql/10/bin/pg_ctl -D /var/lib/postgresql/10/main -l /var/lib/postgresql/logfile start \
  && cd /var/lib/postgresql \
  && psql -U postgres postgres < 01-install.sql

# Download & install WildFly 18
USER root
RUN cd /opt \
  && wget -q https://download.jboss.org/wildfly/18.0.0.Final/wildfly-18.0.0.Final.tar.gz \
  && tar zxf wildfly-18.0.0.Final.tar.gz \
  && rm wildfly-18.0.0.Final.tar.gz
ENV JBOSS_HOME="/opt/wildfly-18.0.0.Final"

# Download & install Adopt OpenJDK 11
RUN cd /opt \
  && wget -q https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.5%2B10/OpenJDK11U-jre_x64_linux_hotspot_11.0.5_10.tar.gz \
  && tar zxf OpenJDK11U-jre_x64_linux_hotspot_11.0.5_10.tar.gz \
  && rm OpenJDK11U-jre_x64_linux_hotspot_11.0.5_10.tar.gz
ENV JAVA_HOME="/opt/jdk-11.0.5+10-jre"

# Update the path, to include both Java & WildFly
ENV PATH="${JAVA_HOME}/bin:${JBOSS_HOME}/bin:${PATH}"

# Now, configure WildFly, since PostgreSQL requires that the user is postgres,
# and it is irrelevant for WildFly, we're changing everything to postgres, so
# when the server is running, we simply start everything as postgres
RUN mkdir -p ${JBOSS_HOME}/modules/org/postgresql/main/ \
  && cd ${JBOSS_HOME}/modules/org/postgresql/main/ \
  && wget -q https://jdbc.postgresql.org/download/postgresql-42.2.8.jar
COPY release/wildfly/module.xml ${JBOSS_HOME}/modules/org/postgresql/main/
COPY release/wildfly/standalone-full.xml ${JBOSS_HOME}/standalone/configuration
COPY docker/cws.war ${JBOSS_HOME}/standalone/deployments
RUN chown -R postgres ${JBOSS_HOME}/standalone

# To be able to use CWS, we have to expose WildFly (HTTP on port 8080)
EXPOSE 8080/tcp

# Finally, we're starting the services
USER postgres
ENTRYPOINT /usr/lib/postgresql/10/bin/pg_ctl -D /var/lib/postgresql/10/main -l /var/lib/postgresql/logfile start \
  && ${JBOSS_HOME}/bin/standalone.sh -c standalone-full.xml -Djboss.node.name=cws -b 0.0.0.0
