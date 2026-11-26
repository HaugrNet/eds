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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>What the users of a run share: the stone, the rooms and who is in them,
 * and what the test expects the stone to hold at the end, so it can check
 * that nothing was lost or left behind under the load.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class World {

    /** The features the rooms of the test have, all of Rune's. */
    static final List<String> FEATURES = List.of("chat", "plans", "calendar", "location", "calls");
    private static final Path DECLARATIONS = Path.of("../eds-quarkus/src/main/webui/src/features/declarations");
    /** One source of the passphrases, which every user's thread may use. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** A room of the test. */
    record Room(int index, String circleId, String name, List<Integer> members) {
    }

    final LoadSettings settings;
    final Metrics metrics;
    final String url;
    /** Prefix of every name the run gives, so runs on one stone don't meet. */
    final String run;
    final List<Room> rooms = new ArrayList<>();
    final Map<String, String> declarations;
    /** The Data Objects the users hold, per room: what the room must list at the end. */
    final Map<String, Set<String>> objects = new ConcurrentHashMap<>();
    /** The chat messages added and not removed, per room. */
    final Map<String, AtomicLong> messages = new ConcurrentHashMap<>();
    volatile boolean stopping = false;

    World(final LoadSettings settings, final Metrics metrics, final String url) throws IOException {
        this.settings = settings;
        this.metrics = metrics;
        this.url = url;
        this.run = "load" + Long.toString(Math.abs(settings.seed()) % 1_000_000L, 36);
        this.declarations = declarations();
    }

    Room room(final String circleId) {
        return rooms.stream().filter(room -> room.circleId().equals(circleId)).findFirst().orElse(null);
    }

    Set<String> objectsOf(final String circleId) {
        return objects.computeIfAbsent(circleId, key -> ConcurrentHashMap.newKeySet());
    }

    AtomicLong messagesOf(final String circleId) {
        return messages.computeIfAbsent(circleId, key -> new AtomicLong());
    }

    /**
     * A passphrase which meets the stone's strength rules: 32 random
     * characters. With 24, the estimate of the stone was near its bar of
     * 128 bits, and about one in 20,000 fell below it (a repeat such as
     * "bbb" costs bits); with 32, the lowest of 100,000 was 169 bits.
     */
    static String passphrase() {
        final byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String base64(final String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** The text of Base64-encoded content, or "" if it isn't. */
    static String text(final String base64) {
        try {
            return new String(Base64.getDecoder().decode(base64), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException _) {
            return "";
        }
    }

    /**
     * Rune's declarations of its features, as the System Administrator gives
     * them the stone; read from Rune's sources, so the test follows them.
     */
    private static Map<String, String> declarations() throws IOException {
        final Map<String, String> found = new LinkedHashMap<>();
        for (final String feature : FEATURES) {
            final Path file = DECLARATIONS.resolve(feature + ".json");
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException("The declaration " + file.toAbsolutePath().normalize()
                        + " is missing; the load test runs from the folder eds-testing.");
            }
            found.put(feature, Files.readString(file));
        }
        return found;
    }
}
