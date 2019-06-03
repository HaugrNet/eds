FROM ubuntu:18.04

RUN echo "Acquire::http::Proxy \"http://proxy.secunet.de:3128/\";" > /etc/apt/apt.conf
RUN apt update
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends tzdata
ENV TZ=Europe/Berlin
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN apt install -y postgresql
COPY pg_hba.conf /var/lib/postgresql/10/main/pg_hba.conf
COPY postgresql.conf.sample /var/lib/postgresql/10/main/postgresql.conf


COPY 0* /var/lib/postgresql/

USER postgres
RUN \
	/usr/lib/postgresql/10/bin/pg_ctl -D /var/lib/postgresql/10/main -l /var/lib/postgresql/logfile start; \
        cd /var/lib/postgresql; \
	psql -U postgres postgres < 01-install.sql; \
        psql -U cws_user -c "select * from cws_versions;" cws
	
USER root

COPY wildfly-16.0.0.Final.tar.gz /opt/
COPY OpenJDK11U-jre_x64_linux_hotspot_11.0.3_7.tar.gz /opt/

RUN cd /opt/; \
	tar xzvf wildfly-16.0.0.Final.tar.gz; \
	tar xzvf OpenJDK11U-jre_x64_linux_hotspot_11.0.3_7.tar.gz 

ENV JAVA_HOME=/opt/jdk-11.0.3+7-jre 
ENV JBOSS_HOME=/opt/wildfly-16.0.0.Final
ENV PATH=${JAVA_HOME}/bin:${JBOSS_HOME}/bin:${PATH}

RUN mkdir -p ${JBOSS_HOME}/modules/org/postgresql/main
COPY module.xml ${JBOSS_HOME}/modules/org/postgresql/main/
COPY postgresql-42.2.5.jar ${JBOSS_HOME}/modules/org/postgresql/main/
COPY cws.war ${JBOSS_HOME}/standalone/deployments
COPY standalone-full.xml ${JBOSS_HOME}/standalone/configuration
RUN chown -R postgres ${JBOSS_HOME}/standalone

EXPOSE 8080/tcp
USER postgres
ENTRYPOINT /usr/lib/postgresql/10/bin/pg_ctl -D /var/lib/postgresql/10/main -l /var/lib/postgresql/logfile start; \
           ${JBOSS_HOME}/bin/standalone.sh -c standalone-full.xml