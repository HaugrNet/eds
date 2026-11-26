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

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>The System Administrator of the test: it claims the stone (or logs in
 * to it), offers the features, and during the load keeps changing who is on
 * the stone, which is the part the users can't do: accounts and rooms come
 * and go, access is given, lowered and taken, invitations are accepted.</p>
 *
 * <p>Each such cycle also checks the rules under load: a reader may not
 * write, a removed trustee may not read, a deleted account may not log
 * in.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class Administrator implements Runnable {

    /** The refusals of a request the rules forbid. */
    private static final Set<Integer> REFUSED = Set.of(401, 403, 404);

    private final World world;
    private final Http http;
    private final SplittableRandom random;
    private final List<JsonNode> status = new ArrayList<>();
    private long lastSequence = 0;
    /**
     * Numbers the names of what the cycles make. Only the thread of {@link #run()}
     * uses it; an AtomicLong all the same, so no write of it can be torn.
     */
    private final AtomicLong counter = new AtomicLong();
    private long lastSanity = System.nanoTime();
    /** When the last rotation was asked for (System.nanoTime), or null before the first. */
    private Long lastRotation = null;

    Administrator(final World world) {
        this.world = world;
        this.http = new Http(world.url, world.metrics);
        this.random = new SplittableRandom(world.settings.seed() * 17);
    }

    Http http() {
        return http;
    }

    /** Logs in; on an empty stone this claims it. */
    void login() {
        final Http.Reply reply = http.post("admin.login", "/members/login", Http.object()
                .put("accountName", world.settings.admin()).put("credential", World.base64(world.settings.passphrase())));
        if (!reply.ok()) {
            throw new IllegalStateException("The System Administrator '" + world.settings.admin() + "' can't log in: " + reply.describe());
        }
        http.session(reply.text("session"));
    }

    /** Offers Rune's features, so rooms can have them. */
    void offerFeatures() {
        for (final Map.Entry<String, String> declaration : world.declarations.entrySet()) {
            final Http.Reply reply = http.put("admin.feature.offer", "/features/" + declaration.getKey(), Http.object().put("declaration", declaration.getValue()));
            if (!reply.ok()) {
                throw new IllegalStateException("The stone refused the feature " + declaration.getKey() + ": " + reply.describe());
            }
        }
    }

    /** Makes an account; its member ID. */
    String createUser(final String name, final String passphrase) {
        final Http.Reply reply = http.post("admin.user.create", "/members", Http.object()
                .put("newAccountName", name).put("newCredential", World.base64(passphrase)).put("memberRole", "STANDARD"));
        if (!reply.ok()) {
            throw new IllegalStateException("The stone refused the user " + name + ": " + reply.describe());
        }
        return reply.text("memberId");
    }

    // =========================================================================
    // During the load
    // =========================================================================

    @Override
    public void run() {
        while (!world.stopping) {
            final int choice = random.nextInt(20);
            try {
                if (choice < 6) {
                    accountCycle();
                } else if (choice < 8) {
                    invitationCycle();
                } else if (choice < 10) {
                    roomCycle(random.nextBoolean());
                } else if (choice < 12) {
                    dataTypeCycle();
                } else if (choice < 13) {
                    featureCycle();
                } else if (choice < 15) {
                    statusKeyCycle();
                } else if (choice < 16) {
                    http.post("admin.settings", "/settings", http.sessionBody());
                } else if (choice < 18) {
                    http.get("admin.members", "/members");
                    http.get("admin.circles", "/circles");
                } else if (choice < 19) {
                    rotateRoom();
                } else {
                    sanity();
                }
            } catch (RuntimeException e) {
                world.metrics.tally("admin", Metrics.Outcome.FAILED, 0, 0, 0, "the test failed: " + e);
            }
            pause(random.nextInt(50, 300));
        }
    }

    /**
     * The Circle Secret of a shared room is rotated (PLAN.md, Crypto C6): the
     * System Administrator sets it due, and the next request of a user in
     * the room starts it, so its content is re-encrypted under the load.
     * What the users keep is compared byte for byte at the end, as always.
     * One at once, then one each {@code eds.load.rotate.every}: a stone
     * rotates each Circle once a year by default, so a rotation all the time
     * would load it with re-encryption no stone in use has.
     */
    private void rotateRoom() {
        final long now = System.nanoTime();
        if (rotationDue(lastRotation, now, world.settings.rotateEvery()) && !world.rooms.isEmpty()) {
            lastRotation = now;
            final World.Room room = world.rooms.get(random.nextInt(world.rooms.size()));
            http.put("admin.room.rotate", "/circles/" + room.circleId() + "/rotation", null);
        }
    }

    /**
     * Whether the next rotation is due: the first at once, then one a period.
     *
     * @param last  When the last was asked for (System.nanoTime), or null
     * @param now   Now (System.nanoTime)
     * @param every The period
     * @return Whether one is due
     */
    static boolean rotationDue(final Long last, final long now, final Duration every) {
        return (last == null) || ((now - last) >= every.toNanos());
    }

    /**
     * An account comes and goes: made, given a room, writes and reads,
     * becomes a reader, loses the room, is deleted, and can't log in any
     * more.
     */
    private void accountCycle() {
        final long number = counter.incrementAndGet();
        final String name = world.run + "-churn" + number;
        final String passphrase = World.passphrase();
        final Http.Reply created = http.post("churn.user.create", "/members", Http.object()
                .put("newAccountName", name).put("newCredential", World.base64(passphrase)).put("memberRole", "STANDARD"));
        if (!created.ok()) {
            return;
        }
        final String memberId = created.text("memberId");
        final String circleId = createRoom(world.run + "-churn-room" + number, true);
        try (Http user = new Http(world.url, world.metrics)) {
            if (circleId != null) {
                http.post("churn.trustee.add", "/circles/" + circleId + "/trustees", Http.object().put("memberId", memberId).put("trustLevel", "WRITE"));
            }
            final Http.Reply login = user.post("churn.login", "/members/login", Http.object().put("accountName", name).put("credential", World.base64(passphrase)));
            if (login.ok() && (circleId != null)) {
                user.session(login.text("session"));
                churnWork(user, circleId);

                http.put("churn.trustee.alter", "/circles/" + circleId + "/trustees/" + memberId, Http.object().put("trustLevel", "READ"));
                user.get("churn.read.as.reader", "/data?circleId=" + circleId + "&pageSize=10&pageNumber=1");
                final Http.Reply refused = user.json("churn.write.as.reader", "POST", "/circles/" + circleId + "/features/chat/entries",
                        Http.object().put("kind", "message").put("content", World.base64("{\"v\":1,\"text\":\"must be refused\"}")), REFUSED);
                user.verdict("check.reader.cannot.write", !refused.ok(), "a reader wrote a message in " + circleId);

                http.delete("churn.trustee.remove", "/circles/" + circleId + "/trustees/" + memberId);
                final Http.Reply gone = user.json("churn.read.after.removal", "GET", "/data?circleId=" + circleId + "&pageSize=10&pageNumber=1", null, REFUSED);
                user.verdict("check.removed.cannot.read", !gone.ok(), "a removed trustee read the room " + circleId);
            }
            http.delete("churn.user.delete", "/members/" + memberId);
            final Http.Reply after = user.json("churn.login.after.delete", "POST", "/members/login",
                    Http.object().put("accountName", name).put("credential", World.base64(passphrase)), REFUSED);
            user.verdict("check.deleted.cannot.login", !after.ok(), "the deleted account " + name + " logged in");
        }
        if (circleId != null) {
            http.delete("churn.room.delete", "/circles/" + circleId);
        }
    }

    /** What a short-lived account does in its room: a message, a file up and down. */
    private void churnWork(final Http user, final String circleId) {
        user.post("churn.chat", "/circles/" + circleId + "/features/chat/entries",
                Http.object().put("kind", "message").put("content", World.base64("{\"v\":1,\"text\":\"hello from a short-lived account\"}")));
        final long seed = random.nextLong();
        final int size = random.nextInt(1, 256 * 1024);
        final Http.Reply added = user.post("churn.data.add", "/data", Http.object().put("circleId", circleId).put("dataName", "churn-" + seed + ".bin"));
        if (added.ok()) {
            final String dataId = added.text("dataId");
            final byte[] bytes = VirtualUser.content(seed, size);
            if (user.bytes("churn.data.upload", "PUT", "/data/" + dataId + "/content", bytes, "application/octet-stream", Map.of(), Set.of(), true).ok()) {
                final Http.Reply down = user.bytes("churn.data.download", "GET", "/data/" + dataId + "/content", null, null, Map.of(), Set.of(), true);
                if (down.ok()) {
                    user.verdict("check.churn.content", java.util.Arrays.equals(down.body(), bytes), "the content of " + dataId + " differs");
                }
            }
        }
    }

    /** An invitation into a room, accepted with a passphrase of the invited; then both go again. */
    private void invitationCycle() {
        final long number = counter.incrementAndGet();
        final String circleId = createRoom(world.run + "-invite-room" + number, false);
        if (circleId == null) {
            return;
        }
        final String name = world.run + "-invited" + number;
        final Http.Reply invited = http.post("invite.create", "/members/inviteMember", http.sessionBody()
                .put("newAccountName", name).put("circleId", circleId).put("trustLevel", "WRITE"));
        if (invited.ok() && (invited.text("invitation") != null)) {
            final String passphrase = World.passphrase();
            try (Http user = new Http(world.url, world.metrics)) {
                final Http.Reply accepted = user.post("invite.accept", "/members/updateMember", Http.object().put("accountName", name)
                        .put("credential", invited.text("invitation")).put("credentialType", "INVITATION").put("newCredential", World.base64(passphrase)));
                if (accepted.ok()) {
                    final Http.Reply login = user.post("invite.login", "/members/login", Http.object().put("accountName", name).put("credential", World.base64(passphrase)));
                    if (login.ok()) {
                        user.session(login.text("session"));
                        user.get("invite.read", "/data?circleId=" + circleId + "&pageSize=10&pageNumber=1");
                    }
                }
            }
            final String memberId = invited.text("memberId");
            if (memberId != null) {
                http.delete("invite.user.delete", "/members/" + memberId);
            }
        }
        http.delete("churn.room.delete", "/circles/" + circleId);
    }

    /** A room is made, renamed, its features switched, and deleted; over the resource or the older JSON API. */
    private void roomCycle(final boolean resource) {
        final String name = world.run + "-room-cycle" + counter.incrementAndGet();
        final String circleId;
        if (resource) {
            circleId = createRoom(name, true);
        } else {
            final Http.Reply created = http.post("room.create.json", "/circles/createCircle", http.sessionBody().put("circleName", name));
            circleId = created.ok() ? created.text("circleId") : null;
        }
        if (circleId == null) {
            return;
        }
        if (resource) {
            http.put("room.rename", "/circles/" + circleId, Http.object().put("circleName", name + "-renamed"));
            http.put("room.feature.switch", "/circles/" + circleId + "/features/chat", Http.object().put("state", "READONLY"));
            http.get("room.features", "/circles/" + circleId + "/features");
            http.delete("room.feature.remove", "/circles/" + circleId + "/features/chat");
            http.delete("room.delete", "/circles/" + circleId);
        } else {
            http.post("room.rename.json", "/circles/updateCircle", http.sessionBody().put("circleId", circleId).put("circleName", name + "-renamed"));
            http.post("room.delete.json", "/circles/deleteCircle", http.sessionBody().put("circleId", circleId));
        }
    }

    private String createRoom(final String name, final boolean withChat) {
        final Http.Reply created = http.post("room.create", "/circles", Http.object().put("circleName", name));
        if (!created.ok()) {
            return null;
        }
        final String circleId = created.text("circleId");
        if (withChat) {
            http.put("room.feature.on", "/circles/" + circleId + "/features/chat", Http.object().put("state", "ON"));
        }
        return circleId;
    }

    private void dataTypeCycle() {
        final long number = counter.incrementAndGet();
        final String name = world.run + "-type" + number;
        http.put("datatype.save", "/dataTypes/" + name, Http.object().put("type", "application/x-load-" + number));
        http.get("datatype.list", "/dataTypes");
        http.delete("datatype.delete", "/dataTypes/" + name);
    }

    /** A feature of the test's own is offered and withdrawn again; it isn't in any room. */
    private void featureCycle() {
        final String name = "ld" + Long.toString(counter.incrementAndGet(), 36);
        final String declaration = world.declarations.get("calendar").replace("\"calendar\"", "\"" + name + "\"");
        http.put("feature.offer", "/features/" + name, Http.object().put("declaration", declaration));
        http.get("feature.list", "/features");
        http.delete("feature.withdraw", "/features/" + name);
    }

    /** A status key is made, the status read with it as a monitor would, and the key withdrawn. */
    private void statusKeyCycle() {
        final String label = world.run + "-key" + counter.incrementAndGet();
        final Http.Reply created = http.post("status.key.create", "/status/keys", Http.object().put("label", label));
        if (created.ok()) {
            try (Http monitor = new Http(world.url, world.metrics)) {
                monitor.session(created.text("key"));
                monitor.get("status.with.key", "/status");
            }
            http.get("status.key.list", "/status/keys");
            http.delete("status.key.withdraw", "/status/keys/" + Http.query(label));
        }
    }

    /** The sanity check of the stored data, at most once a minute. */
    private void sanity() {
        if ((System.nanoTime() - lastSanity) > 60_000_000_000L) {
            lastSanity = System.nanoTime();
            http.post("admin.sanitized", "/sanitized", http.sessionBody());
        }
    }

    // =========================================================================
    // The stone's own view of the load
    // =========================================================================

    /**
     * Reads the samples of the load which the stone took since the last
     * read (GET /status?since=), and keeps them for the report.
     */
    void sampleStatus() {
        final Http.Reply reply = http.get("admin.status", "/status" + ((lastSequence > 0) ? "?since=" + lastSequence : ""));
        if (reply.ok()) {
            for (final JsonNode sample : reply.json().path("load").path("samples")) {
                final long sequence = sample.path("sequence").asLong();
                if (sequence > lastSequence) {
                    lastSequence = sequence;
                    status.add(sample);
                }
            }
        }
    }

    /** The stone's own samples of the load, as it gave them. */
    List<JsonNode> samples() {
        return List.copyOf(status);
    }

    /** The samples as a table: what the stone measured itself, per interval. */
    List<String> status() {
        final List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "%-8s %9s %7s %9s %9s %7s %7s %6s %6s %9s %8s", "time", "requests", "failed", "mean ms", "max ms",
                "sockets", "users", "cpu %", "load", "heap MiB", "threads"));
        for (final JsonNode sample : status) {
            final JsonNode values = sample.path("values");
            final String time = sample.path("time").asText();
            lines.add(String.format(Locale.ROOT, "%-8s %9.0f %7.0f %9.1f %9.1f %7.0f %7.0f %6.0f %6.1f %9.0f %8.0f",
                    (time.length() >= 19) ? time.substring(11, 19) : time,
                    values.path("requests").asDouble(), values.path("failed").asDouble(), values.path("requestMillis").asDouble(),
                    values.path("requestMillisMax").asDouble(), values.path("connections").asDouble(), values.path("accounts").asDouble(),
                    values.path("processShare").asDouble() * 100, values.path("systemLoad").asDouble(),
                    values.path("memoryUsed").asDouble() / 1048576.0, values.path("threads").asDouble()));
        }
        return lines;
    }

    // The pause between the cycles is the administrator's think time, not a wait for a condition.
    @SuppressWarnings("java:S2925")
    private static void pause(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
