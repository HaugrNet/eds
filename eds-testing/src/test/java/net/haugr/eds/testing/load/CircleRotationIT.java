/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.testing.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * <p>The big-data test of the rotation of a Circle Secret (PLAN.md, Crypto
 * C6), with {@code mvn -Protation verify} in eds-testing (build eds-quarkus
 * first). It needs PostgreSQL: {@code EDS_DB_URL} (with
 * {@code EDS_DB_USERNAME} and {@code EDS_DB_PASSWORD}) names an empty
 * database, which the stone of the test claims.</p>
 *
 * <p>A Circle is filled with {@code eds.rotation.size} of content, in
 * objects of random sizes, generated from a seed so that nothing is kept
 * here: each object can be generated again to compare. Then a Trustee is
 * removed, and another Trustee's download hands the Circle Secret to the
 * rotation, which runs in the background of the stone; meanwhile (unless
 * {@code eds.rotation.concurrent} is false) a user reads ranges, uploads
 * and deletes in the Circle. Afterwards every object is downloaded and
 * compared, the database must have no object and no Trustee row left on an
 * old secret and one secret for the Circle, the removed Member can't read,
 * and the stone is alive with no ERROR in its log. The figures are written
 * to {@code target/rotation-report.txt}.</p>
 *
 * <table>
 *   <caption>The properties</caption>
 *   <tr><td>eds.rotation.size</td><td>Amount of content, e.g. 512MiB, 25GiB (2GiB)</td></tr>
 *   <tr><td>eds.rotation.object.max</td><td>Largest object, at most the stone's maximum (256MiB)</td></tr>
 *   <tr><td>eds.rotation.object.min</td><td>Smallest object (1KiB)</td></tr>
 *   <tr><td>eds.rotation.threads</td><td>eds.circle.secret.rotation.threads of the stone (2)</td></tr>
 *   <tr><td>eds.rotation.uploads</td><td>Uploads at a time while the Circle is filled (4)</td></tr>
 *   <tr><td>eds.rotation.concurrent</td><td>A user at work during the rotation (true)</td></tr>
 *   <tr><td>eds.rotation.seed</td><td>Repeat a run's sizes and content (random)</td></tr>
 *   <tr><td>eds.rotation.jvm</td><td>Options for the stone's JVM, e.g. -Xmx512m</td></tr>
 * </table>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class CircleRotationIT {

    private static final Path TARGET = Path.of("target");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long MAX_OBJECT = 1000L * 1024 * 1024;
    private static final Duration ROTATION_TIMEOUT = Duration.ofHours(24);
    private static final String CONTENT = "/content";

    private final List<String> report = Collections.synchronizedList(new ArrayList<>());
    private final List<String> problems = Collections.synchronizedList(new ArrayList<>());
    private HttpClient client = null;
    private String api = null;

    /** An object of the Circle: its content is generated from the seed. */
    private record Stored(String dataId, long seed, long size) {
    }

    @Test
    void testRotationOfABigCircle() throws Exception {
        final Settings settings = Settings.fromSystemProperties();
        final String database = System.getenv("EDS_DB_URL");
        if ((database == null) || database.isBlank()) {
            throw new IllegalStateException("The rotation test needs PostgreSQL: set EDS_DB_URL (and EDS_DB_USERNAME, EDS_DB_PASSWORD) to an empty database.");
        }
        report.add("Rotation of a Circle Secret, " + settings);

        try (TestStone stone = TestStone.start(TARGET, "rotation", 18092, 19092);
             HttpClient http = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).connectTimeout(Duration.ofSeconds(10)).build()) {
            client = http;
            api = stone.url();
            run(settings, database);
            assertTrue(stone.alive(), "The stone stopped.");
            final List<String> errors = stone.errors();
            errors.forEach(line -> problems.add("ERROR in the log of the stone: " + line));
        } finally {
            report.add("");
            report.add(problems.isEmpty() ? "Passed." : "Failed:");
            problems.forEach(problem -> report.add("  " + problem));
            Files.write(TARGET.resolve("rotation-report.txt"), report, StandardCharsets.UTF_8);
        }
        assertEquals(List.of(), problems, "See target/rotation-report.txt");
    }

    private void run(final Settings settings, final String database) throws Exception {
        final SecureRandom random = new SecureRandom();
        final String passphrase = passphrase(random);
        final String admin = login("rotation-admin", passphrase);
        setting(admin, "eds.circle.secret.rotation.threads", String.valueOf(settings.threads()));
        final String ownerId = createMember(admin, "rotation-owner", passphrase);
        final String leaverId = createMember(admin, "rotation-leaver", passphrase);
        final String workerId = createMember(admin, "rotation-worker", passphrase);
        final String owner = login("rotation-owner", passphrase);
        final String circleId = post(owner, "/circles", object().put("circleName", "Rotation"), "circleId");
        addTrustee(owner, circleId, leaverId, "READ");
        addTrustee(owner, circleId, workerId, "WRITE");
        report.add("Members " + ownerId + " (owner), " + leaverId + " (leaves), " + workerId + " (works meanwhile)");

        // The Circle is filled.
        final List<Stored> stored = fill(settings, owner, circleId);
        final long total = stored.stream().mapToLong(Stored::size).sum();

        // A Trustee leaves; the owner's next download hands the secret over.
        final JsonNode before = circle(owner, circleId);
        expect(request(owner, "DELETE", "/circles/" + circleId + "/trustees/" + leaverId, null), "removeTrustee");
        final long started = System.nanoTime();
        download(owner, stored.getFirst(), 0, 0);

        final AtomicBoolean rotating = new AtomicBoolean(true);
        final Worker worker = settings.concurrent() ? new Worker(login("rotation-worker", passphrase), circleId, stored, settings.seed()) : null;
        final Future<?> working;
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            working = (worker != null) ? pool.submit(() -> worker.work(rotating)) : null;
            waitForTheRotation(admin, owner, circleId, before.path("secretRotated").asText());
            rotating.set(false);
            if (working != null) {
                working.get();
            }
        }
        final double seconds = (System.nanoTime() - started) / 1e9;
        report.add(String.format(Locale.ROOT, "Rotation: %d objects, %s in %.1f s: %.1f MB/s, %.1f objects/s",
                stored.size(), bytes(total), seconds, total / seconds / 1e6, stored.size() / seconds));
        if (worker != null) {
            report.add("Meanwhile: " + worker.reads.get() + " ranges read, " + worker.uploads.get() + " uploads, " + worker.deletes.get() + " deletes");
        }

        // Everything as it was, and the old secret gone.
        verifyAll(owner, stored, worker);
        checkDatabase(database, circleId);
        final String leaver = login("rotation-leaver", passphrase);
        final HttpResponse<byte[]> refused = request(leaver, "GET", "/data/" + stored.getFirst().dataId() + CONTENT, null);
        if (refused.statusCode() == 200) {
            problems.add("The Member who left could still read the Circle's content.");
        }
    }

    // =========================================================================
    // Filling the Circle
    // =========================================================================

    private List<Stored> fill(final Settings settings, final String owner, final String circleId) throws Exception {
        final SplittableRandom sizes = new SplittableRandom(settings.seed());
        final List<Long> plan = new ArrayList<>();
        long planned = 0;
        while (planned < settings.size()) {
            final long size = Math.min(settings.size() - planned, settings.objectMin() + (long) (sizes.nextDouble() * sizes.nextDouble() * (settings.objectMax() - settings.objectMin())));
            plan.add(Math.max(1, size));
            planned += Math.max(1, size);
        }

        final long all = planned;
        final long started = System.nanoTime();
        final List<Stored> stored = Collections.synchronizedList(new ArrayList<>());
        final AtomicLong done = new AtomicLong();
        try (ExecutorService pool = Executors.newFixedThreadPool(settings.uploads(), Thread.ofVirtual().factory())) {
            final List<Future<?>> uploads = new ArrayList<>();
            for (int i = 0; i < plan.size(); i++) {
                final long seed = settings.seed() * 1_000_003L + i;
                final long size = plan.get(i);
                final String name = "object-" + i;
                uploads.add(pool.submit(() -> {
                    final String dataId = post(owner, "/data", object().put("circleId", circleId).put("dataName", name), "dataId");
                    upload(owner, dataId, seed, size);
                    stored.add(new Stored(dataId, seed, size));
                    final long now = done.addAndGet(size);
                    if ((now * 10 / all) != ((now - size) * 10 / all)) {
                        log("Filled " + bytes(now) + " of " + bytes(all));
                    }
                    return null;
                }));
            }
            for (final Future<?> upload : uploads) {
                upload.get();
            }
        }
        final double seconds = (System.nanoTime() - started) / 1e9;
        report.add(String.format(Locale.ROOT, "Upload: %d objects, %s in %.1f s: %.1f MB/s", plan.size(), bytes(planned), seconds, planned / seconds / 1e6));
        stored.sort((a, b) -> Long.compare(a.seed(), b.seed()));
        return stored;
    }

    private void upload(final String session, final String dataId, final long seed, final long size) {
        final HttpRequest put = HttpRequest.newBuilder(URI.create(api + "/data/" + dataId + CONTENT))
                .header("Authorization", "Bearer " + session)
                .header("Content-Type", "application/octet-stream")
                .timeout(Duration.ofHours(1))
                .PUT(HttpRequest.BodyPublishers.fromPublisher(HttpRequest.BodyPublishers.ofInputStream(() -> new Generated(seed, size)), size))
                .build();
        expect(send(put), "upload");
    }

    // =========================================================================
    // The rotation, and what is checked afterwards
    // =========================================================================

    // The rotation runs in the background of the stone, so there is nothing to wait on but the time:
    // the Circle is read every two seconds until the rotation has ended.
    @SuppressWarnings("java:S2925")
    private void waitForTheRotation(final String admin, final String owner, final String circleId, final String madeBefore) throws InterruptedException {
        final long deadline = System.nanoTime() + ROTATION_TIMEOUT.toNanos();
        double heap = 0;
        double cpu = 0;
        long lastDone = -1;
        while (System.nanoTime() < deadline) {
            final JsonNode circle = circle(owner, circleId);
            final JsonNode rotation = circle.path("rotation");
            if (rotation.isMissingNode() || rotation.isNull()) {
                if (!madeBefore.equals(circle.path("secretRotated").asText())) {
                    report.add(String.format(Locale.ROOT, "Stone during the rotation: heap up to %s, CPU of the process up to %.0f %%", bytes((long) heap), cpu * 100));
                    return;
                }
            } else if (rotation.path("objectsDone").asLong() != lastDone) {
                lastDone = rotation.path("objectsDone").asLong();
                log("Rotation: " + lastDone + " of " + rotation.path("objects").asLong() + " objects");
            }
            final JsonNode load = json(request(admin, "GET", "/status", null)).path("load").path("samples");
            if (load.isArray() && !load.isEmpty()) {
                final JsonNode values = load.get(load.size() - 1).path("values");
                heap = Math.max(heap, values.path("memoryUsed").asDouble());
                cpu = Math.max(cpu, values.path("processShare").asDouble());
            }
            Thread.sleep(2000);
        }
        problems.add("The rotation didn't end within " + ROTATION_TIMEOUT + '.');
    }

    private void verifyAll(final String owner, final List<Stored> stored, final Worker worker) throws Exception {
        final List<Stored> all = new ArrayList<>(stored);
        if (worker != null) {
            all.removeAll(worker.deleted);
            all.addAll(worker.uploaded);
        }
        final long started = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(4, Thread.ofVirtual().factory())) {
            final List<Future<?>> checks = new ArrayList<>();
            for (final Stored object : all) {
                checks.add(pool.submit(() -> {
                    if (!MessageDigest.isEqual(digest(object), downloadDigest(owner, object))) {
                        problems.add("The content of " + object.dataId() + " differs from what was uploaded.");
                    }
                    return null;
                }));
            }
            for (final Future<?> check : checks) {
                check.get();
            }
        }
        report.add(String.format(Locale.ROOT, "Verified: %d objects, byte for byte, in %.1f s", all.size(), (System.nanoTime() - started) / 1e9));
    }

    // The URL, user and password are those the developer gives the test.
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JDBC")
    private void checkDatabase(final String url, final String circleId) throws SQLException {
        final String jdbc = url.startsWith("jdbc:") ? url : "jdbc:" + url;
        try (Connection connection = DriverManager.getConnection(jdbc, System.getenv("EDS_DB_USERNAME"), System.getenv("EDS_DB_PASSWORD"))) {
            final long keys = count(connection, "SELECT count(*) FROM eds_keys k JOIN eds_circles c ON c.id = k.circle_id WHERE c.external_id = ?", circleId);
            final long active = count(connection, "SELECT count(*) FROM eds_keys k JOIN eds_circles c ON c.id = k.circle_id WHERE c.external_id = ? AND k.status = 'ACTIVE'", circleId);
            final long oldData = count(connection, "SELECT count(*) FROM eds_data d JOIN eds_metadata m ON m.id = d.metadata_id JOIN eds_circles c ON c.id = m.circle_id"
                    + " JOIN eds_keys k ON k.id = d.key_id WHERE c.external_id = ? AND k.status <> 'ACTIVE'", circleId);
            final long previous = count(connection, "SELECT count(*) FROM eds_trustees t JOIN eds_circles c ON c.id = t.circle_id WHERE c.external_id = ? AND t.previous_key_id IS NOT NULL", circleId);
            report.add("Database: " + keys + " Circle Secret(s) of the Circle, " + oldData + " objects and " + previous + " Trustee rows on an old one");
            if ((keys != 1) || (active != 1) || (oldData != 0) || (previous != 0)) {
                problems.add("The database still has an old Circle Secret, or something on it.");
            }
        }
    }

    private static long count(final Connection connection, final String sql, final String circleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, circleId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : -1;
            }
        }
    }

    /** A user at work in the Circle while it rotates: ranges read and checked, uploads, deletes of their own. */
    private final class Worker {

        private final String session;
        private final String circleId;
        private final List<Stored> stored;
        private final SplittableRandom random;
        private final List<Stored> uploaded = Collections.synchronizedList(new ArrayList<>());
        private final List<Stored> deleted = Collections.synchronizedList(new ArrayList<>());
        private final AtomicLong reads = new AtomicLong();
        private final AtomicLong uploads = new AtomicLong();
        private final AtomicLong deletes = new AtomicLong();

        private Worker(final String session, final String circleId, final List<Stored> stored, final long seed) {
            this.session = session;
            this.circleId = circleId;
            this.stored = stored;
            this.random = new SplittableRandom(seed + 1);
        }

        private void work(final AtomicBoolean rotating) {
            int round = 0;
            while (rotating.get()) {
                final Stored object = stored.get(random.nextInt(stored.size()));
                if (!deleted.contains(object)) {
                    final long from = random.nextLong(object.size());
                    final long to = Math.min(object.size() - 1, from + random.nextInt(200_000));
                    final byte[] range = download(session, object, from, to);
                    reads.incrementAndGet();
                    if (!java.util.Arrays.equals(range, generated(object.seed(), object.size(), from, to))) {
                        problems.add("A range of " + object.dataId() + " read during the rotation differs.");
                    }
                }
                if ((round % 10) == 0) {
                    final long seed = 7_000_000_000L + round;
                    final long size = 1 + random.nextInt(300_000);
                    final String dataId = post(session, "/data", object().put("circleId", circleId).put("dataName", "worker-" + round), "dataId");
                    upload(session, dataId, seed, size);
                    uploaded.add(new Stored(dataId, seed, size));
                    uploads.incrementAndGet();
                }
                if (((round % 25) == 0) && (uploaded.size() > 1)) {
                    final Stored own = uploaded.removeFirst();
                    expect(request(session, "DELETE", "/data/" + own.dataId(), null), "delete");
                    deletes.incrementAndGet();
                }
                round++;
            }
        }
    }

    // =========================================================================
    // The content, generated from a seed
    // =========================================================================

    /** The content of an object, generated from its seed, 64 KiB at a time. */
    private static final class Generated extends InputStream {

        private static final int BLOCK = 65536;
        private final SplittableRandom random;
        private final byte[] block = new byte[BLOCK];
        private long left;
        private int position = BLOCK;

        private Generated(final long seed, final long size) {
            this.random = new SplittableRandom(seed);
            this.left = size;
        }

        @Override
        public int read() {
            final byte[] one = new byte[1];
            return (read(one, 0, 1) < 0) ? -1 : (one[0] & 0xFF);
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) {
            if (left <= 0) {
                return -1;
            }
            if (position == BLOCK) {
                random.nextBytes(block);
                position = 0;
            }
            final int count = (int) Math.min(Math.min(length, BLOCK - position), left);
            System.arraycopy(block, position, bytes, offset, count);
            position += count;
            left -= count;
            return count;
        }
    }

    private static byte[] generated(final long seed, final long size, final long from, final long to) {
        try (InputStream stream = new Generated(seed, size)) {
            stream.skipNBytes(from);
            return stream.readNBytes((int) (to - from + 1));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] digest(final Stored object) throws IOException {
        final MessageDigest digest = sha256();
        try (InputStream stream = new Generated(object.seed(), object.size())) {
            final byte[] buffer = new byte[65536];
            int read = stream.read(buffer, 0, buffer.length);
            while (read >= 0) {
                digest.update(buffer, 0, read);
                read = stream.read(buffer, 0, buffer.length);
            }
        }
        return digest.digest();
    }

    private byte[] downloadDigest(final String session, final Stored object) throws IOException, InterruptedException {
        final HttpRequest get = HttpRequest.newBuilder(URI.create(api + "/data/" + object.dataId() + CONTENT))
                .header("Authorization", "Bearer " + session).timeout(Duration.ofHours(1)).GET().build();
        final HttpResponse<InputStream> response = client.send(get, HttpResponse.BodyHandlers.ofInputStream());
        final MessageDigest digest = sha256();
        try (InputStream body = response.body()) {
            if (response.statusCode() != 200) {
                problems.add("The download of " + object.dataId() + " failed with " + response.statusCode() + '.');
                return new byte[0];
            }
            final byte[] buffer = new byte[65536];
            int read = body.read(buffer);
            while (read >= 0) {
                digest.update(buffer, 0, read);
                read = body.read(buffer);
            }
        }
        return digest.digest();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // =========================================================================
    // Requests
    // =========================================================================

    private byte[] download(final String session, final Stored object, final long from, final long to) {
        final HttpRequest get = HttpRequest.newBuilder(URI.create(api + "/data/" + object.dataId() + CONTENT))
                .header("Authorization", "Bearer " + session).header("Range", "bytes=" + from + '-' + to)
                .timeout(Duration.ofMinutes(5)).GET().build();
        final HttpResponse<byte[]> response = send(get);
        if (response.statusCode() != 206) {
            problems.add("A range of " + object.dataId() + " was answered with " + response.statusCode() + '.');
        }
        return response.body();
    }

    private String login(final String account, final String passphrase) {
        final ObjectNode body = object().put("accountName", account)
                .put("credential", Base64.getEncoder().encodeToString(passphrase.getBytes(StandardCharsets.UTF_8)));
        final JsonNode reply = json(expect(request(null, "POST", "/members/login", body), "login"));
        return reply.path("session").asText();
    }

    private String createMember(final String admin, final String name, final String passphrase) {
        return post(admin, "/members", object().put("newAccountName", name)
                .put("newCredential", Base64.getEncoder().encodeToString(passphrase.getBytes(StandardCharsets.UTF_8))), "memberId");
    }

    private void addTrustee(final String session, final String circleId, final String memberId, final String level) {
        expect(request(session, "POST", "/circles/" + circleId + "/trustees", object().put("memberId", memberId).put("trustLevel", level)), "addTrustee");
    }

    private void setting(final String admin, final String key, final String value) {
        final ObjectNode body = object().put("credentialType", "SESSION").put("credential", admin);
        body.putObject("settings").put(key, value);
        final JsonNode reply = json(expect(request(null, "POST", "/settings", body), "settings"));
        if (reply.path("returnCode").asInt() != 200) {
            throw new IllegalStateException("The setting " + key + " wasn't taken: " + reply);
        }
    }

    private JsonNode circle(final String session, final String circleId) {
        return json(expect(request(session, "GET", "/circles/" + circleId, null), "getCircle")).path("circles").path(0);
    }

    private String post(final String session, final String path, final ObjectNode body, final String field) {
        return json(expect(request(session, "POST", path, body), path)).path(field).asText();
    }

    private HttpResponse<byte[]> request(final String session, final String method, final String path, final JsonNode body) {
        final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(api + path)).timeout(Duration.ofMinutes(5));
        if (session != null) {
            builder.header("Authorization", "Bearer " + session);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        builder.method(method, (body != null)
                ? HttpRequest.BodyPublishers.ofString(body.toString())
                : HttpRequest.BodyPublishers.noBody());
        return send(builder.build());
    }

    private HttpResponse<byte[]> send(final HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private HttpResponse<byte[]> expect(final HttpResponse<byte[]> response, final String what) {
        if (response.statusCode() != 200) {
            throw new IllegalStateException(what + " failed with " + response.statusCode() + ": " + new String(response.body(), StandardCharsets.UTF_8));
        }
        return response;
    }

    private static JsonNode json(final HttpResponse<byte[]> response) {
        try {
            return JSON.readTree(response.body());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ObjectNode object() {
        return JSON.createObjectNode();
    }

    private static String passphrase(final SecureRandom random) {
        final String alphabet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789-_.!";
        final StringBuilder builder = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private void log(final String line) {
        System.out.println(line);
    }

    private static String bytes(final long bytes) {
        if (bytes >= (1L << 30)) {
            return String.format(Locale.ROOT, "%.2f GiB", bytes / (double) (1L << 30));
        }
        return (bytes >= (1L << 20)) ? String.format(Locale.ROOT, "%.1f MiB", bytes / (double) (1L << 20))
                : String.format(Locale.ROOT, "%.1f KiB", bytes / 1024.0);
    }

    /** The size of a run. */
    private record Settings(long size, long objectMax, long objectMin, int threads, int uploads, boolean concurrent, long seed) {

        static Settings fromSystemProperties() {
            final long size = parseSize(System.getProperty("eds.rotation.size", "2GiB"));
            final long max = Math.min(MAX_OBJECT, parseSize(System.getProperty("eds.rotation.object.max", "256MiB")));
            final long min = Math.min(max, parseSize(System.getProperty("eds.rotation.object.min", "1KiB")));
            final String seed = System.getProperty("eds.rotation.seed", "").trim();
            return new Settings(size, max, min,
                    Integer.getInteger("eds.rotation.threads", 2),
                    Integer.getInteger("eds.rotation.uploads", 4),
                    Boolean.parseBoolean(System.getProperty("eds.rotation.concurrent", "true")),
                    seed.isEmpty() ? new SecureRandom().nextLong() : Long.parseLong(seed));
        }

        /** A size such as 1000, 512KiB, 512MiB or 25GiB. */
        static long parseSize(final String value) {
            final String text = value.trim().toUpperCase(Locale.ROOT);
            final long factor;
            final String number;
            if (text.endsWith("GIB")) {
                factor = 1L << 30;
                number = text.substring(0, text.length() - 3);
            } else if (text.endsWith("MIB")) {
                factor = 1L << 20;
                number = text.substring(0, text.length() - 3);
            } else if (text.endsWith("KIB")) {
                factor = 1L << 10;
                number = text.substring(0, text.length() - 3);
            } else {
                factor = 1;
                number = text;
            }
            return (long) (Double.parseDouble(number.trim()) * factor);
        }

        @Override
        public String toString() {
            return "size " + bytes(size) + ", objects of " + bytes(objectMin) + " to " + bytes(objectMax) + ", " + threads + " threads, "
                    + uploads + " uploads at a time, concurrent " + concurrent + ", seed " + seed;
        }
    }
}
