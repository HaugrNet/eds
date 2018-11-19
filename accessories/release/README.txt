Thanks for downloading and trying the first release of CWS, Cryptographic
Web Store.


== Requirements ==

CWS requires:
* a database,
* Java 8/11
* a Java EE 7+ container, compatible with Java 8/11


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

=== Fresh Installation ===

In this directory, the 01-install.sql script should be run.  This will
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

=== Update Installation ===

In this directory, the 03-update.sql script should be run. From a command line,
this can be done in one of the following two ways based on your postgres user
configuration:

If you run the postgres user as sudo (recommended):
$ cd ${CWS_HOME}/postgresql
$ sudo -u postgresql psql -U cws_user cws --file 03-update.sql

Or, if you have given the postgres user a linux password:
$ cd postgresql
$ su postgres
$ psql -U cws_user cws --file 03-update.sql
$ exit

You will be prompted for a password while the script is running, enter "cws" as
the password unless you have modified the sql script and changed the cws_user
password to something else.


=== Step 3: Configure Wildfly ===

In your ${CWS_HOME} directory, there are several folders, as of CWS 1.1 both
Payara & WildFly are supported, in the bin directory, there is a shell script
to install the preferred version. The scipt requires that a few environment
variables are predefined, and can be used to start, stop, configure, deploy and
undeploy CWS.

The configure option will start the container and then apply the correct changes
to it. Once done, the deploy and start/stop options should be all that is needed
to further the management of CWS. The scripts uses files relative to the
directory where it is installed, so it is important that they are being run
from their installed directory.

==== Example for Payara ====

The Payara installation is requiring that the system variable "GLASSFISH_HOME"
is being set to the installation directory of your Payara installation.
Following is an example:

$ cd [ /Path/To/CWS/Installation ]
$ export GLASSFISH_HOME="/opt/payara"
$ export PATH=${PATH}:`pwd`/bin
$ payara.sh configure
$ payara.sh deploy

Done, now you should have a running CWS installation using Payara as Container.
If there is any questions regarding the script, simply run it without arguments.

==== Example for WildFly ====

The WildFly installation is requiring that the system variable "JBOSS_HOME" is
being set to the installation directory of your WildFly (JBoss) installation.
Unfortunately, WildFly cannot be started & configured in a single action, hence
for WildFly the following is an example:

$ cd [ /Path/To/CWS/Installation ]
$ export JBOSS_HOME="/opt/wildfly-14.0.1.Final"
$ export PATH=${PATH}:`pwd`/bin
$ wildfly.sh start
$ wildfly.sh configure
$ wildfly.sh deploy

Done, now you should have a running CWS installation using WildFly as Container.
If there is any questions regarding the script, simply run it without arguments.

== Running ==

With the requirements in place, and the installation completed - it is possible
to start CWS, this is done as follows (add the two exports to your environment):

$ cd [ /Path/To/CWS/Installation ]
$ export GLASSFISH_HOME="/opt/payara"
$ export PATH=${PATH}:`pwd`/bin
$ wildfly.sh | payara.sh start

Now, the CWS is running with both a SOAP and REST API available from port 8080,
the WSDLs can be accessed from the following URLs:

http://localhost:8080/cws/management?wsdl
http://localhost:8080/cws/share?wsdl


== Documentation ==

The API is documented in the accompanying JavaDoc, which is in the api.zip file.


== Contact ==

For more information, please contact Kim Jensen <kim.jensen@javadog.io>
