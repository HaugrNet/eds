# CWS Specifications
The CWS, Cryptographic Web Store, is a simple Open Source Project which aim at
being a usable Microservice for other systems, to store data in a secure manner.

The idea behind it, is to provide a webservice, which delivers some of the
features which [PGP](https://www.symantec.com/products/information-protection/encryption)
or [GnuPG](https://gnupg.org/). Meaning a way to communicate data securily with
others. Initially, the focus will purely be on having Groups and Members of
these share data securely. By means of both Symmetric and Asymmetric encryption
with Group Members acting as Recipients with various level of Trust.

Please be aware, that this document will evolve together with the progress of
the CWS Project, as it is being developed using an evolutionary approach,
meaning that features are not set in stone from the beginning but constantly
refined as part of the development. This also means that the specifications will
have to evolve similarly.

## 1.0 Technologies
The CWS will be 100% Java based, and utilize existing standard Java Technologies
to implement the core parts of the CWS, such as the Java Cryptography Extension
or JCE. To turn CWS into a server component, Java EE is used. For the first
version of CWS, the following Standard Versions will be used:
* Java SE 8 (JCE)
* Java EE 7 (REST, SOAP, JPA, JTA, CDI)

It is a decision to limit code to these components, and thus avoid any
third-party library dependencies, which will require additional Security reviews
and updates.

The CWS uses standard Java Logging, but generally, this logging is considered
relevant for tracing errors and development, and should not be activated in any
production environments.

## 2.0 API
The standard API is exposed as a REST or SOAP based WebService - however, it is
important to understandand the underlying Data Model with the Validation and
MetaData Information.

#### 2.1 Features
The features offered, should be able to provide all necessary steps from setting
up the system initially to allow users a secure sharing of data. Meaning that
the initial list of features will include the following:
 * Process Member (Add, Alter, Remove)
 * Retrieve Members (with Filtering)
 * Process Group (Create, Alter, Remove)
 * Retrieve Groups (with filtering)
 * Process Group Membership (Add, Alter, Remove)
 * Process Group Data (Add, Update, Remove)
 * Retrieve Group Data (with Filtering)

#### 2.2 Data Model
There is two Data Layers in the CWS, the initial is the API Data Layer, which
will offer a set of Request and Response Objects for each public method,
complete with Data Transfer Objects (DTO's).

The internal User Group Model with Permissions as well as Administrative
features will also be exposed via the API.

#### 2.2.1 Validation
To avoid needless Cryptographic and Database Operations, a request can only be
processed if the required data is present in a usable form. Meaning, that before
any request is made, it is running through a validation process that will reject
any request with missing or incomplete data.

#### 2.2.2 MetaData Information
The CWS stores and shares data in an encrypted form. Meaning, that it is up to
the Applications using the CWS to decide what kind of data is stored and shared
between Users and Groups.

However, Software is evolving, and any Application using the CWS may soon have
changes in their data - to make it easier to ensure that all Applications can
handle the same data correctly, All encrypted data is stored with a Version
based MetaData Information, which contain some simple ways of mapping over data.


## 3.0 Testing and Static Analysis
Testing is traditionally made on different levels, with the most basic being
Unit testing.

Certain forms of Testing is initially discarded. Regression Testing will be
since once a later version of the CWS is under development. If problems exist
with performance, Performance Testing can also be since, but initially it is not
considered.

#### 3.1 Unit Testing
All standard testing will be done using JUnit. The Coverage of the code with
JUnit tests should be no less than 80%.

#### 3.2 FitNesse Testing
[FitNesse](http://fitnesse.org/) is a convenient way to combine active testing
with documentation. Using this will allow developers who wishes to add the CWS
to their Infrastructure to see how it can be achieved while also see what will
happen with various requests.

Since [FitNesse](http://fitnesse.org/) is intended as Blackbox Acceptance tests,
they may not be able to reach the same coverage level as the JUnit tests, since
they cannot simulate the different types of errors which can occur. However, it
should nevertheless be possible to have at least 65% coverage.

#### 3.3 Static Analysis
Many Static Analysis tools exist to test systems. For the CWS, the following has
been chosen since they can be integrated into an Open Source Build Process and
give feedback regarding the outcome:
 * SonarQube
 * Coverity
 * Codacy
