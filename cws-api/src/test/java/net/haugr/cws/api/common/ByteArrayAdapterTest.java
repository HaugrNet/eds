/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.api.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since CWS 2.0
 */
final class ByteArrayAdapterTest {

    @Test
    void testBackAndForthConverting() {
        final ByteArrayAdapter adapter = new ByteArrayAdapter();
        final String data = UUID.randomUUID().toString();
        final String base64 = adapter.adaptToJson(data.getBytes(StandardCharsets.UTF_8));
        final String result = new String(adapter.adaptFromJson(base64), StandardCharsets.UTF_8);

        assertEquals(result, data);
    }

    @Test
    void testNullValues() {
        final ByteArrayAdapter adapter = new ByteArrayAdapter();
        assertNull(adapter.adaptFromJson(null));
        assertNull(adapter.adaptToJson(null));
    }
}
