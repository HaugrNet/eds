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
package net.haugr.eds.load;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * <p>A load test of a stone: it sets up users and rooms, lets the users
 * work at once for the set time while an administrator changes who is on
 * the stone, then checks that the stone still holds exactly what the users
 * left in it, and reports what it measured.</p>
 *
 * <p>Started by {@link StoneLoadIT} against a stone of its own, or with
 * {@link #main} against a running one (an empty test stone, or one whose
 * System Administrator is given; the test makes its own users and rooms
 * and deletes them at the end):</p>
 *
 * <pre>
 * java -cp ... net.haugr.eds.load.LoadRun   # with -Deds.load.url=... and the other properties of {@link LoadSettings}
 * </pre>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class LoadRun {

    /** What a run found. */
    record Result(String report, long failed, long total) {
    }

    private final LoadSettings settings;
    private final Metrics metrics = new Metrics();
    private final World world;
    private final Administrator admin;
    private final List<VirtualUser> users = new ArrayList<>();
    private final StringBuilder out = new StringBuilder(16384);
    private Instant loadStarted = Instant.now();

    LoadRun(final LoadSettings settings) throws IOException {
        this.settings = settings;
        this.world = new World(settings, metrics, settings.url());
        this.admin = new Administrator(world);
    }

    /**
     * Runs the load test against the stone given with {@code eds.load.url};
     * all settings are system properties, so it takes no arguments.
     *
     * @throws Exception if the run can't be made
     */
    // A command line tool ends with its exit code, which tells a script
    // whether the run passed; there is no container to leave running.
    @SuppressWarnings("PMD.DoNotTerminateVM")
    public static void main() throws Exception {
        final LoadSettings settings = LoadSettings.fromSystemProperties();
        if (settings.url() == null) {
            throw new IllegalArgumentException("Give the stone with -Deds.load.url=http://host:port/api/v1, or run StoneLoadIT to start one.");
        }
        final Result result = new LoadRun(settings).run();
        System.exit((result.failed() > settings.maxFailures()) ? 1 : 0);
    }

    Result run() throws InterruptedException {
        log("Load test of " + settings.url() + ": " + settings.describe());
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            setUp(pool);
            final String setUpReport = metrics.report();
            log("\nSet-up:\n" + setUpReport);
            if (metrics.failed() > 0) {
                log("The set-up failed; no load is started.");
                return new Result(out.toString(), metrics.failed(), metrics.total());
            }
            load();
            check();
        } finally {
            cleanUp();
            users.forEach(VirtualUser::close);
        }
        log("\nThe stone's own view of the load (GET /status), per sample:");
        admin.status().forEach(line -> log("  " + line));
        log("\nLoad:\n" + metrics.report());
        final long failed = metrics.failed();
        log(failed == 0 ? "No failures." : failed + " failures; see the examples above.");
        return new Result(out.toString(), failed, metrics.total());
    }

    // =========================================================================
    // Set-up
    // =========================================================================

    private void setUp(final ExecutorService pool) throws InterruptedException {
        admin.login();
        admin.offerFeatures();

        // Making accounts is costly (key pairs, the passphrase's key), so a few at a time.
        final Semaphore few = new Semaphore(4);
        for (int i = 0; i < settings.users(); i++) {
            users.add(new VirtualUser(world, i, World.passphrase()));
        }
        all(pool, users.stream().map(user -> (Callable<Object>) () -> {
            few.acquire();
            try {
                user.memberId = admin.createUser(user.name, user.passphrase);
            } finally {
                few.release();
            }
            return null;
        }).toList());
        all(pool, users.stream().map(user -> (Callable<Object>) () -> {
            few.acquire();
            try {
                if (!user.login()) {
                    throw new IllegalStateException(user.name + " can't log in");
                }
            } finally {
                few.release();
            }
            return null;
        }).toList());

        // Room r is made by user r, who is its administrator; user u is in rooms u, u+1 and u+2.
        final int rooms = Math.max(1, Math.min(settings.rooms(), settings.users()));
        final int perUser = Math.min(3, rooms);
        for (int r = 0; r < rooms; r++) {
            final VirtualUser creator = users.get(r);
            final Http.Reply created = creator.http().post("setup.room.create", "/circles", Http.object().put("circleName", world.run + "-room" + r));
            if (!created.ok()) {
                throw new IllegalStateException("The room " + r + " wasn't made: " + created.describe());
            }
            final String circleId = created.text("circleId");
            for (final String feature : World.FEATURES) {
                creator.http().put("setup.room.feature", "/circles/" + circleId + "/features/" + feature, Http.object().put("state", "ON"));
            }
            final List<Integer> members = new ArrayList<>();
            for (int u = 0; u < users.size(); u++) {
                boolean in = false;
                for (int k = 0; k < perUser; k++) {
                    in |= ((u + k) % rooms) == r;
                }
                if (in) {
                    members.add(u);
                    users.get(u).circles.add(circleId);
                    if (u != r) {
                        creator.http().post("setup.room.trustee", "/circles/" + circleId + "/trustees",
                                Http.object().put("memberId", users.get(u).memberId).put("trustLevel", "WRITE"));
                    }
                }
            }
            world.rooms.add(new World.Room(r, circleId, world.run + "-room" + r, members));
        }

        all(pool, users.stream().map(user -> (Callable<Object>) () -> {
            if (!user.connect()) {
                throw new IllegalStateException(user.name + " has no socket");
            }
            return null;
        }).toList());
    }

    private static void all(final ExecutorService pool, final List<Callable<Object>> tasks) throws InterruptedException {
        final List<Future<Object>> futures = pool.invokeAll(tasks);
        for (final Future<Object> future : futures) {
            try {
                future.get();
            } catch (java.util.concurrent.ExecutionException e) {
                throw new IllegalStateException("The set-up failed: " + e.getCause().getMessage(), e.getCause());
            }
        }
    }

    // =========================================================================
    // The load
    // =========================================================================

    private void load() throws InterruptedException {
        metrics.start();
        loadStarted = Instant.now();
        admin.sampleStatus();
        final List<Thread> threads = new ArrayList<>();
        final long step = settings.rampUp().toNanos() / Math.max(1, users.size());
        for (final VirtualUser user : users) {
            threads.add(Thread.ofVirtual().name(user.name).start(user));
            pace(Duration.ofNanos(step));
        }
        if (settings.churn()) {
            threads.add(Thread.ofVirtual().name("administrator").start(admin));
        }
        final Instant end = Instant.now().plus(settings.duration());
        int second = 0;
        while (Instant.now().isBefore(end)) {
            pace(Duration.ofSeconds(1));
            if ((++second % 10) == 0) {
                admin.sampleStatus();
                log(String.format("  %4d s: %d requests, %d failed", second, metrics.total(), metrics.failed()));
            }
        }
        metrics.endLoad();
        world.stopping = true;
        for (final Thread thread : threads) {
            if (!thread.join(Duration.ofMinutes(5))) {
                metrics.note("The thread " + thread.getName() + " didn't end within 5 minutes after the load; it hangs.");
            }
        }
        metrics.stop();
        admin.sampleStatus();
        users.forEach(VirtualUser::stop);
    }

    // =========================================================================
    // The checks after the load
    // =========================================================================

    private void check() {
        log("\nChecking what the stone holds ...");
        users.forEach(VirtualUser::verifyAll);
        for (final World.Room room : world.rooms) {
            final Http http = users.get(room.index()).http();
            final Set<String> listed = listAll(http, "/data?circleId=" + room.circleId(), new HashSet<>());
            final Set<String> expected = world.objectsOf(room.circleId());
            final Set<String> missing = new HashSet<>(expected);
            missing.removeAll(listed);
            final Set<String> extra = new HashSet<>(listed);
            extra.removeAll(expected);
            http.verdict("final.check.objects", missing.isEmpty() && extra.isEmpty(), room.name() + " lists " + listed.size()
                    + " objects, expected " + expected.size() + "; missing " + missing + ", unexpected " + extra);

            final Http.Reply count = http.get("final.count", "/circles/" + room.circleId() + "/features/chat/count?kind=message");
            if (count.ok()) {
                final long counted = count.json().path("count").asLong(-1);
                final long added = world.messagesOf(room.circleId()).get();
                http.verdict("final.check.messages", counted == added, room.name() + " counts " + counted + " messages, the users left " + added);
                log(String.format("  %s: %d objects, %d messages", room.name(), listed.size(), counted));
            }
        }
    }

    /** All IDs in a folder (or the room's top level) and its sub-folders. */
    private Set<String> listAll(final Http http, final String path, final Set<String> found) {
        for (int page = 1; ; page++) {
            final String separator = path.contains("?") ? "&" : "?";
            final Http.Reply reply = http.get("final.list", path + separator + "pageSize=100&pageNumber=" + page);
            if (!reply.ok()) {
                return found;
            }
            final JsonNode metadata = reply.json().path("metadata");
            int seen = 0;
            for (final JsonNode object : metadata) {
                seen++;
                final String dataId = object.path("dataId").asText();
                if (found.add(dataId) && "folder".equals(object.path("typeName").asText())) {
                    listAll(http, "/data/" + dataId, found);
                }
            }
            if ((seen == 0) || (seen < 100)) {
                return found;
            }
        }
    }

    // =========================================================================
    // Clean-up
    // =========================================================================

    private void cleanUp() {
        if (!settings.cleanup()) {
            return;
        }
        for (final World.Room room : world.rooms) {
            users.get(room.index()).http().delete("cleanup.room", "/circles/" + room.circleId());
        }
        for (final VirtualUser user : users) {
            if (user.memberId != null) {
                admin.http().delete("cleanup.user", "/members/" + user.memberId);
            }
        }
    }

    // The ramp-up and the ticks of the load are paced by the clock: deliberate, not a wait for a condition.
    @SuppressWarnings("java:S2925")
    private static void pace(final Duration time) throws InterruptedException {
        Thread.sleep(time);
    }

    private void log(final String line) {
        System.out.println(line);
        out.append(line).append('\n');
    }

    /**
     * The report as an HTML page with charts of the load over time.
     *
     * @param result  The result of {@link #run()}
     * @param verdict The outcome, and what is known of the stone after the run
     * @return HTML
     */
    String html(final Result result, final List<String> verdict) {
        return HtmlReport.render(settings, metrics, admin.samples(), loadStarted, verdict, result.report());
    }

    /** Writes the report to a file. */
    static void save(final Result result, final Path file) throws IOException {
        final Path folder = file.toAbsolutePath().getParent();
        if (folder != null) {
            Files.createDirectories(folder);
        }
        Files.writeString(file, result.report());
    }
}
