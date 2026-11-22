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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.jce.DataStreams;
import net.haugr.eds.core.jce.SecretEDSKey;
import org.junit.jupiter.api.Test;

/**
 * <p>Tests for the prepared download (PLAN.md, Streaming): the response
 * headers of full, partial and failed downloads, and which Circle Secret
 * the content is read with while the secret is rotated.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ContentDownloadTest {

    private static final DataFormat.Context CONTEXT = new DataFormat.Context("circle", "object", 7);
    private static final int SIZE = 100;

    @Test
    void testFullDownload() {
        final SecretEDSKey secret = newSecret();
        final ContentDownload download = prepare(secret, ByteRange.parse(null, SIZE));

        assertTrue(download.isOk());
        assertNull(download.error());
        assertFalse(download.isPartial());
        assertEquals(200, download.status());
        assertEquals(SIZE, download.size());
        assertEquals(SIZE, download.contentLength());
        assertNull(download.contentRange());
        assertEquals(42, download.dataId());
        assertEquals("object", download.objectId());
        assertSame(CONTEXT, download.context());
        assertSame(secret, download.circleSecret());
        assertEquals("\"etag\"", download.etag());
        assertEquals("video/mp4", download.contentType());
        assertTrue(download.contentDisposition().startsWith("inline; filename=\"clip.mp4\""), download.contentDisposition());

        final DataStreams.Range plan = download.plan();
        assertEquals(0, plan.firstChunk());
        assertEquals(SIZE, plan.length());
    }

    @Test
    void testPartialDownload() {
        final ContentDownload download = prepare(newSecret(), ByteRange.parse("bytes=10-19", SIZE));

        assertTrue(download.isPartial());
        assertEquals(206, download.status());
        assertEquals(10, download.contentLength());
        assertEquals("bytes 10-19/" + SIZE, download.contentRange());
        assertEquals(10, download.plan().skip());
        assertEquals(10, download.plan().length());
    }

    @Test
    void testFailedDownloads() {
        final EDSResponse error = new EDSResponse(ReturnCode.RANGE_NOT_SATISFIABLE, "Out of range.");
        final byte[] stored = DataFormat.encrypt(newSecret(), CONTEXT, new byte[SIZE]);
        final DataFormat.Header header = DataFormat.Header.read(stored);

        final ContentDownload unsatisfiable = ContentDownload.unsatisfiable(error, header, stored.length, "\"etag\"");
        assertFalse(unsatisfiable.isOk());
        assertSame(error, unsatisfiable.error());
        assertEquals("bytes */" + SIZE, unsatisfiable.contentRange());

        final ContentDownload failed = ContentDownload.failed(error);
        assertFalse(failed.isOk());
        assertEquals(0, failed.size());
        assertNull(failed.plan());
        assertNull(failed.header());
        assertEquals("bytes */0", failed.contentRange());
    }

    @Test
    void testReadingWhileTheSecretIsRotated() {
        final SecretEDSKey retiring = newSecret();
        final SecretEDSKey current = newSecret();
        final ContentDownload download = prepare(retiring, null);

        assertFalse(download.isPartial());
        assertEquals(SIZE, download.contentLength());

        // Not rotated: only the prepared secret reads the content.
        assertSame(retiring, download.reading(CONTEXT.circleSecretVersion()).secret());
        assertNull(download.reading(8));

        // Rotated: the content may now be on the new secret.
        assertSame(download, download.rotating(new ContentDownload.Rotated(8, current)));
        final ContentDownload.Reading reading = download.reading(8);
        assertSame(current, reading.secret());
        assertEquals(new DataFormat.Context("circle", "object", 8), reading.context());
        assertSame(retiring, download.reading(CONTEXT.circleSecretVersion()).secret());
        assertNull(download.reading(9));
    }

    private static ContentDownload prepare(final SecretEDSKey secret, final ByteRange range) {
        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, new byte[SIZE]);
        final DataFormat.Header header = DataFormat.Header.read(stored);
        final ContentDownload download = ContentDownload.prepared(new ContentDownload.Stored(42, header, stored.length, "\"etag\""),
                new ContentDownload.Described("object", "video/mp4", "clip.mp4"), CONTEXT, range, secret);
        assertSame(header, download.header());
        assertEquals(stored.length, download.storedLength());

        return download;
    }

    private static SecretEDSKey newSecret() {
        return Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
    }
}
