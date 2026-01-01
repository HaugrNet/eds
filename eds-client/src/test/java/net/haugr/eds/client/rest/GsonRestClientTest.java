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
package net.haugr.eds.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.Gson;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import net.haugr.eds.api.dtos.Metadata;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class GsonRestClientTest {

    private final Gson gson = GsonRestClient.createGsonInstance();

    @Test
    void testSerializeLocalDateTime() {
        final LocalDateTime date = LocalDateTime.of(2025, 12, 24, 12, 0, 0);
        final String json = gson.toJson(date);

        assertEquals("\"2025-12-24T12:00\"", json);
    }

    @Test
    void testDeserializeLocalDateTimeFromString() {
        final String json = "{\"dataId\":\"123\",\"added\":\"2025-12-24T12:00:00\"}";
        final Metadata metadata = gson.fromJson(json, Metadata.class);

        assertNotNull(metadata);
        assertEquals("123", metadata.getDataId());
        assertEquals(LocalDateTime.of(2025, 12, 24, 12, 0, 0), metadata.getAdded());
    }

    @Test
    void testDeserializeLocalDateTimeFromNumber() {
        final LocalDateTime expected = LocalDateTime.of(2025, 12, 24, 12, 0, 0);
        final long epochMilli = expected.toInstant(ZoneOffset.UTC).toEpochMilli();
        final String json = "{\"dataId\":\"123\",\"added\":" + epochMilli + "}";
        final Metadata metadata = gson.fromJson(json, Metadata.class);

        assertNotNull(metadata);
        assertEquals("123", metadata.getDataId());
        assertEquals(expected, metadata.getAdded());
    }
}
