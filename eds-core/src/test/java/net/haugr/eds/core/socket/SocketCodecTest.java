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
package net.haugr.eds.core.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.SocketFrameType;
import net.haugr.eds.api.dtos.CircleFeature;
import net.haugr.eds.api.dtos.Entry;
import net.haugr.eds.api.dtos.SocketFrame;
import net.haugr.eds.core.exceptions.EDSException;
import org.junit.jupiter.api.Test;

/**
 * The frames of the socket as JSON: what is refused when it is read, and
 * what is left out when it is written.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class SocketCodecTest {

    @Test
    void testTheJsonProviderIsLookedUpOnce() {
        // Ticket 35: each frame looked the provider up again, reading the JAR.
        final String property = jakarta.json.spi.JsonProvider.JSONP_PROVIDER_FACTORY;
        final String before = System.getProperty(property);
        System.setProperty(property, CountingJsonProvider.class.getName());
        try {
            CountingJsonProvider.MADE.set(0);
            for (int i = 0; i < 50; i++) {
                final SocketFrame frame = new SocketFrame(SocketFrameType.LEFT);
                frame.setCircleId("circle " + i);
                assertTrue(SocketCodec.write(frame).contains("circle " + i));
            }
            // Once, if the factories weren't made before in this JVM.
            assertTrue(CountingJsonProvider.MADE.get() <= 1, "The provider was made " + CountingJsonProvider.MADE.get() + " times");
        } finally {
            if (before == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, before);
            }
        }
    }

    @Test
    void testATextFieldIsText() {
        assertRefused("{\"type\":\"JOIN\",\"circleId\":5}", "The field 'circleId' must be text.");
    }

    @Test
    void testAFlagIsTrueOrFalse() {
        assertRefused("{\"type\":\"LIST\",\"bare\":\"yes\"}", "The field 'bare' must be true or false.");
        assertFalse(SocketCodec.parse("{\"type\":\"LIST\",\"bare\":false}").getBare());
    }

    @Test
    void testANumberIsWholeAndFitsALong() {
        assertRefused("{\"type\":\"LIST\",\"limit\":1.5}", "The field 'limit' must be a whole number.");
        assertRefused("{\"type\":\"LIST\",\"after\":123456789012345678901234567890}", "The field 'after' must be a whole number.");
    }

    @Test
    void testOnlyWhatIsSetIsWritten() {
        final CircleFeature bare = new CircleFeature();
        bare.setName("board");
        final Entry entry = new Entry();
        entry.setEntryId("8e9c7a10-6f0b-4d1e-9a51-3c2f4b6d7e81");
        entry.setKind("note");
        entry.setPosition(7L);
        entry.setAdded(LocalDateTime.of(2026, 11, 19, 12, 0));
        entry.setClosed(Boolean.TRUE);
        final SocketFrame frame = new SocketFrame(SocketFrameType.JOINED);
        frame.setFeatures(List.of(bare));
        frame.setEntry(entry);

        final String json = SocketCodec.write(frame);

        assertTrue(json.contains("{\"name\":\"board\"}"), json);
        assertTrue(json.contains("\"position\":7"), json);
        assertTrue(json.contains("\"added\":\"2026-11-19T12:00\""), json);
        assertTrue(json.contains("\"closed\":true"), json);
    }

    @Test
    void testAFrameHasAType() {
        assertRefused("{\"ref\":\"1\"}", "The frame has no known type.");
        assertRefused("{\"type\":null}", "The frame has no known type.");
    }

    @Test
    void testANullFieldIsNotThere() {
        final SocketFrame frame = SocketCodec.parse("{\"type\":\"LIST\",\"circleId\":null,\"bare\":null,\"limit\":null}");

        assertEquals(SocketFrameType.LIST, frame.getType());
        assertNull(frame.getCircleId());
        assertNull(frame.getBare());
        assertNull(frame.getLimit());
    }

    @Test
    void testAnEntryIsWrittenWithoutWhatItLacks() {
        final Entry entry = new Entry();
        entry.setEntryId("8e9c7a10-6f0b-4d1e-9a51-3c2f4b6d7e81");
        final SocketFrame frame = new SocketFrame(SocketFrameType.REMOVED);
        frame.setEntry(entry);

        assertEquals("{\"type\":\"REMOVED\",\"entry\":{\"entryId\":\"8e9c7a10-6f0b-4d1e-9a51-3c2f4b6d7e81\"}}", SocketCodec.write(frame));
    }

    private static void assertRefused(final String frame, final String message) {
        final EDSException cause = assertThrows(EDSException.class, () -> SocketCodec.parse(frame));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals(message, cause.getMessage());
    }
}
