# [![Coverity Status](https://scan.coverity.com/projects/13955/badge.svg)](https://scan.coverity.com/projects/javadogs-cws) [![SonarQube Badge](https://sonarcloud.io/api/badges/gate?key=io.javadog:cws)](https://sonarcloud.io/dashboard?id=io.javadog:cws) [![GitHub license](https://img.shields.io/badge/license-Apache+License+2.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

# CWS - Cryptographic Web Store
CWS, Cryptographic Web Store or CryptoStore, is designed to be used as a backend
component in other systems. Facilitating protected storing, sharing or
collaboration of data between entrusted parties. It is written purely in Java EE,
allowing it to be deployed in a wide range of different environments, depending
on the need.

The CWS focuses on Circles of Trust, where Members of a Circle will have varying
level of access to the Data belonging to the Circle. This is achieved by a
combination of Symmetric and Asymmetric Encryption, guaranteeing that only
Members with Access can view Data. Storage is safe as all data is stored
encrypted.

Communication is achieved via a public API, which allows both REST and SOAP
based WebService requests. This will give a high degree of flexibility for
anyone to integrate it into their system.

# Build, Install & Run
The current version of CWS has reached the point where it can build, deploy and
run in [WildFly](http://www.wildfly.org/) using PostgreSQL as database. To test,
please make sure that you have Java (8+), [Maven](https://maven.apache.org/) and
[PostgreSQL](https://www.postgresql.org/) installed and running, as well as a
local copy of the CWS sources.

In the accessories folder, you can find the configuration for WildFly 10, the
files are located in the same folder structure as you need to add them to your
local WildFly installation.

The database files can all be found in the cws-model module under
`src/main/resources/postgresql` where the `01-install.sql` script will create
the database, user (with password) and setup the database with tables & data for
the initial run.

Then do the following:

```
$ cd /path/to/cws/sources
$ mvn clean install
$ cp cws-war/target/cws.war ${WILDFLY_HOME}/standalone/deployments
$ ${WILDFLY_HOME}/bin/standalone.sh -c standalone.xml
```
Now, you should have a running version of CWS which can be reached from the
following SOAP based URL's:

```
http://localhost:9080/cws/system?wsdl
http://localhost:9080/cws/share?wsdl
```

# Who is this for
It is not designed for anyone particular, but as it provides a rather general
API and scales depending on the deployment - it can be used by anyone who finds
it useful. 

# Release Plan
The code has reached a feature freeze, with most of the code looking close to
being production ready. Testing and bug fixing is well underway and should be
completed by the end of October, 2017.

Support for third-party analysis tools is pending as is testing under various
Application & Cloud Servers and Databases.

The initial plan of having a final 1.0 release ready by the end of 2017 remains
the goal and with the current level of progress, it is still feasible.

# Software License
The CWS is released under Apache License 2 or APL2.

# Contact
Kim Jensen <kim at javadog.io>
