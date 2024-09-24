# EDS - Encrypted Data Share

[![CircleCI](https://circleci.com/gh/HaugrNet/eds.png?style=shield)](https://circleci.com/gh/HaugrNet/eds)
[![SonarQube](https://sonarcloud.io/api/project_badges/measure?project=net.haugr:eds&metric=alert_status)](https://sonarcloud.io/dashboard?id=net.haugr:eds)
[![SonarQube](https://sonarcloud.io/api/project_badges/measure?project=net.haugr:eds&metric=coverage)](https://sonarcloud.io/dashboard?id=net.haugr:eds)
[![Software License](https://img.shields.io/badge/license-Apache+License+2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Coverity](https://scan.coverity.com/projects/28136/badge.svg)](https://scan.coverity.com/projects/haugrnet-eds)

[EDS](https://haugr.net/), Encrypted Data Share, works as a "PGP for the
Cloud". It is designed as a backend component with the vision that it should be
possible to exchange data between multiple parties using encrypted storage,
where both the parties involved, and the data exchanged can be anything from
simple files to complex data objects - EDS only cares about bytes.

Via the public (REST based) API, it is possible to access the
internal logic, where keys are unlocked, based on user credentials and used to
encrypt and decrypt data, storing only encrypted keys and data. Using the same
basic mechanism as PGP, combining Asymmetric & Symmetric keys, it is possible
for multiple parties to exchange data safely and securely.

Since EDS only focused on bytes and does not have any care for more information,
it can be used to share either files between users, or data objects between apps
or applications. This makes EDS the perfect companion for anyone who have Data
Protection & Privacy concerns, such as GDPR.

EDS is written in Java 21 / Jakarta EE 10, with no third-part dependencies, meaning
it can run on any Host or in any Cloud where a Jakarta EE Container is
available. Currently, only the [PostgreSQL](https://www.postgresql.org/) database
is supported, but thanks to the flexibility of Jakarta EE, it is possible to use any
database desired. Testing of EDS has been done with [WildFly](http://www.wildfly.org/).

## Build, Install and Run

The final version 1.2 can be downloaded from [haugr.net](https://haugr.net/),
version 2.0 will be a major upgrade, with new domain, project name, and various
internal changes that makes it a breaking update. Initial development of version
2.0 has completed, with no more changes in the pipeline. Next phase will focus on
testing to guarantee that the stability will remain high. The build requires Java 21,
and [Maven](https://maven.apache.org/).

In the accessories/release folder, there is a number of files, which is used to
install and run EDS. Either by building EDS from scratch, using a local WildFly
instance, or even just use the provided Docker configuration.

### Docker

In the accessories folder, there is a docker.sh script. which can be used to
create a new EDS container, running on a port of your choice. The configuration
will download all the required components, and handle the details for you.

### Local Container

By adding the accessories/release/bin directory in your path, you can take
advantage of the provided WildFly script to help configure & deploy a local EDS
instance using your existing container.

### Build from scratch

**Developers only**: If you want to build from the source, run these steps to
create the deployable WAR package for WildFly:

```
cd [ /path/to/eds/sources ]
mvn clean verify
```

Using the WildFly script to deploy the correct WAR file.
Now copy the ```eds.war``` file in place.

```
export PATH=[ / path/to/eds/sources ]/accessories/release/bin:${PATH}
wildfly.sh configure
wildfly.sh deploy
```

If you have swagger installed, please load the swagger.json file from the
accessories/release folder, which will help you build a local client for the
JSON based REST API.

## Who is this for

Anyone for whom data protection is important may find EDS useful. It is designed
so everything is stored encrypted in a way, so only those who the data belongs
to may access it. This is achieved by ensuring that the full control over all
Keys is placed with the users.

## Security Features

Besides, encrypting all data stored, the EDS also has a number of features to
ensure that the security is as high as it can be, considering that it can be
deployed in a "hostile" environment.

* MasterKey - The MasterKey is used to encrypt and decrypt various information,
  and it must be set during startup, since it is not persisted anywhere.
* Invalidate Accounts, this allows a member to force the keys to be replaced
  internally, so it is possible to log in and view Circles, but it will not be
  possible to extract data, as the keys are not the same as the ones, which
  was used in the Circles.
* Data Corruption checks, to verify if there has been alterations to encrypted
  data, which may result in inability to properly extract information.

## Release Plan

This is the EDS 2.0 development branch, with the these features completed:

* Migrate code base to Java 21
* [Migrate code base from Java 8 to Java 11](https://github.com/HaugrNet/eds/issues/71)
* [Upgrade code base to Java 17](https://github.com/HaugrNet/eds/issues/82)
* [Migrate from Java EE 7 to Jakarta EE 8](https://github.com/HaugrNet/eds/issues/70)
* [Upgrade JavaEE/JakartaEE to JakartaEE 10](https://github.com/HaugrNet/eds/issues/81)
* [Convert Date Objects to Java 8+ Time Objects](https://github.com/HaugrNet/eds/issues/69)
* [Migrate to the haugr.net domain](https://github.com/HaugrNet/eds/issues/72)
* [Rename Packages & Project to EDS](https://github.com/HaugrNet/eds/issues/80)

## Wish to join

The overall quality of the software can only improve if more eyes look at it and
help with implementing features and adding more tests. If you wish to join,
please contact Kim Jensen (see below).

### Code Quality

Code quality if very important, which is why testing & quality checks are very
important. Unfortunately, with version 2.0, it is time to consider alternatives
to FitNesse & Coverity from Synopsis, since neither supports Java 21.

The code is checked against as many analysis tools as possible. The final
code must have 0 issues of any kind - before it is considered ready to be
submitting into the main repository.

### Documentation

Documentation of code is also important, having silly comments is meaningless,
but often it helps to add inline comments to explain intentions and reasoning
for choices made. Commenting out code is banned, if it is commented out, it
is meaningless and should be removed.

## Software License

The EDS is released under Apache License 2 or APL2.

## Contact

Kim dot Jensen at haugr dot net
