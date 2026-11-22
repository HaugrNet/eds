This module contains the combined acceptance tests and documentation of EDS,
written with FitNesse (https://fitnesse.org/), and the fixtures they use.

The fixtures use a Java client generated from the API contract in eds-api
(openapi-generator, generator "java" with the "native" library), and nothing
else of EDS. The client is generated during the build, into
target/generated-sources/openapi.

Run the suites against a runner JAR started for them, from the project root:

$ mvn -Pfitnesse install

Or run FitNesse interactively, from this folder, after building the fixtures
(target/eds-fitnesse.jar):

$ java -jar fitnesse-standalone.jar -p [ PORT ]

and open http://localhost:[ PORT ]/EDS. By default, the tests run against
http://localhost:8080/api/v1; the environment variable EDS_URL replaces it.

See RUNNING.md for details. To shut FitNesse down, open
http://localhost:[ PORT ]/?shutdown

Load test
---------

The load test (package net.haugr.eds.load in src/test/java) starts a stone of
its own, the stone without Rune (eds-quarkus/target/eds-stone.jar), unlocked
with a fresh MasterKey file, and puts it under load. From the project root,
after building eds-quarkus:

$ ./mvnw -Pload verify -pl eds-fitnesse

Many users work at once, each with its own account, connections and socket,
in rooms they share, doing what Rune and Tolk do: chat messages, edits,
reactions, receipts, history and typing over the socket and the REST API;
plans, calendar, locations and call signals; files up and down (inline,
streamed, ranges, download tokens, thumbnails, new content with If-Match,
rename, copy, move, delete, folders); reading rooms, members, trustees and
features; avatars, logging out and in, a new passphrase. Meanwhile the System
Administrator makes and removes users and rooms, gives, lowers and takes
access, invites, has the Circle Secret of a shared room rotated, and works
with Data Types, features, status keys, the settings and the sanity check.
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
It costs a little CPU, and some 15 MB a minute with 16 users.

The size of a run is set with system properties (see LoadSettings):

  eds.load.users          users working at once (16)
  eds.load.rooms          rooms they share (6); each user is in up to three
  eds.load.duration       how long, ISO-8601 (PT2M)
  eds.load.rampup         over which the users start (PT10S)
  eds.load.think.millis   longest pause between two actions, 0 for none (20)
  eds.load.content.max    largest common upload (1 MiB); one in 50 is 4-6 MiB
  eds.load.held.max       content a user keeps before deleting (24 MiB)
  eds.load.churn          the administrator's changes meanwhile (true)
  eds.load.cleanup        delete the test's users and rooms at the end (true)
  eds.load.seed           repeat a run's choices
  eds.load.max.failures   failures a run may have and still pass (0)
  eds.load.jfr            a Flight Recording of the stone (false)
  eds.load.jvm            options for the stone's JVM, e.g. -Xmx1g
  eds.load.http.port, eds.load.management.port   (18090, 19090)

For example, 50 users without pauses for ten minutes:

$ ./mvnw -Pload verify -pl eds-fitnesse -Deds.load.users=50 \
    -Deds.load.think.millis=0 -Deds.load.duration=PT10M

With EDS_DB_URL (and EDS_DB_USERNAME, EDS_DB_PASSWORD) in the environment, the
stone runs on that PostgreSQL database instead of the in-memory H2; it must be
empty, as the test claims the stone as its first account. With
-Deds.load.url=http://host:port/api/v1 the test loads a stone which is already
running instead of starting one: an empty test stone, or one whose System
Administrator is given with -Deds.load.admin and -Deds.load.passphrase. It
makes users and rooms of its own and deletes them at the end, but don't point
it at a stone people use.

Rotation test
-------------

The big-data test of the rotation of a Circle Secret (PLAN.md, Crypto C6),
CircleRotationIT in the same package, starts the stone without Rune on a
PostgreSQL database of its own: EDS_DB_URL (with EDS_DB_USERNAME and
EDS_DB_PASSWORD) must name an empty database, which the test claims. From the
project root, after building eds-quarkus with -Dstone:

$ ./mvnw -Protation verify -pl eds-fitnesse -Deds.rotation.size=25GiB

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
