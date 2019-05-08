FROM ubuntu:18.04

RUN apt update
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends tzdata
ENV TZ=Europe/Berlin
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN apt install -y postgresql
COPY pg_hba.conf /var/lib/postgresql/10/main/pg_hba.conf
COPY postgresql.conf.sample /var/lib/postgresql/10/main/postgresql.conf


COPY 0* /var/lib/postgresql/

USER postgres
RUN /usr/lib/postgresql/10/bin/pg_ctl -D /var/lib/postgresql/10/main -l /var/lib/postgresql/logfile start; \
        cd /var/lib/postgresql; \
	psql -U postgres postgres < 01-install.sql; \
        psql -U cws_user -c "select * from cws_versions;" cws
