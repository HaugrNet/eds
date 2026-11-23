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

The schema has the version 4. A database with another schema version is refused
at startup: EDS doesn't become ready, and says so in the log. Create a fresh
database for it. Until EDS 2.0 is released, the version stays 4 while the tables
may still change, so a database made by an earlier 2.0 build isn't recognised as
such: create it anew as well.

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
java -jar eds-quarkus/target/eds-rune.jar

# PostgreSQL
EDS_DB_URL=jdbc:postgresql://dbserver:5432/eds \
EDS_DB_USERNAME=eds_user \
EDS_DB_PASSWORD=secret \
java -jar eds-quarkus/target/eds-rune.jar
```

The URL can also be given as system property, `-Deds.db.url=...`. Other
Quarkus properties can be passed as system properties, e.g.
`-Dquarkus.http.port=8080`, or via an external config file:

```bash
java -Dquarkus.config.locations=/path/to/application.properties \
     -jar eds-quarkus/target/eds-rune.jar
```

### Properties

| Property | Default | Description |
|----------|---------|-------------|
| `EDS_DB_URL` / `eds.db.url` | (none, H2) | PostgreSQL JDBC URL, selects the database |
| `EDS_DB_USERNAME` | eds_user | PostgreSQL user |
| `EDS_DB_PASSWORD` | (empty) | PostgreSQL password |
| `EDS_DB_RETRY_INTERVAL` | 10s | How often to retry, while PostgreSQL cannot be reached |
| `EDS_MASTERKEY_URL` / `eds.masterkey.url` | (none, locked) | URL of the MasterKey source (e.g. `file:///run/secrets/eds-masterkey`), read at startup: a `file:` URL without a host, or `https:` (no redirects), of at most 16 KiB, read within 10 s. Without it, EDS stays locked until it is unlocked with the MasterKey request; see below |
| `quarkus.http.port` | 8080 | HTTP port |
| `EDS_TLS_CERT`, `EDS_TLS_KEY` | (none) | The certificate and its key, as files; with them, EDS serves HTTPS |
| `quarkus.http.ssl-port` | 8443 | HTTPS port |
| `EDS_INSECURE_REQUESTS` | `enabled` | Plain HTTP beside HTTPS: `enabled`, `redirect` or `disabled` |
| (fixed in the code) | `/` and `/api/v1` | Rune is served at the root; the REST API and the WebSocket are below `/api/v1` (`Constants.REST_API`). There is no context path; put a reverse proxy in front for another layout |
| `quarkus.management.port` | 9000 | Management port, with the health endpoints |

### Streaming of content

Large Data Objects are uploaded and downloaded as raw streams
(`PUT`/`GET /data/{dataId}/content`, up to `eds.stream.max.object.size`, default
1000 MiB). The JSON `data` field is limited to `eds.data.max.inline.size` (default
10 MiB).

The size of a request body:
- The uploads of content and thumbnails (`PUT /data/{dataId}/content`,
  `PUT /data/{dataId}/thumbnail`) are limited by their endpoints and by
  `quarkus.http.limits.max-body-size` (1100M), which must allow
  `eds.stream.max.object.size`.
- Every other request has a body of at most 16 MiB, or the Base64 of
  `eds.data.max.inline.size` plus 1 MiB if that is more. `Content-Length` is
  checked before anything is read: over the limit is answered with 413, a chunked
  body (HTTP/1.1) with 411. An HTTP/2 body without a length is counted while it is
  read, and answered with 413 once it passes the limit.

The streaming settings are EDS settings, changed with the settings request:

| Setting | Default | Description |
|---------|---------|-------------|
| `eds.stream.max.object.size` | 1048576000 | Largest content upload, in bytes |
| `eds.stream.transaction.timeout.seconds` | 3600 | Transaction timeout of a content upload, download, or sanity check of one object |
| `eds.stream.read.batch.size` | 4194304 | The size of the parts content is stored in, which is what one query writes or reads; kept within 64 KiB and 16 MiB. A change is for content written after it |
| `eds.data.max.inline.size` | 10485760 | Largest content in the JSON `data` field |
| `eds.data.max.total.size` | 0 | Ceiling for all stored content together, in bytes of encrypted content. `0` means the default for the kind of system: 2 GiB on a test system (in-memory H2), 250 GiB on a production system. A positive value sets it explicitly, as the opt-in to allow more |
| `eds.sanity.check.interval.days` | 180 | Days between checks of a Data Object; `0` switches the checks off |
| `eds.sanity.check.max.objects` | 1000 | Most Data Objects checked by one hourly run; `0` for no limit |

Content is stored in parts (`eds_data_parts`), a row for each, so no row and no
statement carries more than a part, and memory stays bounded by one, on PostgreSQL
and on H2. H2 is in memory as a whole all the same, and only for tests and small
trials. A long upload or download keeps
its transaction open, which holds back VACUUM on PostgreSQL. A copy or a move to
another Circle of an object above the inline limit is streamed: the source is read
on a second database connection while the re-encrypted copy is written, so such a
request uses two connections of the pool. EDS writes no temporary files.

`eds.data.max.total.size` puts a ceiling on all stored content, checked before
content is written (uploads, the JSON `data`, copies). Left at `0`, it is 2 GiB on a
test system (in-memory H2), which protects its memory, and 250 GiB on a production
system; set a positive value to allow more or less. A write above it fails with
`507` (`INSUFFICIENT_STORAGE`). The content which is replaced doesn't count, and a
move adds nothing. The check takes no lock, so writes running at the same time may
together exceed the ceiling by what they add. It counts the encrypted bytes, not
the database's own overhead (indexes, WAL, dead rows before VACUUM), so leave a
margin to the real space. If the database itself runs out of space, the write fails
and is rolled back.

### MasterKey

The MasterKey is the kill-switch of EDS: everything stored is protected by it,
and it is never stored. EDS derives it at startup from the content of
`EDS_MASTERKEY_URL` (environment), the system property `eds.masterkey.url`, or the
`eds.masterkey.url` setting, in that order. The content must be a high-entropy
secret (at least 15 characters, an estimated 128 bits or more). The source is a
`file:` URL without a host (a regular file, not a device or a pipe; symbolic links
are followed), or an `https:` URL, whose answer must be 200 without a redirect. It
may hold at most 16 KiB and must be read within 10 seconds, the TLS handshake
included; at most two sources are read at the same time. A source which doesn't
meet this leaves EDS locked, and the log says why, without the URL. The database only
holds a key check value, so a wrong source is detected at startup. There is no
default MasterKey: without a valid source, EDS stays locked, and requests fail with
CRYPTO_ERROR (591) until the System Administrator unlocks it with the MasterKey
request. On a fresh database, the first MasterKey (from the source or the request)
is set up as version 1.

### Rotating Circle Secrets

Each Circle has a Circle Secret, which its Data Objects are encrypted with. It is
rotated when a Trustee leaves or is removed (also when their account is deleted),
when a Circle administrator asks for it (`PUT /circles/{circleId}/rotation`), and a
period after the last rotation: the Circle gets a new secret, and its Data Objects
are re-encrypted with it in the background, while everything reads and writes as
usual. The stone can't read a Circle Secret by itself, so a rotation which is due
starts at the next request of a Trustee which reads the Circle's secret; a Circle
nobody uses waits. The Circle tells it in `rotationDue` and `rotation`, the status in
its inventory (`rotationsDue`, `rotationsRunning`, `rotationObjects`). Pending
invitations into a Circle are deleted when its secret is rotated.

| Setting | Default | Description |
|---------|---------|-------------|
| `eds.circle.secret.rotation.days` | 365 | Days from a new Circle, or the end of a rotation, until the next, 0 to 36500; `0` rotates only after a removal or when asked. A change applies from a Circle's next rotation on |
| `eds.circle.secret.rotation.threads` | 2 | Data Objects re-encrypted at a time by the rotation of a Circle, 1 to 64 |

A rotation re-encrypts every Data Object of the Circle, so it costs about as much as
reading and writing all of it once. Each object is re-encrypted in its own row, so
the database needs room for the objects re-encrypted at a time once more (on
PostgreSQL until VACUUM gives back the old rows), not for the Circle twice.

### Checking Passphrases

A passphrase is checked with PBKDF2, which costs about a fifth of a CPU-second.
The stone checks only so many at once; a request which finds no place in time is
answered `SERVICE_BUSY` (503) and may be sent again. With
`eds.auth.max.failed.attempts` above 0, too many failed attempts lock an account
for a while; the lock heals itself, and a new passphrase clears it.

| Setting | Default | Description |
|---------|---------|-------------|
| `eds.auth.concurrent.passphrases` | 0 | Passphrases checked at once, 0 to 1024; `0` is half the processors, at least one |
| `eds.auth.passphrase.wait.millis` | 2000 | How long a request waits for a place, 0 to 60000 ms |
| `eds.auth.max.failed.attempts` | 0 | Failed attempts within the window before the account is locked, 0 to 1000; `0` switches the lock off |
| `eds.auth.failed.window.minutes` | 15 | The window in which failed attempts are counted, 1 to 1440 |
| `eds.auth.lockout.minutes` | 15 | How long the lock lasts, 1 to 1440 |

### The System Administrator

A stone comes with no account: there is neither a default passphrase nor a
default account name which someone could have left as it was. Whoever first
comes to a stone without accounts, with a login or any other request which names
an account and a passphrase, becomes its System Administrator, under that name
and with that passphrase. `admin` is what the tests and the examples here use;
`sysop`, `dude` or any other name is as good.

- **Be the first.** Until someone has come, the stone is anybody's who reaches it.
  Make the first request right after the first start, before the stone is
  reachable from outside. The passphrase must meet the strength rules.
- **Only the first.** Once the stone has an account, an unknown name is refused,
  whatever it is; `admin` has no meaning of its own, and may be any member's name.
- **The System Administrator stays the first account**, whatever its name becomes:
  it may change its name like any member, but it can't be deleted and can't lose
  its role, also not by another account which was given the role. Only it may give
  or change the MasterKey.
- **A mistyped name** at the first request is the name of the System Administrator;
  it is corrected by changing the name, with the passphrase which was given.

**Several administrators.** A stone may have more than one System Administrator.
The first one, which claimed the stone, has a place no other has: besides the
MasterKey, it alone makes, unmakes and deletes administrators. The others run the
stone's users, rooms and settings: they create and delete users, but can't create
an administrator, change anyone's role, or delete an administrator.

- **Making one.** The first System Administrator creates the account with a
  passphrase and the role of an administrator (Rune: "New user", Tolk:
  `tolk user new <name> --admin`), or makes an existing user one (Rune: "Make
  administrator" under the user, Tolk: `tolk user role <name> admin`). The same
  takes the role back.
- **Invitations make users, always.** An invitation link can fall into the wrong
  hands, so the stone refuses one which asks for an administrator. A person who is
  to be an administrator gets an account with a passphrase, given over a channel
  the administrator trusts, or is invited and then given the role.
- **Who is who.** The list of users marks the first as "first admin" and the others
  as "admin"; the member data has `firstAdministrator`.

## Option 2: Podman Container

Run EDS as a single Quarkus container, with rootless Podman. EDS does not ship
a database container. Like the Quarkus JAR (see Option 1), it uses an existing
PostgreSQL database when `EDS_DB_URL` is set, and otherwise the in-memory H2
database. The image is defined in `accessories/podman/Containerfile`.

### Prerequisites

- Podman installed, running rootless
- For PostgreSQL: a database, user and password, see
  [PostgreSQL Database Setup](#postgresql-database-setup); EDS creates the tables

### Configuration

Without configuration, the container starts with the in-memory H2 database.
For PostgreSQL, create `accessories/podman/eds.env` with the database
connection. The file is ignored by Git, as it holds the password. Comments
must be on their own lines, as the values are read up to the end of the line:

```bash
# PostgreSQL connection, the username defaults to eds_user
EDS_DB_URL=jdbc:postgresql://host.containers.internal:5432/eds
EDS_DB_USERNAME=eds_user
EDS_DB_PASSWORD=...

# Optional, enables TLS with the certificate and key from EDS_CERTS_DIR
EDS_CERTS_DIR=/path/to/certs
EDS_TLS_CERT=/app/certs/tls.crt
EDS_TLS_KEY=/app/certs/tls.key
# Plain HTTP requests: enabled | redirect | disabled
EDS_INSECURE_REQUESTS=enabled
```

The same variables can also be set in the environment of `eds.sh`, which then
take precedence over the file.

Inside the container, `localhost` is the container itself. For a PostgreSQL
running on the host, use `host.containers.internal`, which Podman maps to the
host. Connections to it don't reach services that only listen on the loopback
interface, so PostgreSQL must also listen on a host address
(`listen_addresses` in `postgresql.conf`) and accept the connection (a
matching entry in `pg_hba.conf`).

On systems with SELinux, like RHEL, the log and certificate mounts are
relabelled for the container.

### Control Script

The container is managed with `eds.sh` from the project root:

```bash
./eds.sh build            # Build EDS and the stone alone, and their container images
./eds.sh build stone      # Only the stone alone, and its container image
./eds.sh start            # Start the container, wait until ready
./eds.sh start stone      # The same with the stone alone
./eds.sh stop             # Stop the container
./eds.sh stop --remove    # Remove the container and both images
./eds.sh restart          # Stop and start, e.g. after a new build
./eds.sh status           # Build, health and version of the container
./eds.sh logs             # Follow the container logs
```

`build` takes `eds` and `stone` to build only those, and `--test`
to also run the tests. With the tests of `eds`, SonarQube analyses the code too, and
the build fails unless its quality gate passes, so anything new it finds is resolved
before a commit. The scanner takes the server and the token from `SONAR_HOST_URL`
and `SONAR_TOKEN`, and the proxy from `HTTPS_PROXY`; `--no-sonar` leaves it out.
`start` builds the container image first, if it is missing
or was not built from the present JAR.

### The Stone Alone

The stone alone is EDS built as the server alone, with a JAR and a container image
of its own (the Maven property `stone`):

| | JAR | Image | Serves |
|---|---|---|---|
| The stone alone | `eds-quarkus/target/eds-stone.jar` | `eds-stone:local` | the API below `/api/v1` |

```bash
./eds.sh build stone                       # The JAR and the image
./eds.sh start stone                       # Run it
mvn -Dstone -pl eds-quarkus package        # Or the JAR with Maven alone
```

It is run and set up as EDS is, and takes the same settings; it is what a host
runs (below).

The MasterKey (see MasterKey above) can be handled with it too:

```bash
./eds.sh masterkey generate     # Random secret in accessories/podman/masterkey
./eds.sh start --masterkey      # Start EDS with that file as its MasterKey source
./eds.sh masterkey load         # Or: send it to a running, locked EDS as admin
```

`masterkey generate` writes 48 random bytes, Base64-encoded, to the file (mode 600,
ignored by Git), and never replaces an existing file without `--force`, as data
protected by the old MasterKey can't be opened any more. Keep a safe copy of it.
`EDS_MASTERKEY_FILE` names another file. `start --masterkey` mounts the file
read-only into the container and sets `EDS_MASTERKEY_URL` to it. `masterkey load`
asks for the System Administrator's account name and passphrase (or takes
`EDS_ADMIN_ACCOUNT` and `EDS_ADMIN_PASSPHRASE`); on a new EDS, the name and the
passphrase given become those of the System Administrator (see below), and the
passphrase must meet the strength rules (e.g. 20 random characters). The secret and the
passphrase are sent through stdin, so they don't show in the process list.

### Health Checks

The health endpoints are served on the management port `9000`, which is only
exposed to the container network, not published to the host:

| Endpoint | Purpose |
|----------|---------|
| `/q/health/live` | Liveness, the application is running |
| `/q/health/ready` | Readiness, the database and EDS are ready |
| `/q/health/started` | Startup completed |
| `/q/health/group/sanity` | Outcome of the sanity checks of the stored data: DOWN while any Data Object is marked as failed (damaged), with the counts and the time of the last run. Not part of readiness, so damaged data doesn't take EDS out of service; alert on it. |

The container `HEALTHCHECK` uses the readiness endpoint, which is why the
image is built in the Docker format; the OCI format doesn't support it. For
Kubernetes, use the same endpoints for the probes, on port `9000`.

### Data Persistence

With PostgreSQL, all data is stored in the database; with H2, it is lost when
the container stops. The container only writes its logs to a local bind
mount, `accessories/podman/logs/` (`eds.log`). The `eds` user of the image
(UID 1001) is mapped to the host user, so the logs are owned by the host user.
`start` always creates a new container, from the current image and
configuration.

## Setting Up a Stone on a Host

This is the whole way from an empty host to a stone which runs, keeps its data
in PostgreSQL, is reached over HTTPS and starts with the host. It runs the image
of the stone alone, `eds-stone:local`, saved as the archive `eds-stone.tar`.

The host needs nothing of the EDS sources: one image and the script `stone`
(`accessories/podman/stone`), which sets up and controls the container. The
script runs as a normal user, with rootless Podman. It never asks for root and
never calls `sudo`, and refuses to run as root. Some steps change the system and
so need root; they are marked **[ROOT]** here, in `stone help <topic>` and in
`stone check`, and are done once, by whoever has the rights. Lines starting with
`#` are run as root, lines starting with `$` as the user of the stone. Once they
are done, everything is done as that user.

`stone help setup` tells the same steps on the host itself, and every step has a
topic with the details: `podman`, `images`, `settings`, `postgresql`, `masterkey`,
`tls`, `proxy`, `boot`, `instances`, `cors` and `cleanup`.

### 1. Where EDS is built: the image and the script

```bash
$ ./eds.sh build stone
$ podman save -o eds-stone.tar eds-stone:local
$ scp eds-stone.tar accessories/podman/stone user@host:
```

### 2. [ROOT] Podman, and a user for the stone

```bash
# apt install podman                                 # Debian, Ubuntu
# dnf install podman                                 # Fedora, RHEL, Rocky, Alma
# useradd --create-home stone                        # a user of its own is a good habit
```

A rootless container needs subordinate user and group ids, which most systems
give every new user. If `stone check` says they are missing:

```bash
# usermod --add-subuids 100000-165535 --add-subgids 100000-165535 stone
```

### 3. The script, in the PATH of that user

```bash
$ mkdir -p ~/.local/bin && install -m 755 stone ~/.local/bin/stone
$ stone                                              # the help
$ stone load eds-stone.tar
```

If `~/.local/bin` is not in the PATH, add it in `~/.profile`:
`export PATH="$HOME/.local/bin:$PATH"`.

### 4. [ROOT] The database

The stone needs an empty PostgreSQL database and a role which owns it, and
creates its tables itself; see [PostgreSQL Database Setup](#postgresql-database-setup).
Without a database it keeps everything in memory and loses it when it stops,
which is only for trying it out.

```bash
# apt install postgresql                             # Debian, Ubuntu
# dnf install postgresql-server && postgresql-setup --initdb && systemctl enable --now postgresql
# sudo -u postgres psql
postgres=# CREATE ROLE eds_user WITH LOGIN PASSWORD 'a long password';
postgres=# CREATE DATABASE eds WITH OWNER = eds_user;
```

For a server on the same host, the container must be able to reach it. In the
container, `localhost` is the container itself; the host is
`host.containers.internal`, and a connection to it doesn't reach a server which
only listens on the loopback address. So in `postgresql.conf` (`SHOW config_file;`)
and `pg_hba.conf` (`SHOW hba_file;`):

```
listen_addresses = 'localhost,<address of this host>'
host  eds  eds_user  <address of this host>/32  scram-sha-256
```

```bash
# systemctl restart postgresql
```

Keep the port 5432 closed to other hosts in the firewall. A database server which
already exists elsewhere only needs the role and the database.

### 5. The settings

```bash
$ stone init
```

This creates `~/.config/stone/stone.env`, readable only by that user, as it
holds the password of the database. One setting per line, without quotes:

```bash
STONE_BIND=127.0.0.1
STONE_PORT=8080
STONE_MASTERKEY=file
EDS_DB_URL=jdbc:postgresql://host.containers.internal:5432/eds
EDS_DB_USERNAME=eds_user
EDS_DB_PASSWORD=a long password
```

| Setting | Default | |
|---|---|---|
| `STONE_BIND` | `127.0.0.1` | The address of the host the stone listens on. `127.0.0.1` is only the host itself, which is right behind a reverse proxy; `0.0.0.0` is every address |
| `STONE_PORT` | `8080` | The port of the host for HTTP |
| `STONE_TLS_PORT` | `8443` | The port of the host for HTTPS by the stone itself, published when `EDS_TLS_CERT` is set |
| `STONE_MASTERKEY` | `file` | `file` starts the stone with `~/.config/stone/masterkey`; `none` doesn't |
| `STONE_IMAGE` | `eds-stone:local` | Another image, e.g. one from a registry |
| `STONE_NAME` | `stone` | The name of the container; `stone-<name>` for a named instance |
| `STONE_ADMIN_ACCOUNT` | (asked) | The account name of the System Administrator, for `masterkey load` |
| `EDS_...` | | The settings of the stone itself, see [Properties](#properties) |

A variable of the environment takes precedence over the file, for one call.
`STONE_HOME` names another directory than `~/.config/stone`; the logs are in
`~/.local/state/stone/logs` (`STONE_STATE`). The file is read, never executed.

### 6. The MasterKey

Everything a stone stores is protected by its MasterKey (see [MasterKey](#masterkey)),
and whoever loses it loses all that is stored. There are three ways to give it:

| Way | Settings | The stone starts |
|---|---|---|
| The MasterKey file on the host | `STONE_MASTERKEY=file`, after `stone masterkey generate` | by itself, also with the host |
| A source elsewhere, read at each start | `STONE_MASTERKEY=none`, `EDS_MASTERKEY_URL=https://...` | by itself, as long as the source is there: taking it away is the kill-switch |
| By hand | `STONE_MASTERKEY=none`, then `stone masterkey load` after each start | locked, until the System Administrator unlocks it |

```bash
$ stone masterkey generate
```

Keep a copy of the file in a safe place, apart from the backup of the database.

### 7. Check and start

```bash
$ stone check                                        # what is in place, what is missing, and who can do it
$ stone start
$ stone status
```

`check` goes through Podman, the image, the settings, the database, the MasterKey
and the start with the host, and tells for each thing which is missing whether it
is done by the user (`[USER]`) or needs root (`[ROOT]`), with the command.
`status` tells the database the stone uses; `H2, in memory` means it has none.

A new stone has no account, and sets no name for one (see
[The System Administrator](#the-system-administrator)): whoever comes to it first
becomes its System Administrator, under the account name and with the passphrase
given. So choose the name and be the first, right after the first start, before
the stone is reachable from outside: with `stone masterkey load`, which asks for
the name, or with the MasterKey request.

### 8. [ROOT] HTTPS, by a reverse proxy

The stone listens only on the host itself (`STONE_BIND=127.0.0.1`), and a
reverse proxy in front of it holds the ports 80 and 443 and the certificate.
Whichever proxy it is, it must:

- pass everything on to `http://127.0.0.1:8080`, with the `Host` header
- pass on WebSocket upgrades, for `/api/v1/socket` (the chat and calls)
- allow bodies up to the largest file of the stone (1000 MiB by default) and pass
  them on while they arrive, without storing them first
- allow a request to last as long as a large file takes
- never pass on the port 9000 (health), which the host doesn't publish

With Caddy, which gets and renews the certificate itself (`/etc/caddy/Caddyfile`):

```
stone.example.com {
    request_body {
        max_size 1100MB
    }
    reverse_proxy 127.0.0.1:8080 {
        flush_interval -1
    }
}
```

`stone help proxy` has the same for nginx, and the firewall. Without a proxy and
without root, the stone can serve HTTPS itself on a port from 1024 on, with a
certificate in `~/.config/stone/certs`; see `stone help tls`.

### 9. [ROOT, once] Start with the host

```bash
# loginctl enable-linger stone                       # the services of the user run without a login
$ stone enable                                       # a systemd service of the user starts the stone
```

`stone disable` takes the service away again; the stone keeps running.

`enable` writes a service of the user, `~/.config/systemd/user/stone.service`,
which needs no root. The services of a user only run while that user is logged
in, unless the user may linger, which root allows once. When the host starts
again:

1. systemd starts the services of every user who may linger, without a login.
2. The service waits for the network (where Podman has
   `podman-user-wait-network-online.service`), starts the container and is done.
   If the container can't be started yet, it tries again every ten seconds.
3. The stone itself waits for its database: one which isn't up yet is tried
   again until it answers, so the order of the two doesn't matter. PostgreSQL on
   the same host starts by itself once root has enabled it
   (`# systemctl enable postgresql`).
4. The stone reads its MasterKey from the file or another source, and is
   unlocked. With `STONE_MASTERKEY=none` and no `EDS_MASTERKEY_URL` it starts
   locked and waits for `stone masterkey load`: such a host needs a person after
   each start.

So a stone is back by itself after a start of the host if it has: linger for its
user, `stone enable`, a database which starts by itself, and a MasterKey source.
`stone check` tells which of them is missing. It can be tried without starting
the host again:

```bash
$ stone stop
$ systemctl --user start stone
$ stone status
```

### Several Stones on One Host

A host may run several stones, e.g. one for each group of people. Each is an
instance with a name, given before the command; without one it is the default
instance, which is all a host with one stone needs:

```bash
$ stone -i north init                                # proposes the next free port
$ stone -i south init
$ stone list                                         # the instances, and what each is doing
```

`STONE_INSTANCE=north` names it for a whole shell. A name is of small letters,
digits and `-`. An instance has, of its own:

| | The default instance | The instance `north` |
|---|---|---|
| Settings | `~/.config/stone/stone.env` | `~/.config/stone/north/stone.env` |
| MasterKey file | `~/.config/stone/masterkey` | `~/.config/stone/north/masterkey` |
| Logs | `~/.local/state/stone/logs` | `~/.local/state/stone/north/logs` |
| Container | `stone` | `stone-north` |
| Service | `stone.service` | `stone-north.service` |

Every step above from the settings on is done for each instance, with `-i`. What
each must be given, as nothing of it can be shared:

- **A port** of the host, `STONE_PORT` in its settings (and `STONE_TLS_PORT`, if
  it serves HTTPS itself). `init` proposes the next one no instance has, and
  `check` tells if two have the same.
- **A database**: another name in `EDS_DB_URL`, and best a role of its own. The
  server, `listen_addresses` and the restart are done once for the host; the role,
  the database and its line in `pg_hba.conf` for each stone:
  `CREATE ROLE north_user ...; CREATE DATABASE north WITH OWNER = north_user;`
- **A MasterKey**: `stone -i north masterkey generate`.
- **A name in the reverse proxy**: one site for each stone, passing on to its port.

They share Podman, the images (several instances run from the same image) and
the right to linger, which root gives once for the user.

Each instance is started with the host by its own service, so after a start of
the host all of them come back, and one which fails doesn't keep the others:

```bash
# loginctl enable-linger stone                       # once, for the user
$ stone -i north enable
$ stone -i south enable
$ stone list
```

Every command is for one instance. Only `list` and `load` are for all of them,
and `cleanup` keeps an image the container of another instance runs from.

### A New Version

```bash
$ stone load eds-stone.tar
$ stone restart
$ stone cleanup                                      # removes the image it replaced
```

### Cleaning Up

| Command | Removes |
|---|---|
| `stone cleanup` | EDS images which a newer one replaced. The stone keeps running |
| `stone cleanup --images` | The container, and all EDS images |
| `stone cleanup --all` | The same, and the logs and the start with the host |

Each lists what it removes and asks first, unless `--yes` is given. Only images of
EDS are touched, never other images or containers of the user. None of them
removes the settings or the MasterKey file, as what the database holds can never
be read again without the MasterKey: that file is only ever removed by hand. The
database is removed by whoever runs it (`# sudo -u postgres dropdb eds`).

## Verifying the Installation

After starting, verify EDS is running:

```bash
curl -X POST -H "Content-Type: application/json" -d '{}' http://localhost:8080/api/v1/version
```

The answer also tells what kind of system it is: `database` is the product
name of the database, and `persistent` is `false` on a test system, i.e. an
in-memory H2 database whose data is lost when the server stops. The web app
shows "Test system" in its status line, in the about dialog and on the
maintenance page.

## FitNesse Integration Testing

EDS includes FitNesse-based acceptance tests, which are both tests and documentation.
The fixtures in `eds-testing` use a Java client generated from the API contract
(openapi-generator, `java` with the `native` library), so the suites also check that
a generated client works against the server.

### Running the Suites with Maven

```bash
mvn -Pfitnesse install
```

The profile `fitnesse` builds everything, then starts the runner JAR
(`eds-quarkus/target/eds-rune.jar`) on the ports 18080 and 19000, runs the `EDS`
suite against it, and stops it. The results are in `eds-testing/target/`:
`fitnesse-results.txt`, and the log of EDS in `eds-server.log`; FitNesse keeps the
detailed results in `FitNesseRoot/files/testResults`. Without `EDS_DB_URL`, EDS uses
the in-memory H2 database; with it, the given PostgreSQL database, whose Circles,
Members and DataTypes the suite removes first.

When the runner JAR is already built, `mvn -Pfitnesse verify -pl eds-testing` is
enough. Options, as system properties: `eds.runner` (another runner JAR),
`eds.it.http.port` and `eds.it.management.port`, and `eds.it.suite` (e.g.
`EDS.API`).

### Running FitNesse Interactively

Download the FitNesse standalone JAR from https://fitnesse.org/ and place it in the
`eds-testing` directory (or create a symlink) as `fitnesse-standalone.jar`. Build the
fixtures, and start FitNesse from that directory:

```bash
mvn clean package -pl eds-testing -am -DskipTests
cd eds-testing
java -jar fitnesse-standalone.jar -p 2080
```

Or use `./eds.sh fitnesse start`. Access the test suite at http://localhost:2080/EDS.
Tests can be run as a complete suite or individually; each test is independent and
self-contained.

By default, the tests run against `http://localhost:8080/api/v1`, set in the setup page
of the suite. The environment variable `EDS_URL` replaces it, e.g.
`EDS_URL=http://localhost:18080/api/v1 java -jar fitnesse-standalone.jar -p 2080`.

### Shutdown FitNesse

```bash
curl http://localhost:2080/?shutdown
```

Or visit http://localhost:2080/?shutdown in a browser, or use `./eds.sh fitnesse stop`.

### Load and Rotation Tests

`eds-testing` also holds the load test (`./mvnw -Pload verify -pl eds-testing`,
optionally watching a PostgreSQL in a container) and the big-data test of the
rotation of a Circle Secret (`./mvnw -Protation verify -pl eds-testing`). Both start
the stone alone, `eds-quarkus/target/eds-stone.jar`, which is built with
`./mvnw -Dstone -pl eds-quarkus package`. With Podman on the PATH, they run it in a
rootless container built from that JAR (`accessories/podman/Containerfile`), which a
later run removes if a killed run left it. Their properties and reports are in
`eds-testing/README.txt`.

## API Documentation

The REST API is documented in:
- `eds-api/src/main/resources/openapi.json` - OpenAPI 3.0 specification, the API
  contract (the build checks that the code matches it; update it after an intended
  API change with `mvn install -Deds.contract.update=true`)
- `GET /api/v1/openapi.json` on a running EDS - the same file, served unchanged
  without authentication, e.g. to generate a client against a running server

The stone has a WebSocket, `ws://localhost:8080/api/v1/socket` (`wss://` with TLS),
for the features of the rooms, the chat among them: the frames are described in the
overview of the specification (section "Features and the socket") and by the schema
`SocketFrame`. A reverse proxy in front of EDS must pass WebSocket upgrades on that
path. What changes reaches the connections of the same EDS instance only.

The web app, Rune (the PWA), is served at `http://localhost:8080/`. The build makes it
with Quinoa, which downloads Node 24 and the npm packages into `eds-quarkus/.quinoa`
and `src/main/webui/node_modules`. Behind an HTTP proxy, npm uses `HTTPS_PROXY`, but
Quinoa's Node download doesn't. `eds.sh build` therefore puts the archive in Quinoa's
cache itself when `HTTPS_PROXY` is set; for a build with Maven alone, do it by hand,
e.g. `curl -o eds-quarkus/.quinoa/cache/node-v24.22.0-linux-x64.tar.gz
https://nodejs.org/dist/v24.22.0/node-v24.22.0-linux-x64.tar.gz`. The build container
(`eds.sh build --podman`) is Alpine and uses the musl build of Node
(`node-v24.22.0-linux-x64-musl.tar.gz`) and of the npm packages, so a build on the
host after one in the container, or the other way, installs both anew. For work on the app,
`mvn quarkus:dev -pl eds-quarkus` runs the Vite dev server behind EDS with hot reload.
Images and videos in the app use download tokens (`eds.download.token.lifetime.minutes`).

## Encrypting in the Client

EDS encrypts everything it stores, but it sees the content while it handles it. A
client can encrypt before it sends, so the stone never does (`E2E.md`). EDS takes no
part in that: it offers places for values it doesn't look into, and rules for who
writes and reads them.

| Value | Field | Written by | Read by |
|---|---|---|---|
| A Member's public key | `publicKey` of the account | the Member | everyone who sees the Member |
| A Member's vault | `vault` of the account | the Member | the Member only |
| A Circle's shared value | `circleKey` of the Circle | its administrators | its Trustees |
| A Member's secret in a Circle | `secret` of the Trustee | the Member, or an administrator of the Circle | the Member, and the administrators of the Circle |

A secret given with an invitation waits with it and becomes the invited Member's
when they accept. `POST /trustees/setSecrets` sets the secrets of several Trustees,
and the shared value, in one step: all or nothing.

Rune and Tolk follow one convention for these values (`E2E.md`). Any other client
may follow its own, in its own Circles. The walk-through below uses nothing but
`curl`, `jq` and OpenSSL: two people share a file which the stone can't read. It is
an example of the fields, not a recommendation for the cryptography (the file has
no integrity protection, for one). It was run against a stone as it stands here.

```sh
EDS=${EDS:-http://localhost:8080/api/v1}
b64() { printf '%s' "$1" | base64 -w0; }
call() { curl -s -X POST -H "Content-Type: application/json" -d "$2" "$EDS$1"; }

ADMIN=$(b64 'the stone keeper has a long passphrase, 2026')
ALICE=$(b64 'alice keeps her own counsel and passphrase, 2026')
BOB=$(b64 'bob has a rather different passphrase, 2026')

# Two accounts, made by the System Administrator.
A=$(call /members/createMember "{\"accountName\":\"admin\",\"credential\":\"$ADMIN\",\"newAccountName\":\"alice\",\"newCredential\":\"$ALICE\"}" | jq -r .memberId)
B=$(call /members/createMember "{\"accountName\":\"admin\",\"credential\":\"$ADMIN\",\"newAccountName\":\"bob\",\"newCredential\":\"$BOB\"}" | jq -r .memberId)

# Each makes a key pair and gives EDS the public half.
for who in alice bob; do
  openssl genpkey -quiet -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out $who.pem
  openssl pkey -in $who.pem -pubout -out $who.pub
done
call /members/updateMember "$(jq -n --arg c "$ALICE" --rawfile k alice.pub '{accountName:"alice",credential:$c,publicKey:$k}')" >/dev/null
call /members/updateMember "$(jq -n --arg c "$BOB" --rawfile k bob.pub '{accountName:"bob",credential:$c,publicKey:$k}')" >/dev/null

# Alice makes the Circle's key, wraps it for herself, and creates the Circle.
openssl rand -out circle.key 32
wrap() { openssl pkeyutl -encrypt -pubin -inkey "$1" -pkeyopt rsa_padding_mode:oaep -pkeyopt rsa_oaep_md:sha256 -in circle.key | base64 -w0; }
C=$(call /circles/createCircle "{\"accountName\":\"alice\",\"credential\":\"$ALICE\",\"circleName\":\"two friends\",\"circleKey\":\"openssl: rsa-oaep-sha256 wraps the key, aes-256-cbc the files\",\"secret\":\"$(wrap alice.pub)\"}" | jq -r .circleId)

# She takes Bob's public key from EDS, wraps the key for him, and adds him.
call /members/fetchMembers "{\"accountName\":\"alice\",\"credential\":\"$ALICE\",\"memberId\":\"$B\"}" | jq -r '.members[0].publicKey' > bob.fetched.pub
call /trustees/addTrustee "{\"accountName\":\"alice\",\"credential\":\"$ALICE\",\"circleId\":\"$C\",\"memberId\":\"$B\",\"trustLevel\":\"WRITE\",\"secret\":\"$(wrap bob.fetched.pub)\"}" >/dev/null

# She encrypts a file herself and stores what comes out.
echo 'We meet on the bench at noon.' > note.txt
openssl enc -aes-256-cbc -pbkdf2 -pass file:circle.key -in note.txt -out note.enc
D=$(call /data/addData "{\"accountName\":\"alice\",\"credential\":\"$ALICE\",\"circleId\":\"$C\",\"dataName\":\"note\",\"data\":\"$(base64 -w0 note.enc)\"}" | jq -r .dataId)

# Bob fetches his secret, unwraps the key with his private key, and reads.
call /trustees/fetchTrustees "{\"accountName\":\"bob\",\"credential\":\"$BOB\",\"circleId\":\"$C\",\"memberId\":\"$B\"}" | jq -r '.trustees[0].secret' | base64 -d \
  | openssl pkeyutl -decrypt -inkey bob.pem -pkeyopt rsa_padding_mode:oaep -pkeyopt rsa_oaep_md:sha256 -out bob.circle.key
call /data/fetchData "{\"accountName\":\"bob\",\"credential\":\"$BOB\",\"dataId\":\"$D\"}" | jq -r .data | base64 -d \
  | openssl enc -d -aes-256-cbc -pbkdf2 -pass file:bob.circle.key
```

It prints `We meet on the bench at noon.` What the stone stored for the file is
the output of `openssl enc`, under its own encryption; the key to it never left the
two machines unwrapped.

## The Status of a Stone

A stone tells those who look after it how it is doing (`STATUS.md`): its **load**,
how busy it is, and its **inventory**, what it holds. Only general numbers are
told, never a name.

```sh
# The System Administrator creates a status key; it is shown once.
SESSION=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"accountName":"admin","credential":"'"$(printf '%s' "$PASSPHRASE" | base64 -w0)"'"}' \
  http://localhost:8080/api/v1/members/login | jq -r .session)
KEY=$(curl -s -X POST -H "Authorization: Bearer $SESSION" -H "Content-Type: application/json" \
  -d '{"label":"overview at home"}' http://localhost:8080/api/v1/status/keys | jq -r .key)

# The key reads the status, and nothing else.
curl -s -H "Authorization: Bearer $KEY" "http://localhost:8080/api/v1/status?history=true" | jq .
```

| Setting | Default | |
|---|---|---|
| `eds.stone.name` | (none) | The name of the stone, which may be a technical one. Told with the status only. |
| `eds.status.load.interval.seconds` | 5 | Seconds between two samples of the load, 0 to 3600; 0 switches the gathering off. One second is affordable: a sample takes well below a millisecond. |
| `eds.status.inventory.interval.minutes` | 60 | Minutes between two samples of the inventory, which are queries, 1 to 10080. |
| `eds.cors.value` | `http://localhost` | The origins of pages which may call the stone from a browser, separated by commas; `*` for all. A Rune which shows several stones is named here on each of them. |

A status key also works while the stone is locked and waits for its MasterKey, and
stays valid when the MasterKey changes; `DELETE /api/v1/status/keys/{label}`
withdraws it. Everything gathered is kept in memory: the last 720 samples of the
load, the last 24 hours in steps of five minutes, and the last 720 samples of the
inventory. A restart starts them anew. The load of the database itself isn't told;
`databaseMillis`, the time a trivial query takes, rises with it. Stones on the same
database server tell the same `databaseMark`.
