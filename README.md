[![Build Status](https://api.travis-ci.org/JavaDogs/cws.svg)](https://travis-ci.org/JavaDogs/cws) [![CircleCI](https://circleci.com/gh/JavaDogs/cws.png?style=shield)](https://circleci.com/gh/JavaDogs/cws) [![Coverity](https://scan.coverity.com/projects/13955/badge.svg)](https://scan.coverity.com/projects/javadogs-cws) [![SonarQube](https://sonarcloud.io/api/project_badges/measure?project=io.javadog:cws&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.javadog:cws) [![Codacy](https://api.codacy.com/project/badge/Grade/78366d7059554164a3f65ceabe986598)](https://www.codacy.com/app/cws/cws) [![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/1566/badge)](https://bestpractices.coreinfrastructure.org/projects/1566) [![Software License](https://img.shields.io/badge/license-Apache+License+2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
--

# CWS - Cryptographic Web Store
[CWS](https://javadog.io/), Cryptographic Web Store, works like a "PGP for the
Cloud". It is designed as a backend component with the vision that it should be
possible to exchange data between multiple parties using encrypted storage,
where both the parties involved and the data exchanged can be anything from
simple files to complex data objects - CWS only cares about bytes.

Via the public API (REST or SOAP based WebServices), it is possible to access
the internal logic, where keys are unlocked based on user credentials and used
to encrypt and decrypt data, storing only encrypted keys and data. Using the
same basic mechanism as PGP, combining Asymmetric & Symmetric keys, it is
possible for multiple parties to exchange data safely and securely.

Since CWS only focused on bytes and does not have any care for more information,
it can be used to store either files between users or data objects between apps
or applications. This makes CWS the perfect companion for anyone who have Data
Protection & Privacy concerns, such as GDPR.

CWS is written in Java 8 / Java EE 7, with no third-part depedencies, meaning
that it can run on any Host or in any Cloud where a Java EE Container is
available. The first version is using [PostgreSQL](https://www.postgresql.org/)
as database, but thanks to the flexibility of Java EE, it is possible to use any
database desired. Testing of CWS has been done using both
[WildFly](http://www.wildfly.org/) and [Payara](https://payara.fish/).

# Build, Install and Run
The final version 1.0 of CWS can be downloaded from [JavaDog](https://javadog.io/),
version 1.1 is more or less feature complete and currently being tested. The aim
is to constantly have a stable and usable system, so if needed - please download
the sources and build CWS yourself. The build only requires Java JDK 8 (patch
level 161 or greater), and [Maven](https://maven.apache.org/).

In the accessories/release folder, there is a number of files, which is used to
install and run CWS. Please put the bin folder in your path, and then invoke
either the payara.sh or wildfly.sh script, which will provide the options for
configuring, deploying, starting & stopping CWS in either Payara or WildFly. The
scripts can also create the database, but it does require that you have the
correct permissions in PostgreSQL - see the README.txt file in the Accessories
folder for details regarding this.

**Developers only**: If you want to build from source, run these steps to
create the deployable WAR packages for either Payara or WildFly:

```
$ cd [ /path/to/cws/sources ]
$ mvn clean verify
```

Using the Payara or WildFly scripts to deploy the correct WAR file.
Now copy the cws.war file in place.

```
$ export PATH=[ / path/to/cws/sources ]/accessories/release/bin:${PATH}
$ payara.sh configure
$ payara.sh deploy
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
 * Removing sensitive data from memory - by overwriting it in memory, before it
   is being dereferenced. This should also include the keys, but due to a not
   implemented feature in Java, this is not the case.
 * Invalidate Accounts, this allows a member to force the keys to be replaced
   internally, so it is possible to log in and view Circles, but it will not be
   possible to extract data, as the keys are not the same as the ones, which
   was used in the Circles.
 * Data Corruption checks, to verify if there has been alterations to encrypted
   data, which may result in inability to properly extract information.

# Release Plan
This is the CWS 1.1 development branch, following features have been completed:
 * Copy Data from one Circle of Trust to a second, with a property flag to
   enable or disable the feature. By default, the feature is disabled.
 * Move Data from one Circle of Trust to a second, with a property flag to
   enable or disable the feature. By default, the feature is disabled. Further,
   the feature will require that the requesting Member is a Circle Administrator
   of the source Circle, and have write permissions in the target Circle.
 * External control of the MasterKey via a URI, so it can be referenced from a
   central source and destroyed centrally, rather than the Administrator having
   to unlock it manually.
 * New request - authenticated; simple check to verify if an Account has
   permissions to access the give CWS instance
 * Complete external testing with FitNesse.
 * AES GCM based encryption, as CBC based encryption is slowly being deprecated
   and instead GCM based encryption should be used. CWS 1.1 supports 128 bit
   GCM AES encryption.

As can be seen, CWS 1.1 release is feature complere, pending code review & pen
testing to be completed. Release date for CWS 1.1 is estimated to happen around
August 31st. Focus for version 2.0 is primarily on migrating the entire code
base to Java 11 and Java EE 8, however CWS 1.1 is fully compatible with both
Java 8 & 11.

# Wish to join
The overall quality of the software can only improve if more eyes look at it and
help with implementing features and adding more tests. If you wish to join,
please contact Kim Jensen (see below).

### Code Quality
Code quality if very important, which means that the goal is 200% test coverage,
which may sound ridiulous. However, so that both test suites (Junit & FitNesse)
will each cover as close to 100% as possible. Of course, external testing cannot
and should not cover the same bases as the standard tests.

The code is also checked against as many analysis tools as possible. The final
code must have 0 issues of any kind - before it is considered ready to be
submitting into the main repository.

### Documentation
Documentation of code is also important, having silly comments is meaningless,
but often it helps to add inline comments to explain intentions and reasoning
for choices made. But, commenting out code is banned, if it is commented out, it
is meaningless and should be removed.

### Style Guide
CWS is developed using IntelliJ IDEA & a local SonarQube instance. The styles
used and the SonarQube Quality Profile can be found in the accessories folder.

# Software License
The CWS is released under Apache License 2 or APL2.

# Contact
Kim.Jensen at javadog.io
