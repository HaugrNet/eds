# EDS – Encrypted Data Share

[![CircleCI](https://circleci.com/gh/HaugrNet/eds.png?style=shield)](https://circleci.com/gh/HaugrNet/eds)
[![SonarQube](https://sonarcloud.io/api/project_badges/measure?project=net.haugr:eds&metric=alert_status)](https://sonarcloud.io/dashboard?id=net.haugr:eds)
[![SonarQube](https://sonarcloud.io/api/project_badges/measure?project=net.haugr:eds&metric=coverage)](https://sonarcloud.io/dashboard?id=net.haugr:eds)
[![Software License](https://img.shields.io/badge/license-Apache+License+2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Coverity](https://scan.coverity.com/projects/28136/badge.svg)](https://scan.coverity.com/projects/haugrnet-eds)

[EDS](https://haugr.net/), Encrypted Data Share, works as a "PGP for the
Cloud." It is designed as a backend component with the vision that it should be
possible to exchange data between multiple parties using encrypted storage.
The data exchanged between the parties can be anything from simple files to
complex data objects – EDS only cares about bytes.

Via the public (REST-based) API, it is possible to access the
internal logic, where keys are unlocked, based on user credentials, and used to
encrypt and decrypt data, storing only encrypted keys and data. Using the same
basic mechanism as PGP, combining Asymmetric & Symmetric keys, it is possible
for multiple parties to exchange data safely and securely.

Since EDS only focused on bytes and does not have any care for more information,
it can be used to share either files between users or data objects between apps
or applications. This makes EDS the perfect companion for anyone who has Data
Protection & Privacy concerns, such as GDPR.

EDS is written in Java 21 / Jakarta EE 11, with no third-part dependencies, meaning
it can run on any Host or in any Cloud where a Jakarta EE Container is
available. Currently, only the [PostgreSQL](https://www.postgresql.org/) database
is supported, but thanks to the flexibility of Jakarta EE, it is possible to use any
database desired. Testing of EDS has been done with [WildFly](http://www.wildfly.org/) and
[Quarkus](https://quarkus.io/).

## Build, Install, and Run

The final version 1.2 can be downloaded from [haugr.net](https://haugr.net/),
version 2.0 will be a major upgrade, with a new domain, project name, and various
internal changes that make it a breaking update. Initial development of version
2.0 has completed, with no more changes in the pipeline. The next phase will focus
on testing to guarantee that the stability will remain high. The build requires
Java 21, and [Maven](https://maven.apache.org/).

In the root folder, there exists a small script, ```eds.sh```, which can be used to
build the entire project, including running all tests and quality checks.

### OpenAPI Specification
in the accessories/release folder, there is an OpenAPI Specification file,
```openapi.json```, which can be used to generate client code or documentation
for the REST API.

## Who is this for

Anyone for whom data protection is important may find EDS useful. It is designed
so everything is stored encrypted in a way, so only those who the data belongs
to may access it. This is achieved by ensuring that the full control over all
Keys is placed with the users.

## Security Features

Besides encrypting all data stored, the EDS also has a number of features to
ensure that the security is as high as it can be, considering that it can be
deployed in a "hostile" environment.

* MasterKey – The MasterKey is used to encrypt and decrypt various information,
  and it must be set during startup, since it is not persisted anywhere.
* Invalidate Accounts, this allows a member to force the keys to be replaced
  internally, so it is possible to log in and view Circles. However, it will not
  be possible to extract data, as the keys are different from the ones that were
  used in the Circles.
* Data Corruption checks to verify if there have been alterations to encrypted
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

Code quality is important, which is why testing and quality checks are crucial.
Unfortunately, with version 2.0, it is time to consider alternatives
to FitNesse & Coverity from Synopsis, since neither supports Java 21.

The code is checked against as many analysis tools as possible. The final
code must have zero issues of any kind before it is considered ready to be
submitted into the main repository.

### Documentation

Documentation of code is also important, having silly comments is meaningless,
but often it helps to add inline comments to explain intentions and reasoning
for choices made. Commenting on code is banned, if it is commented out, it
is meaningless and should be removed.

## Software License

The EDS is released under Apache License 2 or APL2.

## Contact

Kim dot Jensen at haugr dot net
