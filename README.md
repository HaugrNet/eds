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

EDS is written in Java 25 / Jakarta EE 11, with no third-part dependencies, and
runs on [Quarkus](https://quarkus.io/), either as a JAR or as a container, on any
Host or in any Cloud. For production, it uses a
[PostgreSQL](https://www.postgresql.org/) database, and creates the tables itself;
without a database configured, it uses an in-memory H2 database, for tests and
demos. See [RUNNING.md](RUNNING.md) for details.

## Build, Install, and Run

The final version 1.2 can be downloaded from [haugr.net](https://haugr.net/),
version 2.0 will be a major upgrade, with a new domain, project name, and various
internal changes that make it a breaking update. Initial development of version
2.0 has completed. The current phase focuses on testing, and on working the
findings of that testing, to guarantee that the stability will remain high. The build requires
Java 25, and [Maven](https://maven.apache.org/).

In the root folder, there exists a small script, ```eds.sh```, which can be used to
build EDS and the stone alone, and to control the EDS container:

```bash
./eds.sh build [eds] [stone] [--test [--no-sonar]] [--sbom] [--podman]
./eds.sh start [stone] [--masterkey] | stop [--remove] | restart
./eds.sh status | logs
./eds.sh fitnesse start | fitnesse stop
./eds.sh masterkey generate [--force] | masterkey load
./eds.sh install [--remove] | completion
```

Without a name, ```build``` builds both; the tests only run with ```--test```.
With the tests of EDS, SonarQube analyses the code as well, and the build fails unless
its quality gate passes (```SONAR_HOST_URL``` and ```SONAR_TOKEN``` name the server and
the token; ```--no-sonar``` leaves it out).
```install``` puts ```eds.sh``` on the PATH, in ```~/.local/bin```, with its bash
completion. ```~/.bashrc``` is only changed when ```~/.local/bin``` isn't
on the PATH or bash-completion is missing, and ```install --remove``` takes all of it out
again. ```completion``` prints the bash completion of the script alone.

The stone alone is the server built as a JAR and a container image of its own
(```eds-quarkus/target/eds-stone.jar```, ```eds-stone:local```), which a host runs.
```build stone``` builds it, ```start stone``` runs it. See [RUNNING.md](RUNNING.md),
"The Stone Alone".

```eds.sh``` is for the machine EDS is built on. On a host which only runs a stone,
the script ```accessories/podman/stone``` is all that is needed
beside the image: it is put in the PATH, and sets up and controls the container as
a normal user. ```stone help setup``` tells the steps, from the database to HTTPS,
and which of them need root. See [RUNNING.md](RUNNING.md), "Setting Up a Stone on
a Host".

With ```--podman```, the Maven build runs in the Podman build container,
```build.container```, so only Podman is needed instead of a local Java. The
container image and the SBOM scans are still built and run on the host.

### OpenAPI Specification
The OpenAPI Specification of the REST API is ```eds-api/src/main/resources/openapi.json```.
It is the API contract: the build fails if the API in the code differs from it, so
every API change is also a reviewed change of this file. It can be used to generate
client code or documentation. After an intended API change, update it with
```mvn install -Deds.contract.update=true```.

### Software Bill of Materials (SBOM)
The build can generate [CycloneDX](https://cyclonedx.org/) SBOM files
(specification 1.6), in both JSON and XML format, via the
[CycloneDX Maven Plugin](https://github.com/CycloneDX/cyclonedx-maven-plugin).
SBOM generation is disabled by default, and is enabled with the ```sbom```
profile:

```bash
mvn clean package -Psbom -DskipTests
```

This produces the following files:

* ```target/eds-<version>-cyclonedx.(json|xml)``` – the aggregated SBOM,
  covering the modules of the project which are shipped; the test module
  (eds-testing) is left out.
* ```<module>/target/<module>-<version>-cyclonedx.(json|xml)``` – an SBOM for
  each individual module, e.g. ```eds-quarkus/target/eds-quarkus-2.0-SNAPSHOT-cyclonedx.json```.

The SBOM files are attached to the build, so they will also be installed or
deployed together with the other artifacts when running ```mvn install -Psbom```
or ```mvn deploy -Psbom -Prelease```. Test scoped dependencies are not included.

Note, the plugin needs online access to Maven repositories, so it is skipped
when Maven runs in offline mode (```-o```).

The CI Build scans the aggregated SBOM with [Grype](https://github.com/anchore/grype),
and fails if a vulnerability with severity High or Critical is found. The same
scan can be run locally with:

```bash
grype sbom:target/eds-2.0-SNAPSHOT-cyclonedx.json --fail-on high
```

Vulnerabilities which have been reviewed and found not to apply to EDS, can be
suppressed in ```.grype.yaml```, together with a justification.

#### Container SBOM
The Maven SBOM only covers the Java libraries. The EDS container image, built
from ```accessories/podman/Containerfile```, also contains operating
system packages and a Java runtime, so the CI Build generates a CycloneDX SBOM
for it with [Syft](https://github.com/anchore/syft), written to
```target/container-eds-quarkus-cyclonedx.json```. EDS does not ship a database
container, it uses an existing PostgreSQL database.

The container SBOM is scanned with Grype, and fails the build on High or
Critical findings, like the Maven SBOM. All SBOMs and vulnerability reports are
available as build artifacts. The container SBOM can be generated locally,
after the Maven build, with:

```bash
#  It's built with --format docker, because Podman's default OCI format silently drops the HEALTHCHECK
podman build --format docker -f accessories/podman/Containerfile -t eds-quarkus:local eds-quarkus/target
podman save --format oci-archive -o target/eds-quarkus.tar eds-quarkus:local
syft scan oci-archive:target/eds-quarkus.tar -o cyclonedx-json=target/container-eds-quarkus-cyclonedx.json
grype sbom:target/container-eds-quarkus-cyclonedx.json --fail-on high
```

All of the above, the Maven build, the container images, the SBOMs and the
scans, can also be run in one step, with the same pass/fail rules as the CI
Build. It requires Syft, Grype and jq, and Podman for the images:

```bash
./eds.sh build --sbom            # Maven & container SBOMs and scans
./eds.sh build eds --sbom        # Without the image of the stone alone
```

## Who is this for

Anyone for whom data protection is important may find EDS useful. It is designed
so everything is stored encrypted in a way, so only those who the data belongs
to may access it. This is achieved by ensuring that the full control over all
Keys is placed with the users.

## Design and plans

The design documents — what each part does, why, and what has been built — live in
[`accessories/design/`](accessories/design/README.md). Start with the README there
for a plain-language tour; the code refers to these documents by name (e.g.
`PLAN.md`, `E2E.md`).

## Security Features

Besides encrypting all data stored, the EDS also has a number of features to
ensure that the security is as high as it can be, considering that it can be
deployed in a "hostile" environment.

* MasterKey – The MasterKey is used to encrypt and decrypt various information,
  and it is given from a source at startup, or later with the MasterKey request
  while EDS stays locked, since it is not persisted anywhere.
* Invalidate Accounts, this allows a member to force the keys to be replaced
  internally, so it is possible to log in and view Circles. However, it will not
  be possible to extract data, as the keys are different from the ones that were
  used in the Circles.
* Data Corruption checks to verify if there have been alterations to encrypted
  data, which may result in inability to properly extract information.

## Release Plan

This is the EDS 2.0 development branch, with the these features completed:

* Migrate code base to Java 25
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
Unfortunately, with version 2.0, it is time to consider alternatives to
Coverity from Synopsis, which doesn't support Java 25. FitNesse runs on Java 25,
and its suites are part of every verification.

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
