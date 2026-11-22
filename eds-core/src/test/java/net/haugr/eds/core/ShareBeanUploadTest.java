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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.EncryptedDataStore;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * <p>Tests for the streamed content upload (PLAN.md, S2), via the
 * ShareBean. The HTTP layer is tested in eds-quarkus.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ShareBeanUploadTest extends DatabaseSetup {

    /**
     * Empty, one chunk, exactly one chunk, an exact multiple of the chunk
     * size, and a partial last chunk.
     */
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 1000, DataFormat.DEFAULT_CHUNK_SIZE, 2 * DataFormat.DEFAULT_CHUNK_SIZE, 200_000 })
    void testUploadRoundTrip(final int size) {
        final ShareBean bean = prepareShareBean();
        final String dataId = register(bean, "upload " + size);
        final byte[] content = (size == 0) ? new byte[0] : generateData(size);

        final ProcessDataResponse response = bean.uploadContent(login(MEMBER_1), dataId, new ByteArrayInputStream(content), size, null);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode(), response.getReturnMessage());
        assertEquals(dataId, response.getDataId());
        assertNotNull(response.getEtag());
        assertArrayEquals(content, fetch(bean, MEMBER_2, dataId));
    }

    @Test
    void testUploadReplacesContentAndChangesETag() {
        final ShareBean bean = prepareShareBean();
        final String dataId = register(bean, "replaced");
        final byte[] token = login(MEMBER_1);

        final ProcessDataResponse first = bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(100)), 100, null);
        final byte[] second = generateData(300);
        final ProcessDataResponse replaced = bean.uploadContent(token, dataId, new ByteArrayInputStream(second), 300, first.getEtag());
        assertTrue(replaced.isOk(), replaced.getReturnMessage());
        assertNotEquals(first.getEtag(), replaced.getEtag());
        assertArrayEquals(second, fetch(bean, MEMBER_1, dataId));

        // The old ETag no longer matches; "*" matches any existing content.
        final ProcessDataResponse stale = bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(10)), 10, first.getEtag());
        assertEquals(ReturnCode.PRECONDITION_FAILED.getCode(), stale.getReturnCode());
        assertArrayEquals(second, fetch(bean, MEMBER_1, dataId));
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(10)), 10, "*").isOk());
    }

    @Test
    void testIfMatchWithoutContent() {
        final ShareBean bean = prepareShareBean();
        final String dataId = register(bean, "no content yet");

        final ProcessDataResponse response = bean.uploadContent(login(MEMBER_1), dataId, new ByteArrayInputStream(generateData(10)), 10, "*");
        assertEquals(ReturnCode.PRECONDITION_FAILED.getCode(), response.getReturnCode());
    }

    /**
     * An upload which fails half way leaves the previous content intact.
     */
    @Test
    void testInterruptedUploadKeepsPreviousContent() {
        final ShareBean bean = prepareShareBean();
        final String dataId = register(bean, "interrupted");
        final byte[] token = login(MEMBER_1);
        final byte[] original = generateData(1000);
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(original), original.length, null).isOk());

        final ProcessDataResponse failed = bean.uploadContent(token, dataId, new FailingStream(150_000), 300_000, null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), failed.getReturnCode());
        assertTrue(failed.getReturnMessage().startsWith("The upload failed and was rolled back"), failed.getReturnMessage());

        // A body shorter than its Content-Length fails the same way.
        final ProcessDataResponse shorter = bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(100)), 200, null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), shorter.getReturnCode());

        entityManager.clear();
        assertArrayEquals(original, fetch(bean, MEMBER_1, dataId));
    }

    @Test
    void testUploadErrors() {
        final ShareBean bean = prepareShareBean();
        final String dataId = register(bean, "errors");
        final byte[] token = login(MEMBER_1);
        final byte[] content = generateData(10);

        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), bean.uploadContent(null, dataId, new ByteArrayInputStream(content), 10, null).getReturnCode());
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), bean.uploadContent(generateData(32), dataId, new ByteArrayInputStream(content), 10, null).getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), bean.uploadContent(token, "4c5bc2c6-0fa5-4c3a-9d1e-1a2b3c4d5e6f", new ByteArrayInputStream(content), 10, null).getReturnCode());

        // member3 has only READ access in circle1.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), bean.uploadContent(login(MEMBER_3), dataId, new ByteArrayInputStream(content), 10, null).getReturnCode());
    }

    /**
     * Content without a length (-1, as from a pipe) is read to its end, and
     * stored in the same format as content with one.
     */
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 1000, DataFormat.DEFAULT_CHUNK_SIZE, DataFormat.DEFAULT_CHUNK_SIZE + 1, 2 * DataFormat.DEFAULT_CHUNK_SIZE, 200_000 })
    void testUploadWithoutLength(final int size) {
        final ShareBean bean = prepareShareBean();
        final String dataId = register(bean, "piped " + size);
        final byte[] content = (size == 0) ? new byte[0] : generateData(size);

        final ProcessDataResponse response = bean.uploadContent(login(MEMBER_1), dataId, new ByteArrayInputStream(content), -1, null);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode(), response.getReturnMessage());
        assertNotNull(response.getEtag());
        entityManager.clear();
        assertArrayEquals(content, fetch(bean, MEMBER_2, dataId));
    }

    /**
     * Without a length, content above the size limit is cut off while it is
     * read; the upload is rolled back, and the previous content stays.
     */
    @Test
    void testUploadWithoutLengthTooLarge() {
        final Settings mySettings = newSettings();
        final ShareBean bean = prepareShareBean(mySettings);
        final String dataId = register(bean, "piped too large");
        final byte[] token = login(MEMBER_1);
        final byte[] original = generateData(100);
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(original), -1, null).isOk());

        mySettings.set(StandardSetting.STREAM_MAX_OBJECT_SIZE.getKey(), "100000");
        final ProcessDataResponse response = bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(100_001)), -1, null);
        assertEquals(ReturnCode.CONTENT_TOO_LARGE.getCode(), response.getReturnCode(), response.getReturnMessage());
        assertEquals("The content is larger than the allowed 100000 bytes.", response.getReturnMessage());
        // Exactly the limit is fine.
        final byte[] atTheLimit = generateData(100_000);
        mySettings.set(StandardSetting.STREAM_MAX_OBJECT_SIZE.getKey(), "50");
        assertEquals(ReturnCode.CONTENT_TOO_LARGE.getCode(), bean.uploadContent(token, dataId, new ByteArrayInputStream(atTheLimit), -1, null).getReturnCode());
        entityManager.clear();
        assertArrayEquals(original, fetch(bean, MEMBER_1, dataId));

        mySettings.set(StandardSetting.STREAM_MAX_OBJECT_SIZE.getKey(), "100000");
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(atTheLimit), -1, null).isOk());
        entityManager.clear();
        assertArrayEquals(atTheLimit, fetch(bean, MEMBER_1, dataId));
    }

    /** A failing source without a length is rolled back as one with. */
    @Test
    void testInterruptedUploadWithoutLength() {
        final ShareBean bean = prepareShareBean();
        final String dataId = register(bean, "piped interrupted");
        final byte[] token = login(MEMBER_1);
        final byte[] original = generateData(1000);
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(original), -1, null).isOk());

        final ProcessDataResponse failed = bean.uploadContent(token, dataId, new FailingStream(150_000), -1, null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), failed.getReturnCode());
        assertTrue(failed.getReturnMessage().startsWith("The upload failed and was rolled back"), failed.getReturnMessage());
        entityManager.clear();
        assertArrayEquals(original, fetch(bean, MEMBER_1, dataId));
    }

    @Test
    void testUploadTooLarge() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.STREAM_MAX_OBJECT_SIZE.getKey(), "100");
        final ShareBean bean = prepareShareBean(mySettings);
        final String dataId = register(prepareShareBean(), "too large");

        final ProcessDataResponse response = bean.uploadContent(login(MEMBER_1), dataId, new ByteArrayInputStream(generateData(101)), 101, null);
        assertEquals(ReturnCode.CONTENT_TOO_LARGE.getCode(), response.getReturnCode());
        assertEquals(413, ReturnCode.CONTENT_TOO_LARGE.getHttpCode());
    }

    @Test
    void testUploadWithAnEmptySessionToken() {
        final ShareBean bean = prepareShareBean();
        final String dataId = register(bean, "empty token");

        final ProcessDataResponse response = bean.uploadContent(new byte[0], dataId, new ByteArrayInputStream(generateData(10)), 10, null);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
    }

    /**
     * Without a length, content is refused before any of it is read, if the
     * storage ceiling is already reached.
     */
    @Test
    void testUploadWithoutLengthWhenTheCeilingIsReached() {
        final Settings mySettings = newSettings();
        final ShareBean bean = prepareShareBean(mySettings);
        final byte[] token = login(MEMBER_1);
        final String stored = register(bean, "below the ceiling");
        assertTrue(bean.uploadContent(token, stored, new ByteArrayInputStream(generateData(100)), 100, null).isOk());

        mySettings.set(StandardSetting.DATA_MAX_TOTAL_SIZE.getKey(), "1");
        final String dataId = register(bean, "no room");
        final ByteArrayInputStream content = new ByteArrayInputStream(generateData(10));
        final ProcessDataResponse response = bean.uploadContent(token, dataId, content, -1, null);
        assertEquals(ReturnCode.INSUFFICIENT_STORAGE.getCode(), response.getReturnCode(), response.getReturnMessage());
        assertEquals(10, content.available());
    }

    /**
     * Stored content too short for a header has no ETag, so it matches no
     * If-Match, not even "*".
     */
    @Test
    void testIfMatchWithDamagedContent() {
        final ShareBean bean = prepareShareBean();
        final String dataId = register(bean, "damaged");
        final byte[] token = login(MEMBER_1);
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(100)), 100, null).isOk());
        final long id = bean.prepareContent(token, dataId, null, null).dataId();
        final byte[] damaged = new byte[DataFormat.HEADER_BYTES - 1];
        new DataDao(entityManager).withConnection(connection -> EncryptedDataStore.write(connection, id, new ByteArrayInputStream(damaged), damaged.length, "SHA-512"));

        final ProcessDataResponse response = bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(10)), 10, "*");
        assertEquals(ReturnCode.PRECONDITION_FAILED.getCode(), response.getReturnCode());
    }

    @Test
    void testUploadToFolder() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setDataName("a folder");
        request.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse folder = bean.processData(request);
        assertTrue(folder.isOk(), folder.getReturnMessage());

        final ProcessDataResponse response = bean.uploadContent(login(MEMBER_1), folder.getDataId(), new ByteArrayInputStream(generateData(10)), 10, null);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private String register(final ShareBean bean, final String name) {
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, name, 0);
        final ProcessDataResponse response = bean.processData(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getDataId();
    }

    private byte[] login(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        final ProcessMemberResponse response = prepareManagementBean().processMember(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getSession();
    }

    private static byte[] fetch(final ShareBean bean, final String account, final String dataId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, account);
        request.setDataId(dataId);
        final FetchDataResponse response = bean.fetchData(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return (response.getData() != null) ? response.getData() : new byte[0];
    }

    /** Delivers some bytes, then fails like a dropped connection. */
    private static final class FailingStream extends InputStream {
        private int remaining;

        private FailingStream(final int before) {
            this.remaining = before;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                throw new IOException("Connection reset");
            }
            remaining--;
            return 42;
        }
    }
}
