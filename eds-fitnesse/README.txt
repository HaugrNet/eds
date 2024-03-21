Note; FitNesse is a nice testing framework, which excelled in visualizing the
tests with documentation. Unfortunately, it seems that they do not wish to
remove the possibility for users with older Java versions to lose access to the
deprecated SecurityManager. This means that users of newer Java versions are
being forced to look around for alternatives.

See: https://github.com/unclebob/fitnesse/issues/1338

---- old documentation ----

This module contains the combined test and documentation of EDS written using
FitNesse. The FitNesse testing tool can be downloaded from http://fitnesse.org/

The tests have been verified against version 20190202. The Fixtures required to
run the FitNesse tests are also located in this module.

To build the FitNesse Test Fixtures for EDS, please use Maven:

$ mvn clean verify

Running FitNesse is done with the following command:

$ java -jar fitnesse-standalone.jar -p [ PORT ]

Please note that the tests requires the fixtures which is located in the
target folder after Maven have completed building them. So for the tests to
work, FitNesse must be started from this folder.

To access the FitNesse tests, go to http://localhost:[ PORT ]/EDS

Here, the tests can be started either as a suite or individually. All the tests
have been written, so they work independently. This comes with the cost that they
do take a bit longer than required. But for the sake of accuracy they have been
written so.

Note, by default, the tests are invoked against a standard local EDS instance,
which will be available on http://localhost:8080/eds. If this is not the case,
please correct it in the setup part of the tests. Additionally, the type of
request made (SOAP / REST) can also be controlled in the setup part of the
tests.

To shut down FitNesse, please invoke the following URL:
http://localhost:[ PORT ]/?shutdown
