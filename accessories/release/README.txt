Thanks for downloading and testing the fourth Beta version of CWS, Cryptographic
Web Store or CryptoStore.


== Requirements ==

CWS requires:
* a database, 
* Java 8
* a Java EE 7+ container 


== Test Installation Component Recommendations ==

Database (postgresql)
PostgreSQL can be downloaded from https://postgresql.org, as it is a standard
package for most Linux/*BSD distributions. If you do not already have postgresql
installed, follow the installation instructions for your platform.

Java 8 JDK (not Java 9)
Java 8 is also a standard package, but can also be downloaded from 
http://www.oracle.com/technetwork/java. Please be aware that CWS only works with 
Java 8, not yet Java 9. If you do not have java 8, follow the instructions for
your platform to install or upgrade java.

Java EE 7+ container (Wildfly)
The default container used for both development and testing is WildFly 
(https://wildfly.org) version 10 & 11. If you do not already have Wildfly 
installed, download Wildfly and unpack it.  Follow the instructions on setting
up Wildfly as a standalone server.


== CWS Installation ==

To correctly install CWS, first the database must be setup.

Step 1: Configure access rights for cws_user

If you have a fresh installation of postgresql, the pg_hba.conf file will need 
to be updated with the database user we are about to create. Use a text editor 
to edit /etc/postgresql/<VERSION>/main/pg_hba.conf. You will need root rights 
to edit this file.

In pg_hba.conf directly after the following line:

# "local" is for Unix domain socket connections only

Add the following entry:

local	all		cws_user				password

Then restart the postgresql service. 

$ service postgresql restart

Please keep in mind that this configuration should not be used in a production 
environment. Refer to the postgresql documentation for recommendations on a more 
secure user access configuration.

Step 2: Run the database setup scripts

Next run the psql scripts in the "postgresql" directory of your cws installation.  

$cd /{CWS_HOME}/postgresql/ 

In this directory, start the 01-install.sql script should be run.  This will 
automatically trigger the two other scripts. From a command line, this can be done 
in one of the following two ways based on your postgres user configuration:

If you run the postgres user as sudo (recommended):
$ cd ${CWS_HOME}/postgresql
$ sudo -u postgres psql --file 01-install.sql

Or, if you have given the postgres user a linux password:
$ cd postgresql
$ su postgres
$ psql postgres --file 01-install.sql
$ exit

You will be prompted for a password while the script is running, enter "cws" as
the password unless you have modified the sql script and changed the cws_user 
password to something else.

Step 3: Configure Wildfly

In your ${CWS_HOME} directory, there are two folders: wildfly-10 and wildfly-11.
The configuration files needed for a cws installation are contained in these 
directories. You will need to copy these files from these directories to your 
Wildfly installation.  Pick the directory that matches the version of your 
Wildfly installation and perform the following.

$ cd ${CWS_HOME}/wildfly-1X/modules
$ sudo cp -r org ${JBOSS_HOME}/modules
$ cd ${CWS_HOME}/wildfly-1X/standalone 
$ sudo cp configuration/standalone-cws.xml ${JBOSS_HOME}/standalone/configuration/standalone-cws.xml

This will then install the PostgreSQL database driver and add a custom CWS 
standalone script for starting WildFly. Once done, simply copy the cws.war file 
into the WildFly as follows:

$ sudo cp ${CWS_HOME}/cws.war ${JBOSS_HOME}/standalone/deployments


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
