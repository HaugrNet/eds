# Running EDS

EDS can be deployed using multiple runtime options. All require a PostgreSQL database.

## Prerequisites

### Java

EDS requires Java 21 or later.

### PostgreSQL Database Setup

Use the provided SQL scripts to set up the database:

```bash
# Run as postgres user (recommended)
sudo -u postgres psql --file accessories/release/postgresql/01-setup.sql

# Or if postgres has a Linux password
su postgres
psql postgres --file accessories/release/postgresql/01-setup.sql
exit
```

The `01-setup.sql` script automatically runs:
- `02-init.sql` - Creates database and user
- `03-create.sql` - Creates tables
- `04-update-*.sql` - Applies any schema updates

#### PostgreSQL Access Configuration

For fresh PostgreSQL installations, add to `/etc/postgresql/<VERSION>/main/pg_hba.conf`:

```
local   all     eds_user    password
```

Then restart PostgreSQL:

```bash
sudo service postgresql restart
```

## Option 1: WildFly Standalone Server

Deploy the WAR file to an existing WildFly installation using the provided control script.

### Build

```bash
mvn clean package -pl eds-wildfly -am -DskipTests
```

### Setup Environment

```bash
export JBOSS_HOME="/path/to/wildfly"
export PATH="${PATH}:$(pwd)/accessories/release/bin"
```

### Using the Control Script

```bash
cd accessories

# Start WildFly
wildfly.sh start

# Configure WildFly (downloads PostgreSQL driver, creates datasource)
wildfly.sh configure

# Deploy EDS
wildfly.sh deploy

# Check status
wildfly.sh status

# View logs
wildfly.sh log

# Undeploy
wildfly.sh undeploy

# Stop WildFly
wildfly.sh stop
```

#### Debug Mode

Start WildFly with remote debugging enabled:

```bash
export DEBUG_PORT=8787
wildfly.sh start
```

#### Script Configuration

Edit `accessories/release/bin/wildfly.sh` to customize:

| Variable | Default | Description |
|----------|---------|-------------|
| `dbHost` | localhost | Database host |
| `dbPort` | 5432 | Database port |
| `dbUser` | eds_user | Database user |
| `dbPassword` | eds | Database password |
| `dbName` | eds | Database name |
| `maxPostSize` | 26214400 | Max upload size (25 MB) |

## Option 2: WildFly Bootable JAR (Galleon)

Self-contained executable JAR with embedded WildFly server.

### Build

```bash
mvn clean package -pl eds-wildfly -am -DskipTests
```

### Run with Defaults

```bash
java -jar eds-wildfly/target/eds-runnable.jar
```

### Run with Custom Properties

Create a properties file (e.g., `eds.properties`):

```properties
# Database
eds.db.url=jdbc:postgresql://dbserver:5432/eds
eds.db.user=eds_user
eds.db.password=secret

# Server ports
jboss.http.port=8080
jboss.management.http.port=9990
```

Run with properties file:

```bash
java -jar eds-wildfly/target/eds-runnable.jar --properties=eds.properties
```

Or pass properties directly:

```bash
java -Deds.db.url=jdbc:postgresql://dbserver:5432/eds \
     -Deds.db.user=eds_user \
     -Deds.db.password=secret \
     -jar eds-wildfly/target/eds-runnable.jar
```

### Bootable JAR Properties

| Property | Default | Description |
|----------|---------|-------------|
| `eds.db.url` | jdbc:postgresql://localhost:5432/eds | Database JDBC URL |
| `eds.db.user` | eds_user | Database user |
| `eds.db.password` | eds | Database password |
| `jboss.http.port` | 8080 | HTTP port |
| `jboss.management.http.port` | 9990 | Management port |

## Option 3: Quarkus

Lightweight runtime using Quarkus framework.

### Build

```bash
mvn clean package -pl eds-quarkus -am -DskipTests
```

### Run with Defaults

```bash
java -jar eds-quarkus/target/eds-runnable.jar
```

### Run with Custom Properties

Pass properties via system properties:

```bash
java -Dquarkus.datasource.jdbc.url=jdbc:postgresql://dbserver:5432/eds \
     -Dquarkus.datasource.username=eds_user \
     -Dquarkus.datasource.password=secret \
     -Dquarkus.http.port=8080 \
     -jar eds-quarkus/target/eds-runnable.jar
```

Or use an external config file:

```bash
java -Dquarkus.config.locations=/path/to/application.properties \
     -jar eds-quarkus/target/eds-runnable.jar
```

### Quarkus Properties

| Property | Default | Description |
|----------|---------|-------------|
| `quarkus.http.port` | 8080 | HTTP port |
| `quarkus.http.root-path` | /eds | Application context path |
| `quarkus.datasource.jdbc.url` | jdbc:postgresql://localhost:5432/eds | Database URL |
| `quarkus.datasource.username` | eds_user | Database user |
| `quarkus.datasource.password` | eds | Database password |

## Option 4: Docker

Run EDS in a Docker container with embedded PostgreSQL and WildFly.

### Build and Run

```bash
cd accessories

# Build the Docker image and start container
./docker.sh build
```

### Docker Control Script

```bash
cd accessories

# Start stopped container
./docker.sh start

# Stop running container
./docker.sh stop

# View logs
./docker.sh logs

# Check status
./docker.sh status

# Enter container shell
./docker.sh interactive

# Remove container and image
./docker.sh remove
```

### Custom Port

```bash
export EDS_PORT=9080
./docker.sh build
```

## Verifying the Installation

After starting, verify EDS is running:

```bash
curl http://localhost:8080/eds/api/version
```

## FitNesse Integration Testing

EDS includes FitNesse-based integration tests that provide both testing and documentation.

### Prerequisites

Download FitNesse standalone JAR from http://fitnesse.org/ and place it in the `eds-fitnesse` directory (or create a symlink).

### Build Test Fixtures

```bash
mvn clean package -pl eds-fitnesse -am -DskipTests
```

### Start FitNesse

```bash
cd eds-fitnesse
java -jar fitnesse-standalone.jar -p 2080
```

Access the test suite at: http://localhost:2080/EDS

### Running Tests

- Tests can be run as a complete suite or individually
- Each test is independent and self-contained
- By default, tests run against `http://localhost:8080/eds`

### Configuration

The target EDS instance and request type (REST) can be configured in the setup section of the tests within FitNesse.

### Shutdown FitNesse

```bash
curl http://localhost:2080/?shutdown
```

Or visit http://localhost:2080/?shutdown in a browser.

## API Documentation

The REST API is documented in:
- `accessories/release/oas3.json` - OpenAPI 3.0 specification
- `accessories/release/swagger.json` - Swagger specification
