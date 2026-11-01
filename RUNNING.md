# Running EDS

EDS runs on Quarkus, either directly as a JAR, or as a container. It uses
PostgreSQL when configured, and otherwise an in-memory H2 database for tests
and demos.

## Prerequisites

### Java

EDS requires Java 25 or later.

### PostgreSQL Database Setup

PostgreSQL is only needed for production use; for tests and demos, EDS runs
without any database configuration, using the in-memory H2 database.

The PostgreSQL database, user and password are a prerequisite, provided by
whoever runs the database server. EDS does not need, and should not have,
the rights to create them. For example:

```sql
CREATE ROLE eds_user WITH LOGIN PASSWORD 'secret';
CREATE DATABASE eds WITH OWNER = eds_user;
```

Nothing else is needed: at startup, EDS detects an empty database, and
creates the tables itself, in a single transaction. If several EDS instances
start at the same time, only one of them creates the tables.

The schema is shared between PostgreSQL and the in-memory H2 database, see
`eds-core/src/main/resources/META-INF/eds-schema.sql`. EDS 2.0 requires a
fresh database, as earlier versions cannot be upgraded in place.

## Option 1: Quarkus

Lightweight runtime using the Quarkus framework. A single JAR supports both
databases, and selects one at startup:

| `EDS_DB_URL` | Database |
|--------------|----------|
| Set | PostgreSQL at the given JDBC URL. If it cannot be reached, EDS stays not ready and retries until it is reachable; it never falls back to H2. |
| Not set | In-memory H2. All data is lost when EDS stops, so this is only meant for tests and evaluation. |

For PostgreSQL, the role and database must exist, see
[PostgreSQL Database Setup](#postgresql-database-setup). If the schema is
missing, EDS creates it at startup, in a single transaction.

### Build

```bash
mvn clean package -pl eds-quarkus -am -DskipTests
```

### Run

```bash
# In-memory H2 database
java -jar eds-quarkus/target/eds-runnable.jar

# PostgreSQL
EDS_DB_URL=jdbc:postgresql://dbserver:5432/eds \
EDS_DB_USERNAME=eds_user \
EDS_DB_PASSWORD=secret \
java -jar eds-quarkus/target/eds-runnable.jar
```

The URL can also be given as system property, `-Deds.db.url=...`. Other
Quarkus properties can be passed as system properties, e.g.
`-Dquarkus.http.port=8080`, or via an external config file:

```bash
java -Dquarkus.config.locations=/path/to/application.properties \
     -jar eds-quarkus/target/eds-runnable.jar
```

### Properties

| Property | Default | Description |
|----------|---------|-------------|
| `EDS_DB_URL` / `eds.db.url` | (none, H2) | PostgreSQL JDBC URL, selects the database |
| `EDS_DB_USERNAME` | eds_user | PostgreSQL user |
| `EDS_DB_PASSWORD` | (empty) | PostgreSQL password |
| `EDS_DB_RETRY_INTERVAL` | 10s | How often to retry, while PostgreSQL cannot be reached |
| `quarkus.http.port` | 8080 | HTTP port |
| `quarkus.http.root-path` | /eds | Application context path (build time) |
| `quarkus.management.port` | 9000 | Management port, with the health endpoints |

## Option 2: Docker Compose

Run EDS as a single Quarkus container, using Docker Compose. EDS does not ship
a database container. Like the Quarkus JAR (see Option 1), it uses an existing
PostgreSQL database when `EDS_DB_URL` is set, and otherwise the in-memory H2
database.

### Prerequisites

- Docker and Docker Compose installed
- For PostgreSQL: a database, user and password, see
  [PostgreSQL Database Setup](#postgresql-database-setup); EDS creates the tables

### Configuration

Without configuration, the container starts with the in-memory H2 database.
For PostgreSQL, create `accessories/docker/.env` with the database connection:

```bash
# PostgreSQL connection
EDS_DB_URL=jdbc:postgresql://host.docker.internal:5432/eds
EDS_DB_PASSWORD=...
EDS_DB_USERNAME=eds_user        # Default: eds_user

# Optional
EDS_CERTS_DIR=/path/to/certs    # Enables TLS, together with:
EDS_TLS_CERT=/app/certs/tls.crt
EDS_TLS_KEY=/app/certs/tls.key
EDS_INSECURE_REQUESTS=enabled   # enabled | redirect | disabled
```

Inside the container, `localhost` is the container itself. For a PostgreSQL
running on the Docker host, use `host.docker.internal`, which the Compose
setup maps to the host. PostgreSQL must then also accept connections from the
Docker network (`listen_addresses` in `postgresql.conf` and a matching entry
in `pg_hba.conf`).

### Control Script

The containers are managed with `eds.sh` from the project root:

```bash
./eds.sh start     # Build the JAR if missing, start the container, wait until ready
./eds.sh stop      # Stop the container
./eds.sh restart   # Restart the container
./eds.sh status    # Health and version of the running container
./eds.sh logs      # Follow the container logs
./eds.sh remove    # Remove the container and image
```

### Health Checks

The health endpoints are served on the management port `9000`, which is only
exposed to the container network, not published to the host:

| Endpoint | Purpose |
|----------|---------|
| `/q/health/live` | Liveness, the application is running |
| `/q/health/ready` | Readiness, the database and EDS are ready |
| `/q/health/started` | Startup completed |

The container `HEALTHCHECK` uses the readiness endpoint. For Kubernetes, use
the same endpoints for the probes, on port `9000`.

### Data Persistence

With PostgreSQL, all data is stored in the database; with H2, it is lost when
the container stops. The container only writes its logs to a local bind
mount, `accessories/docker/volumes/logs/` (`eds.log`).

## Verifying the Installation

After starting, verify EDS is running:

```bash
curl -X POST -H "Content-Type: application/json" -d '{}' http://localhost:8080/eds/version
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

The fitnesse binary can be found at https://fitnesse.org/.

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
- `accessories/release/openapi.json` - OpenAPI 3.0 specification
