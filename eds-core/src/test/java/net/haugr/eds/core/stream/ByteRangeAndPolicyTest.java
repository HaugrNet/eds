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
package net.haugr.eds.core.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ByteRangeAndPolicyTest {

    private static void assertRange(final String header, final long size, final ByteRange.Kind kind, final long first, final long last) {
        final ByteRange range = ByteRange.parse(header, size);
        assertEquals(kind, range.kind(), header);
        if (kind == ByteRange.Kind.PARTIAL) {
            assertEquals(first, range.first(), header);
            assertEquals(last, range.last(), header);
        }
    }

    @Test
    void testRanges() {
        assertRange(null, 100, ByteRange.Kind.FULL, 0, 99);
        assertRange("bytes=0-0", 100, ByteRange.Kind.PARTIAL, 0, 0);
        assertRange("bytes=99-99", 100, ByteRange.Kind.PARTIAL, 99, 99);
        assertRange("bytes=10-", 100, ByteRange.Kind.PARTIAL, 10, 99);
        assertRange("bytes=10-500", 100, ByteRange.Kind.PARTIAL, 10, 99);
        assertRange("bytes=-10", 100, ByteRange.Kind.PARTIAL, 90, 99);
        assertRange("bytes=-500", 100, ByteRange.Kind.PARTIAL, 0, 99);
        assertRange("BYTES = 5-6", 100, ByteRange.Kind.FULL, 0, 0);
        assertRange("bytes= 5-6", 100, ByteRange.Kind.PARTIAL, 5, 6);
        assertRange("bytes=100-", 100, ByteRange.Kind.UNSATISFIABLE, 0, 0);
        assertRange("bytes=-0", 100, ByteRange.Kind.UNSATISFIABLE, 0, 0);
        assertRange("bytes=0-", 0, ByteRange.Kind.UNSATISFIABLE, 0, 0);
        // Ignored: multiple ranges, other units, invalid syntax.
        assertRange("bytes=0-1,5-6", 100, ByteRange.Kind.FULL, 0, 0);
        assertRange("items=0-1", 100, ByteRange.Kind.FULL, 0, 0);
        assertRange("bytes=a-b", 100, ByteRange.Kind.FULL, 0, 0);
        assertRange("bytes=5-1", 100, ByteRange.Kind.FULL, 0, 0);
        assertRange("bytes=5", 100, ByteRange.Kind.FULL, 0, 0);
        assertEquals(1, ByteRange.parse("bytes=3-3", 10).length());
    }

    @Test
    void testContentPolicy() {
        assertTrue(ContentPolicy.isInline("video/mp4"));
        assertTrue(ContentPolicy.isInline("IMAGE/PNG"));
        assertFalse(ContentPolicy.isInline("image/svg+xml"));
        assertFalse(ContentPolicy.isInline("text/html"));
        assertFalse(ContentPolicy.isInline(null));

        assertEquals("video/mp4", ContentPolicy.contentType("video/mp4"));
        assertEquals("application/octet-stream", ContentPolicy.contentType("image/svg+xml"));
        assertEquals("application/octet-stream", ContentPolicy.contentType(null));

        assertEquals("inline; filename=\"clip.mp4\"; filename*=UTF-8''clip.mp4", ContentPolicy.contentDisposition("video/mp4", "clip.mp4"));
        assertEquals("attachment; filename=\"_quoted_ name _.svg\"; filename*=UTF-8''%22quoted%22%20name%20%C3%A6.svg",
                ContentPolicy.contentDisposition("image/svg+xml", "\"quoted\" name æ.svg"));
    }
}
