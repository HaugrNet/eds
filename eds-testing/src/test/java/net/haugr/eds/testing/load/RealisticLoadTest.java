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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The load test asks as the clients do (ticket 38): it counts the messages
 * after the position it read up to, and has a Circle Secret rotated once a
 * period, not all the time.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class RealisticLoadTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void testARotationIsAskedForAtOnceThenOnceAPeriod() {
        final Duration every = Duration.ofMinutes(20);
        final long start = 1_000_000L;

        assertTrue(Administrator.rotationDue(null, start, every));
        assertFalse(Administrator.rotationDue(start, start + every.toNanos() - 1, every));
        assertTrue(Administrator.rotationDue(start, start + every.toNanos(), every));
    }

    @Test
    void testTheRotationPeriodIsTwentyMinutesUnlessSet() {
        final String before = System.getProperty("eds.load.rotate.every");
        try {
            System.clearProperty("eds.load.rotate.every");
            assertEquals(Duration.ofMinutes(20), LoadSettings.fromSystemProperties().rotateEvery());
            System.setProperty("eds.load.rotate.every", "PT1M");
            assertEquals(Duration.ofMinutes(1), LoadSettings.fromSystemProperties().rotateEvery());
            assertTrue(LoadSettings.fromSystemProperties().describe().contains("a rotation every PT1M"));
        } finally {
            if (before == null) {
                System.clearProperty("eds.load.rotate.every");
            } else {
                System.setProperty("eds.load.rotate.every", before);
            }
        }
    }

    @Test
    void testTheNewestMessageHeardIsKeptForEachRoom() throws Exception {
        final Map<String, Long> heard = new HashMap<>();

        VirtualUser.noteHeard(heard, "room", entry("chat", "message", 7));
        VirtualUser.noteHeard(heard, "room", entry("chat", "message", 5));
        VirtualUser.noteHeard(heard, "room", entry("chat", "receipt", 9));
        VirtualUser.noteHeard(heard, "room", entry("plans", "message", 11));
        VirtualUser.noteHeard(heard, "other", MAPPER.readTree("{\"feature\":\"chat\",\"kind\":\"message\"}"));

        assertEquals(Map.of("room", 7L), heard);
    }

    private static JsonNode entry(final String feature, final String kind, final long position) throws Exception {
        return MAPPER.readTree("{\"feature\":\"" + feature + "\",\"kind\":\"" + kind + "\",\"position\":" + position + "}");
    }
}
