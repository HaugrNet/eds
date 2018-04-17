[CWS](https://javadog.io/) [![Build Status](https://api.travis-ci.org/JavaDogs/cws.svg)](https://travis-ci.org/JavaDogs/cws) [![CircleCI](https://circleci.com/gh/JavaDogs/cws.png?style=shield)](https://circleci.com/gh/JavaDogs/cws) [![Coverity](https://scan.coverity.com/projects/13955/badge.svg)](https://scan.coverity.com/projects/javadogs-cws) [![SonarQube](https://sonarcloud.io/api/project_badges/measure?project=io.javadog:cws&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.javadog:cws) [![Codacy](https://api.codacy.com/project/badge/Grade/78366d7059554164a3f65ceabe986598)](https://www.codacy.com/app/cws/cws) [![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/1566/badge)](https://bestpractices.coreinfrastructure.org/projects/1566) [![Software License](https://img.shields.io/badge/license-Apache+License+2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
--

# CWS - Cryptographic Web Store
CWS, Cryptographic Web Store, works like "PGP for the Cloud". It is designed as
a backend component, which can be integrated into other systems or be used
directly by other applications or apps. It us written purely in Java / Java EE,
and it is not containing any third-party dependencies. It is build around the
vision of allowing safe and easy exchange of data between multiple parties,
where data is stored encrypted.

The CWS focuses on Circles of Trust, where Members of a Circle will have varying
level of access to the Data belonging to the Circle. This is achieved by a
combination of Symmetric and Asymmetric Encryption, guaranteeing that only
Members with Access can view Data. Storage is safe as all data is stored
encrypted.

Communication is achieved via a public API, which allows both REST and SOAP
based WebService requests. This will give a high degree of flexibility for
anyone to integrate it into their system.

# Build, Install and Run
The current version of CWS has reached the point where it can build, deploy and
run in [WildFly](http://www.wildfly.org/) using PostgreSQL as database. To test,
please make sure that you have Java (8+), [Maven](https://maven.apache.org/) and
[PostgreSQL](https://www.postgresql.org/) installed and running, as well as a
local copy of the CWS sources.

In the accessories folder, you can find the configuration for WildFly 10 and 11,
the files are located in the same folder structure as you need to add them to
your local WildFly installation.

The database files can also be found in the accessories module under
`configuration/postgresql` where the `01-install.sql` script will create
the database, user (with password) and setup the database with tables &amp; data
for the initial run.

Then do the following:

First, setup your database: The following will create a database named cws and a
user named cws_user as well as set up all needed tables. Note, that re-running
the script will destroy an existing CWS database. For the initial release, no
update scripts will be made, unless requested.

```
$ cd [ /path/to/cws/files/ ] accessories/postgresql
$ psql postgres --file 01-install.sql
```

**Developers only**: If you want to build from source, run these steps to create the cws.war package:

```
$ cd [ /path/to/cws/sources ]
$ mvn clean verify
```

Now copy the cws.war file in place.

```
$ cp cws.war ${WILDFLY_HOME}/standalone/deployments
```

*Note: if you built it from scratch, you will probably find it at cws-war/target/cws.war*

And finally we need to also copy the wildfly configuration into place:

```
$ cd wildfly-[WILDFLYVERSION]
$ cp -R * ${WILDFLY_HOME}
```

*Note: If you built from scratch, the configuration is located under accessories/configuration/.*

And now you can start the server with this command:

```
$ ${WILDFLY_HOME}/bin/standalone.sh -c standalone-cws.xml
```

Now, you should have a running version of CWS which can be reached from the
following SOAP based URL's:

```
http://localhost:8080/cws/management?wsdl
http://localhost:8080/cws/share?wsdl
```

# Who is this for
It is not designed for anyone particular, but as it provides a rather general
API and scales depending on the deployment - it can be used by anyone who finds
it useful. 

# Release Plan
CWS is feature complete, version 0.9.9 has been released, and now it is just a
question of time before [Circle-CI](https://circleci.com) &amp;
[Travis-CI](https://travis-ci.org) upgrade their Build Images to use the latest
Java 8. Once they have upgraded, the default settings will be changed to
ensure that the default key size for the symmetric keys is set to the highest
value possible.

In the meantime, testing will continue to be improved, by adding a new external
test suit using [FitNesse](http://fitnesse.org/), and also trying to perform
Penetration Testing. Bugs and flaws will still be corrected, but no new features
or changes will be made.

# Wish to join
The overall quality of the software can only improve if more eyes look at it and
help with implementing features and adding more tests. If you wish to join,
please contact Kim Jensen (see below).

### Code Quality
Code quality if very important, which means that we aim at 200% test coverage,
which may sound ridiulous. However, it is meant in the way that we aim at having
both standard tests (JUnit) and external testing (FitNesse), where both suites
are covering as much as possible. Of course, external testing cannot and should
not cover the same bases as the standard tests.

The tests written, have been written using JUnit only, i.e. no mocking. Mocking
is unreliable in testing, as you simply test your mocks rather than your logic.
For that reason, CWS is using faking for tests, meaning that the buildup of the
expected result will demand a bit more work, but as the code is properly tested,
it also has the added advantage of giving more reliable tests.

The code is also checked against as mane analysis tools as possible. The final
code must have 0 issues of any kind - before it is considered ready for
submitting into the main repository.

### Documentation
Docummentation of code is also important, having silly comments is meaningless,
but often it helps to add inline comments to explain intentions and reasoning
for choices made. But, commenting out code is banned, if it is commented out, it
is meaningless and should be removed.

### Style Guide
The style guide is simple, it is more or less the default from IntelliJ. Only
important guideline is don't use tabs. mixing tabs and spaces means that the
code looks unreadable, and the default indentation is made with spaces. All
Classes have a default header, which must also be applied.

# Software License
The CWS is released under Apache License 2 or APL2.

# Contact
Kim.Jensen at javadog.io
