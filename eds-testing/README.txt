This module holds the tests of EDS against a running stone: the combined
acceptance tests and documentation of EDS, written with FitNesse
(https://fitnesse.org/), and the fixtures they use; the load test; and the
rotation test. Its code is in the package net.haugr.eds.testing: the fixtures
in .fitnesse, the load and rotation tests in .load, the generated client in
.client.

The fixtures use a Java client generated from the API contract in eds-api
(openapi-generator, generator "java" with the "native" library), and nothing
else of EDS. The client is generated during the build, into
target/generated-sources/openapi.

Run the suites against a runner JAR started for them, from the project root:

$ mvn -Pfitnesse install

Or run FitNesse interactively, from this folder, after building the fixtures
(target/eds-testing.jar):

$ java -jar fitnesse-standalone.jar -p [ PORT ]

and open http://localhost:[ PORT ]/EDS. By default, the tests run against
http://localhost:8080/api/v1; the environment variable EDS_URL replaces it.

See RUNNING.md for details. To shut FitNesse down, open
http://localhost:[ PORT ]/?shutdown

Load test
---------

The load test (StoneLoadIT, package net.haugr.eds.testing.load in
src/test/java) starts a stone of its own, the stone without Rune
(eds-quarkus/target/eds-stone.jar), unlocked with a fresh MasterKey file
(target/load-masterkey), and puts it under load. From the project root,
after building the stone:

$ ./mvnw -Dstone -pl eds-quarkus package
$ ./mvnw -Pload verify -pl eds-testing

With Podman on the PATH, the stone runs in a container, eds-<name>-stone
(eds-load-stone here). Its image, eds-test-stone:local, is built from that
JAR with accessories/podman/Containerfile. The container uses the host's
network and the user's own ID (rootless), and writes the MasterKey, the log
and the recording to target/. A container left over by a run which was
killed (kill -9) is removed by name when the next run starts. When the
test's JVM is stopped (Ctrl-C, SIGTERM), a shutdown hook stops the
container. Without Podman, or with -Deds.stone.container=false, the stone
runs as java -jar. The rotation test starts its stone the same way.

Many users work at once, each with its own account, connections and socket,
in rooms they share, doing what Rune and Tolk do: chat messages, edits,
reactions, receipts, history and typing over the socket and the REST API;
plans, calendar, locations and call signals; files up and down (inline,
streamed, ranges, download tokens, thumbnails, new content with If-Match,
rename, copy, move, delete, folders); reading rooms, members, trustees and
features; avatars, logging out and in, a new passphrase. Meanwhile the System
Administrator makes and removes users and rooms, gives, lowers and takes
access, invites, has the Circle Secret of a shared room rotated (one at
once, then one each eds.load.rotate.every), and works with Data Types,
features, status keys, the settings and the sanity check. As Rune and Tolk
do, a user counts the messages after the position its last receipt read up
to, not all of a room's.
Not covered: the MasterKey.

Afterwards it checks what the stone holds: every file a user kept is
downloaded and compared byte for byte with what was uploaded, each room must
list exactly the files the users left in it and count exactly their chat
messages, and the stone must still be alive, ready and without ERROR in its
log. During the load it checks that a reader can't write, a removed member
can't read, a deleted account can't log in, and that a new passphrase ends
the Session.

The run passes when no request failed. Expected refusals (those the test
provokes, and reactions to messages someone else just deleted) are counted
apart. The report, with the latency of every operation, the time a chat
message and a signal take to reach the others, and the stone's own load
samples, is written to target/load-report.txt; the stone's log to
target/load-stone.log. The same report as a page, target/load-report.html,
shows the run over time: requests per second and their latency (median and
90th percentile) for REST, the socket and the delivery to the others, and
from the stone's own samples its CPU, the load of the host, its heap, its
threads and its socket connections. It needs nothing but a browser.

With -Deds.load.jfr=true the stone keeps a Flight Recording of the run,
target/load-stone.jfr, for JDK Mission Control (or IntelliJ): every thread,
also the virtual ones, CPU per thread, heap and GC, locks and socket I/O.
It costs a little CPU, and some 15 MB a minute with 16 users. It keeps
up to 8 GB, the whole of a 4-hour run with 160 users (some 4 GB); JFR's
default of 250 MB kept only the last 15 minutes of one.

The size of a run is set with system properties (see LoadSettings):

  eds.load.users          users working at once (16)
  eds.load.rooms          rooms they share (6); each user is in up to three
  eds.load.duration       how long, ISO-8601 (PT2M)
  eds.load.rampup         over which the users start (PT10S)
  eds.load.think.millis   longest pause between two actions, 0 for none (20)
  eds.load.content.max    largest common upload (1 MiB); one in 50 is 4-6 MiB
  eds.load.held.max       content a user keeps before deleting (24 MiB)
  eds.load.rotate.every   a room's Circle Secret rotated each period (PT20M);
                          a stone rotates each Circle once a year by default
  eds.load.churn          the administrator's changes meanwhile (true)
  eds.load.cleanup        delete the test's users and rooms at the end (true)
  eds.load.seed           repeat a run's choices
  eds.load.max.failures   failures a run may have and still pass (0)
  eds.load.jfr            a Flight Recording of the stone (false)
  eds.load.pg.container   watch the PostgreSQL in this container (below)
  eds.load.jvm            options for the stone's JVM, e.g. -Xmx1g
  eds.load.http.port, eds.load.management.port   (18090, 19090)
  eds.runner              the stone's JAR (../eds-quarkus/target/eds-stone.jar)

For example, 50 users without pauses for ten minutes:

$ ./mvnw -Pload verify -pl eds-testing -Deds.load.users=50 \
    -Deds.load.think.millis=0 -Deds.load.duration=PT10M

With EDS_DB_URL (and EDS_DB_USERNAME, EDS_DB_PASSWORD) in the environment, the
stone runs on that PostgreSQL database instead of the in-memory H2; it must be
empty, as the test claims the stone as its first account (a stone which
finds an earlier run's MasterKey stays locked). With
-Deds.load.url=http://host:port/api/v1 the test loads a stone which is already
running instead of starting one: an empty test stone, or one whose System
Administrator is given with -Deds.load.admin and -Deds.load.passphrase. It
makes users and rooms of its own and deletes them at the end, but don't point
it at a stone people use.

When the test starts the stone (not with eds.load.url) and EDS_DB_URL names a
PostgreSQL on localhost in a container, -Deds.load.pg.container=<name> also
watches the database during the run (PostgresObserver), to tell a slowdown
apart: running, waiting for a lock, or reading from disk. Before the stone
starts it checks that the container is running, that it publishes the port of
EDS_DB_URL on this host, that the server behind EDS_DB_URL is the one in the
container (the time it started, read both ways), and that eds.load.pg.user is
a superuser there; otherwise the test stops before the load. Everything it does
goes through "podman exec" (or "docker exec", see eds.load.pg.command) into
that container, so a database outside a container is never touched. It sets log_lock_waits (with a
deadlock_timeout of 200 ms) and track_io_timing on the database (ALTER
DATABASE), resets its statistics (pg_stat_reset), and writes:

  target/load-pg-activity.txt    every second, the connections: state, wait
                                 event, who blocks them, transaction age,
                                 statement
  target/load-pg-lockwaits.log   the lock waits over 200 ms and deadlocks,
                                 from the container's log
  target/load-pg-snapshots.txt   pg_stat_user_tables, pg_statio_user_tables,
                                 pg_stat_user_indexes and pg_stat_database at
                                 the start, every eds.load.pg.snapshot.minutes
                                 and at the end
  target/load-pg-summary.txt     the connections by hour, the lock waits and
                                 their most frequent statements, and the
                                 changes between snapshots; also in the report

At the end the settings are set back to what they were, also when the test
fails or the JVM is stopped (a shutdown hook). Only a JVM which is killed
(kill -9) leaves them; then, before the next run (docker alike):

$ podman exec <container> psql -U postgres -c "ALTER DATABASE <database> RESET log_lock_waits" \
    -c "ALTER DATABASE <database> RESET deadlock_timeout" -c "ALTER DATABASE <database> RESET track_io_timing"

More properties: eds.load.pg.user (the superuser, postgres),
eds.load.pg.command (podman if it is on the PATH, as it runs without root,
else docker) and eds.load.pg.snapshot.minutes (60, at least 1). For example:

$ podman run -d --name eds-load-pg -p 127.0.0.1:55432:5432 \
    -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=<secret> docker.io/library/postgres:18
$ podman exec eds-load-pg psql -U postgres -c "CREATE ROLE eds_load LOGIN PASSWORD '<secret>'" \
    -c "CREATE DATABASE eds_load OWNER eds_load"
$ EDS_DB_URL=jdbc:postgresql://127.0.0.1:55432/eds_load EDS_DB_USERNAME=eds_load \
    EDS_DB_PASSWORD=<secret> ./mvnw -Pload verify -pl eds-testing \
    -Deds.load.pg.container=eds-load-pg

Rotation test
-------------

The big-data test of the rotation of a Circle Secret (PLAN.md, Crypto C6),
CircleRotationIT in the same package, starts the stone without Rune on a
PostgreSQL database of its own: EDS_DB_URL (with EDS_DB_USERNAME and
EDS_DB_PASSWORD) must name an empty database, which the test claims. From the
project root, after building eds-quarkus with -Dstone:

$ ./mvnw -Protation verify -pl eds-testing -Deds.rotation.size=25GiB

It fills a Circle with the given amount of content, in objects of random
sizes generated from a seed, so that nothing is kept by the test: each object
can be generated again to compare. Then a Trustee is removed, the owner's
next download hands the Circle Secret to the rotation, and the stone
re-encrypts every object in the background, while another user reads ranges,
uploads and deletes in the Circle. Afterwards every object is downloaded and
compared byte for byte, the database must hold no object and no Trustee row on
an old secret and one secret for the Circle, the removed Member can't read,
and the stone must be alive without ERROR in its log. The report, with the
time and speed of the upload and of the rotation and the stone's heap and CPU,
is target/rotation-report.txt; the stone's log target/rotation-stone.log.

  eds.rotation.size        amount of content, e.g. 512MiB, 25GiB (2GiB)
  eds.rotation.object.max  largest object, at most 1000 MiB (256MiB)
  eds.rotation.object.min  smallest object (1KiB)
  eds.rotation.threads     objects re-encrypted at a time (2)
  eds.rotation.uploads     uploads at a time while the Circle is filled (4)
  eds.rotation.concurrent  a user at work during the rotation (true)
  eds.rotation.seed        repeat a run's sizes and content
  eds.rotation.jvm         options for the stone's JVM, e.g. -Xmx512m
  eds.rotation.http.port, eds.rotation.management.port   (18092, 19092)

The heap of the stone may be far below the amount of content: memory stays
bounded by the parts content is stored in.
