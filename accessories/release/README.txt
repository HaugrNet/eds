Thanks for downloading and trying CWS, Cryptographic Web Share.

== Requirements ==

CWS requires:
* a database,
* Java 11+
* a Jakarta EE 9+ compatible container


== Test Installation Component Recommendations ==

Database (postgresql)
PostgreSQL can be downloaded from https://postgresql.org, as it is a standard
package for most Linux/*BSD distributions. If you do not already have postgresql
installed, follow the installation instructions for your platform.

Java 11 JDK
Java 11 is the latest Long Term Support version of Java, it can be downloaded
from https://adoptopenjdk.net/.

Jakarta EE 8 container (Wildfly)
The default container used for both development and testing is WildFly 
(https://wildfly.org) version 21+. If you do not already have Wildfly
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

To either create or update a CWS installation, the 01-setup.sql script should
be run. This will automatically trigger the other scripts. From a command
line, this can be done in one of the following two ways based on your postgres
user configuration:

If you run the postgres user as sudo (recommended):
$ sudo -u postgres psql --file ${CWS_HOME}/postgresql/01-setup.sql

Or, if you have given the postgres user a linux password:
$ cd postgresql
$ su postgres
$ psql postgres --file ${CWS_HOME}/postgresql/01-setup.sql
$ exit

Note; The error from creating the database may be ignored.

=== Step 3: Configure Wildfly ===

In your ${CWS_HOME} directory, there are several folders, as of CWS 1.1 both
Payara & WildFly are supported, in the bin directory, there is a shell script
to install the preferred version. The script requires that a few environment
variables are predefined, and can be used to start, stop, configure, deploy and
undeploy CWS.

The configure option will start the container and then apply the correct changes
to it. Once done, deploy and start/stop options should be all that is needed
to further the management of CWS. The scripts use files relative to the
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
$ export JBOSS_HOME="/opt/wildfly-21.0.2.Final"
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

Now, the CWS is offers a REST API available from port 8080. The swagger.json
 (Open API) file has a complete description of the API.

== Documentation ==

The API is documented in the accompanying JavaDoc, which is in the api.zip file.

== Contact ==

For more information, please contact Kim Jensen <kim.jensen@javadog.io>
