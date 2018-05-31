[![Build Status](https://api.travis-ci.org/JavaDogs/cws.svg)](https://travis-ci.org/JavaDogs/cws) [![CircleCI](https://circleci.com/gh/JavaDogs/cws.png?style=shield)](https://circleci.com/gh/JavaDogs/cws) [![Coverity](https://scan.coverity.com/projects/13955/badge.svg)](https://scan.coverity.com/projects/javadogs-cws) [![SonarQube](https://sonarcloud.io/api/project_badges/measure?project=io.javadog:cws&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.javadog:cws) [![Codacy](https://api.codacy.com/project/badge/Grade/78366d7059554164a3f65ceabe986598)](https://www.codacy.com/app/cws/cws) [![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/1566/badge)](https://bestpractices.coreinfrastructure.org/projects/1566) [![Software License](https://img.shields.io/badge/license-Apache+License+2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
--

# CWS - Cryptographic Web Store
[CWS](https://javadog.io/), Cryptographic Web Store, works like "PGP for the
Cloud". It is designed as a backend component, which can be integrated into
other systems or be used directly by other applications or apps. It us written
purely in Java / Java EE, and it is not containing any third-party dependencies.
It is build around the vision of allowing safe and easy exchange of data between
multiple parties, where data is stored encrypted.

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
Anyone for whom data protection is important may find CWS useful. It is designed
so everything is stored encrypted in a way, so only those who the data belongs
to may access it. This is achieved by ensuring that the full control over all
Keys is placed with the users.

# Security Features
Besides, encrypting all data stored, the CWS also has a number of features to
ensure that the security is as high as it can be, considering that it can be
deployed in a "hostile" environment.

 * MasterKey - The MasterKey is used to encrypt and decrypt various information
   and it must be set at startup, since it is not persisted anywhere.
 * Deleting sensitive data - Data, which is considered sensitive is actively
   being deleted once it is no longer needed. This includes the credentials,
   which a member gives to unlock their account. It should also include the
   symmetric and private keys, but due to a not implemented feature in Java,
   this is not the case.
 * Invalidate Accounts, this allows a member to force the keys to be replaced
   internally, so it is possible to log in and view Circles, but it will not be
   possible to extract data, as the keys are not the same as the ones, which
   was used in the Circles.
 * Data Corruption checks, to verify if there has been alterations to encrypted
   data, which may result in inability to properly extract information.

# Release Plan
This is CWS 1.1 development branch - it will focus on the following
features:
 * Copy Data from one Circle of Trust to a second, with a property flag to
   enable or disable the feature. By default, the feature is disabled.
 * Move Data from one Circle of Trust to a second, with a property flag to
   enable or disable the feature. By default, the feature is disabled. Further,
   the feature will require that the requesting Member is a Circle Administrator
   of the source Circle, and have write permissions in the target Circle.
 * Search Circles, Members & Data. Although, the CWS is storing very little
   information, it may still be necessary to search for somethnig using the
   simple names.
 * Re-key, i.e. force an update of the Keys used for a Circle or Member. This
   feature will have different ways to be triggered, either being forced with
   a request or it can be started if the key exceeds a certain age, the latter
   requires that the Circle is being used, as a re-key operation requires access
   to the existing key.

CWS 1.1 is not yet having a release date - but as the current feature list is
rather short, it should not take long before it is being released.

Besides these features and others which may be requested, the plan is to make
at first a simple command line tool to use the CWS with. This tool can be used
as the basis for other Clients, including mobile apps and websites.

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
