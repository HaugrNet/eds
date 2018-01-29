Thanks for downloading and testing the fourth Beta version of CWS, Cryptographic
Web Store or CryptoStore.

== Requirements ==
CWS Requires a database, Java 8 and a Java EE 7+ Container to run, the default
container used for both development and testing is WildFly (https://wildfly.org)
version 10 & 11. in the wildfly-10 and wildfly-11 folders, you can find the
PostgreSQL database driver and the standard configuration file used for a
standalone installation.

PostgreSQL can be downloaded from https://postgresql.org, as it is a standard
package for most Linux/*BSD distributions. Java 8 is also a standard package,
but can also be downloaded from http://www.oracle.com/technetwork/java. Please
be aware that CWS only works with Java 8, not yet Java 9.

== Installation ==
To correctly install CWS, first the database must be setup, this can be done by
using the files under "postgresql". Simply run the 01-install.sql script, which
will automatically include the two other scripts. From a command line, this can
be done as follows:

$ cd postgresql
$ psql postgres --file 01-install.sql

WildFly can be configured by copying the correct WildFly folder scructure into
the WildFly instance you have, the files are placed in the correct subfolders,
reflecting the WildFly folder structure. This will then install the PostgreSQL
database driver and add a custom CWS standalone script for starting WildFly.

Once done, simply copy the cws.war file into the WildFly as follows:

$ cp cws.war ${JBOSS_HOME}/standalone/deployments

== Running ==

With the requirements in place, and the installation completed - it is possible
to start CWS, this is done as follows:

$ ${JBOSS_HOME}/bin/standalone.sh -c standalone-cws.xml

Now, the CWS is running with both a SOAP and REST API available from port 8080,
the WSDLs can be accessed from the following URLs:

http://localhost:8080/cws/management?wsdl
http://localhost:8080/cws/share?wsdl

== Documentation ==

The API is documented in the accompanying JavaDoc, which is in the api.zip file.

== Release plan ==

This release is version 0.9.0, which is the feature freeze version. Pending for
the final 1.0 release, is the last external pen-testing results, and a review
of the documentation. February 2nd, version 0.9.9 will be released, which is the
final release candidate, differing from the final 1.0.0 release in the default
key settings only. The 0.9.9 will refer to 128 bit AES keys, and once the two
CIs (Travis & Circle) have upgraded their images to use Java 1.8.0_161 or later,
will the default settings be raised to 256 bit.

For more information, please contact Kim Jensen <kim.jensen@javadog.io>
