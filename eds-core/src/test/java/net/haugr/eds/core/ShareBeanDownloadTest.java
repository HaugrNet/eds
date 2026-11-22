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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.managers.DownloadDataManager;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.EncryptedDataStore;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.stream.ContentDownload;
import net.haugr.eds.core.stream.ContentStreamer;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>Tests for the content download (PLAN.md, S3), via the ShareBean:
 * full content, ranges at and around chunk boundaries, If-Range, errors, the
 * serving headers, and a tampered chunk. The HTTP layer is tested in
 * eds-quarkus.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ShareBeanDownloadTest extends DatabaseSetup {

    private static final int CHUNK = DataFormat.DEFAULT_CHUNK_SIZE;
    private static final int SIZE = 3 * CHUNK + 1000;

    @Test
    void testFullAndRanges() throws IOException {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final byte[] content = generateData(SIZE);
        final String dataId = upload(bean, token, "ranges", null, content);

        final ContentDownload full = bean.prepareContent(token, dataId, null, null);
        assertTrue(full.isOk());
        assertEquals(200, full.status());
        assertEquals(SIZE, full.contentLength());
        assertArrayEquals(content, stream(bean, full));

        // First byte, last byte, one byte, a whole chunk, across two chunks,
        // around a boundary, open-ended and the suffix range.
        final long[][] ranges = {
                { 0, 0 }, { SIZE - 1, SIZE - 1 }, { 12345, 12345 }, { CHUNK, 2L * CHUNK - 1 },
                { CHUNK - 10, CHUNK + 9 }, { CHUNK - 1, CHUNK }, { 2L * CHUNK, SIZE - 1 }, { 1, 3L * CHUNK + 5 } };
        for (final long[] range : ranges) {
            final ContentDownload partial = bean.prepareContent(token, dataId, "bytes=" + range[0] + '-' + range[1], null);
            assertEquals(206, partial.status());
            assertEquals("bytes " + range[0] + '-' + range[1] + '/' + SIZE, partial.contentRange());
            assertArrayEquals(Arrays.copyOfRange(content, (int) range[0], (int) range[1] + 1), stream(bean, partial), "Range " + range[0] + '-' + range[1]);
        }
        final ContentDownload suffix = bean.prepareContent(token, dataId, "bytes=-100", null);
        assertArrayEquals(Arrays.copyOfRange(content, SIZE - 100, SIZE), stream(bean, suffix));
        final ContentDownload open = bean.prepareContent(token, dataId, "bytes=" + (SIZE - 5) + '-', null);
        assertArrayEquals(Arrays.copyOfRange(content, SIZE - 5, SIZE), stream(bean, open));
    }

    @Test
    void testIfRangeAndUnsatisfiable() throws IOException {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final byte[] content = generateData(1000);
        final String dataId = upload(bean, token, "if-range", null, content);
        final String etag = bean.prepareContent(token, dataId, null, null).etag();

        final ContentDownload matching = bean.prepareContent(token, dataId, "bytes=0-9", etag);
        assertEquals(206, matching.status());
        final ContentDownload stale = bean.prepareContent(token, dataId, "bytes=0-9", "\"other\"");
        assertEquals(200, stale.status());
        assertArrayEquals(content, stream(bean, stale));

        final ContentDownload unsatisfiable = bean.prepareContent(token, dataId, "bytes=1000-", null);
        assertFalse(unsatisfiable.isOk());
        assertEquals(ReturnCode.RANGE_NOT_SATISFIABLE.getCode(), unsatisfiable.error().getReturnCode());
        assertEquals("bytes */1000", unsatisfiable.contentRange());
    }

    @Test
    void testEmptyContent() throws IOException {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, "empty", null, new byte[0]);

        final ContentDownload download = bean.prepareContent(token, dataId, null, null);
        assertEquals(0, download.contentLength());
        assertArrayEquals(new byte[0], stream(bean, download));
        assertFalse(bean.prepareContent(token, dataId, "bytes=0-", null).isOk());
    }

    @Test
    void testServingHeaders() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final ContentDownload video = bean.prepareContent(token, upload(bean, token, "clip.mp4", "video/mp4", generateData(10)), null, null);
        assertEquals("video/mp4", video.contentType());
        assertTrue(video.contentDisposition().startsWith("inline;"));

        final ContentDownload svg = bean.prepareContent(token, upload(bean, token, "image.svg", "image/svg+xml", generateData(10)), null, null);
        assertEquals("application/octet-stream", svg.contentType());
        assertTrue(svg.contentDisposition().startsWith("attachment;"));
    }

    @Test
    void testDownloadErrors() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, "errors", null, generateData(10));

        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), bean.prepareContent(null, dataId, null, null).error().getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), bean.prepareContent(token, "4c5bc2c6-0fa5-4c3a-9d1e-1a2b3c4d5e6f", null, null).error().getReturnCode());
        // A registered Data Object without content.
        final ProcessDataResponse registered = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "no content", 0));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), bean.prepareContent(token, registered.getDataId(), null, null).error().getReturnCode());
        // member3 has READ access in circle1, which is enough; member5 has none.
        assertTrue(bean.prepareContent(login(MEMBER_3), dataId, null, null).isOk());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), bean.prepareContent(login(MEMBER_5), dataId, null, null).error().getReturnCode());
    }

    /**
     * What can't be downloaded at all: without a Session token, a folder,
     * stored content shorter than a header, and the one-step request, which
     * downloads don't use.
     */
    @Test
    void testDownloadRefusals() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, "refusals", null, generateData(10));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), bean.prepareContent(new byte[0], dataId, null, null).error().getReturnCode());

        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setDataName("a folder to download");
        request.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse folder = bean.processData(request);
        assertTrue(folder.isOk(), folder.getReturnMessage());
        final ContentDownload ofFolder = bean.prepareContent(token, folder.getDataId(), null, null);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), ofFolder.error().getReturnCode());
        assertEquals("Folders have no content.", ofFolder.error().getReturnMessage());

        final long id = bean.prepareContent(token, dataId, null, null).dataId();
        final byte[] damaged = new byte[DataFormat.HEADER_BYTES - 1];
        new DataDao(entityManager).withConnection(connection -> EncryptedDataStore.write(connection, id, new ByteArrayInputStream(damaged), damaged.length, "SHA-512"));
        final ContentDownload ofDamaged = bean.prepareContent(token, dataId, null, null);
        assertEquals(ReturnCode.INTEGRITY_ERROR.getCode(), ofDamaged.error().getReturnCode());
        assertEquals("The stored content is damaged.", ofDamaged.error().getReturnMessage());

        final DownloadDataManager manager = new DownloadDataManager(settings, entityManager);
        final FetchDataRequest fetch = new FetchDataRequest();
        final EDSException oneStep = assertThrows(EDSException.class, () -> manager.perform(fetch));
        assertEquals(ReturnCode.ILLEGAL_SERVICE, oneStep.getReturnCode());
    }

    /**
     * A chunk which fails its check aborts the stream: the bytes of the
     * chunks before it may have been sent, but nothing of the tampered chunk
     * or after it, and the Data Object is marked as failed.
     */
    @Test
    void testTamperedChunkAbortsTheStream() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final byte[] content = generateData(SIZE);
        final String dataId = upload(bean, token, "tampered", null, content);
        final ContentDownload download = bean.prepareContent(token, dataId, null, null);

        // Flip one byte in the third chunk of the stored data.
        final DataDao dao = new DataDao(entityManager);
        final long id = download.dataId();
        final byte[] stored = dao.withConnection(connection -> EncryptedDataStore.read(connection, id, 0, (int) download.storedLength()));
        final int position = DataFormat.HEADER_BYTES + 2 * (CHUNK + DataFormat.TAG_BYTES) + 100;
        stored[position] ^= 1;
        dao.withConnection(connection -> EncryptedDataStore.write(connection, id, new ByteArrayInputStream(stored), stored.length, "SHA-512"));

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final ContentStreamer.ContentException cause = assertThrows(ContentStreamer.ContentException.class, () -> bean.streamContent(download, output));
        assertTrue(cause.isDamaged());
        assertTrue(output.size() <= 2 * CHUNK, "Only verified chunks were written: " + output.size());
        assertArrayEquals(Arrays.copyOf(content, output.size()), output.toByteArray());

        entityManager.clear();
        final DataEntity entity = entityManager.find(DataEntity.class, id);
        assertEquals(SanityStatus.FAILED, entity.getSanityStatus());
    }

    /**
     * A download prepared before an upload replaced the content is aborted,
     * not mixed with the new content.
     */
    @Test
    void testContentChangedAfterPrepare() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, "changed", null, generateData(1000));
        final ContentDownload download = bean.prepareContent(token, dataId, null, null);
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(1000)), 1000, null).isOk());

        final ContentStreamer.ContentException cause = assertThrows(ContentStreamer.ContentException.class, () -> bean.streamContent(download, new ByteArrayOutputStream()));
        assertFalse(cause.isDamaged());
        assertNotEquals(SanityStatus.FAILED, statusOf(download.dataId()));
    }

    /**
     * A client which goes away is no problem of the content, also when it is
     * first seen at the flush, as for content smaller than the buffers: the
     * Data Object is not marked as failed.
     */
    @Test
    void testClientGoneAtTheFlushIsNotDamage() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, "gone", null, generateData(1000));
        final ContentDownload download = bean.prepareContent(token, dataId, null, null);

        final ByteArrayOutputStream gone = new ByteArrayOutputStream() {
            @Override
            public void flush() throws IOException {
                throw new IOException("Broken pipe");
            }
        };
        assertThrows(ContentStreamer.ClientAbortException.class, () -> bean.streamContent(download, gone));
        assertNotEquals(SanityStatus.FAILED, statusOf(download.dataId()));
    }

    /**
     * A client which goes away while a large content is written to it is no
     * problem of the content either.
     */
    @Test
    void testClientGoneWhileWritingIsNotDamage() throws IOException {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, "left", null, generateData(SIZE));
        final ContentDownload download = bean.prepareContent(token, dataId, null, null);

        try (OutputStream gone = new OutputStream() {
            @Override
            public void write(final int value) throws IOException {
                throw new IOException("Connection reset by peer");
            }

            @Override
            public void write(final byte[] bytes, final int offset, final int length) throws IOException {
                throw new IOException("Connection reset by peer");
            }
        }) {
            assertThrows(ContentStreamer.ClientAbortException.class, () -> bean.streamContent(download, gone));
        }
        assertNotEquals(SanityStatus.FAILED, statusOf(download.dataId()));
    }

    /**
     * A database which gives up (lost connection, transaction timeout) tells
     * nothing about the content: that is not damage either.
     */
    @Test
    void testDatabaseFailureIsNotDamage() throws SQLException {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, "lost", null, generateData(1000));
        final ContentDownload download = bean.prepareContent(token, dataId, null, null);

        // Every call fails, except closing it, which the test does as the
        // stone does with a borrowed connection.
        try (Connection lost = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    throw new SQLException("The connection was lost.");
                })) {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            final ContentStreamer.ContentException cause = assertThrows(ContentStreamer.ContentException.class,
                    () -> ContentStreamer.stream(lost, download, output, 1024));
            assertFalse(cause.isDamaged());
        }
    }

    /**
     * A chunk which fails its check, while the database fails too, so it
     * can't be told whether the content was replaced meanwhile: that isn't
     * called damage, and the failure of the database is kept with it.
     */
    @Test
    void testFailedCheckWhileTheDatabaseFailsIsNotDamage() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, "failed check, lost database", null, generateData(SIZE));
        final ContentDownload download = bean.prepareContent(token, dataId, null, null);
        final DataDao dao = new DataDao(entityManager);
        final long id = download.dataId();

        // The third chunk fails its check. The stored bytes after the short
        // first part are one part, read before the first chunk goes out.
        final byte[] stored = dao.withConnection(connection -> EncryptedDataStore.read(connection, id, 0, (int) download.storedLength()));
        stored[DataFormat.HEADER_BYTES + 2 * (CHUNK + DataFormat.TAG_BYTES) + 100] ^= 1;
        dao.withConnection(connection -> EncryptedDataStore.write(connection, id, new ByteArrayInputStream(stored), stored.length, "SHA-512"));
        final AtomicBoolean lost = new AtomicBoolean(false);

        final UncheckedIOException thrown = assertThrows(UncheckedIOException.class, () -> dao.withConnection(connection -> {
            final Connection losing = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, arguments) -> {
                        if (lost.get() && method.getName().endsWith("Statement")) {
                            throw new SQLException("The connection was lost.");
                        }
                        try {
                            return method.invoke(connection, arguments);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
            final ByteArrayOutputStream output = new ByteArrayOutputStream() {
                @Override
                public synchronized void write(final byte[] bytes, final int offset, final int length) {
                    super.write(bytes, offset, length);
                    lost.set(true);
                }
            };
            try {
                ContentStreamer.stream(losing, download, output, CHUNK);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return null;
        }));
        final ContentStreamer.ContentException cause = assertInstanceOf(ContentStreamer.ContentException.class, thrown.getCause());
        assertFalse(cause.isDamaged());
        assertTrue(cause.getMessage().startsWith("The content changed while it was streamed"), cause.getMessage());
        final Throwable[] suppressed = cause.getCause().getSuppressed();
        assertEquals(1, suppressed.length);
        assertInstanceOf(SQLException.class, suppressed[0]);
    }

    /**
     * A database which is lost after the first chunk went out tells nothing
     * about the content either.
     */
    @Test
    void testDatabaseLostWhileStreamingIsNotDamage() {
        // Stored in parts of the smallest size, so later parts are read
        // after the first chunk went out.
        final net.haugr.eds.core.model.Settings small = newSettings();
        small.set(net.haugr.eds.core.enums.StandardSetting.STREAM_READ_BATCH_SIZE.getKey(), String.valueOf(net.haugr.eds.core.model.entities.DataPart.MIN_SIZE));
        final ShareBean bean = prepareShareBean(small);
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, "lost later", null, generateData(SIZE));
        final ContentDownload download = bean.prepareContent(token, dataId, null, null);
        final DataDao dao = new DataDao(entityManager);
        final AtomicBoolean lost = new AtomicBoolean(false);

        final UncheckedIOException thrown = assertThrows(UncheckedIOException.class, () -> dao.withConnection(connection -> {
            final Connection losing = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, arguments) -> {
                        if (lost.get() && method.getName().endsWith("Statement")) {
                            throw new SQLException("The connection was lost.");
                        }
                        try {
                            return method.invoke(connection, arguments);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
            final ByteArrayOutputStream output = new ByteArrayOutputStream() {
                @Override
                public synchronized void write(final byte[] bytes, final int offset, final int length) {
                    super.write(bytes, offset, length);
                    lost.set(true);
                }
            };
            try {
                ContentStreamer.stream(losing, download, output, CHUNK);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return null;
        }));
        final ContentStreamer.ContentException cause = assertInstanceOf(ContentStreamer.ContentException.class, thrown.getCause());
        assertFalse(cause.isDamaged());
        assertNotEquals(SanityStatus.FAILED, statusOf(download.dataId()));
    }

    /**
     * Without a snapshot (H2) content which is replaced while it is streamed
     * ends early or fails a tag. That is a change, not damage.
     */
    @Test
    void testContentChangedWhileStreamingIsNotDamage() {
        // Stored in parts of the smallest size, so the content is several of
        // them, and those after the first chunk are read after the change.
        final net.haugr.eds.core.model.Settings small = newSettings();
        small.set(net.haugr.eds.core.enums.StandardSetting.STREAM_READ_BATCH_SIZE.getKey(), String.valueOf(net.haugr.eds.core.model.entities.DataPart.MIN_SIZE));
        final ShareBean bean = prepareShareBean(small);
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, "replaced", null, generateData(SIZE));
        final ContentDownload download = bean.prepareContent(token, dataId, null, null);
        final DataDao dao = new DataDao(entityManager);
        final long id = download.dataId();
        final byte[] stored = dao.withConnection(connection -> EncryptedDataStore.read(connection, id, 0, (int) download.storedLength()));
        final byte[] shorter = Arrays.copyOf(stored, stored.length / 2);

        final UncheckedIOException thrown = assertThrows(UncheckedIOException.class, () -> dao.withConnection(connection -> {
            // The first chunk is on its way when the content is replaced.
            final ByteArrayOutputStream output = new ByteArrayOutputStream() {
                private boolean replaced = false;

                @Override
                public synchronized void write(final byte[] bytes, final int offset, final int length) {
                    super.write(bytes, offset, length);
                    if (!replaced) {
                        replaced = true;
                        replace(connection, id, shorter);
                    }
                }
            };
            try {
                ContentStreamer.stream(connection, download, output, CHUNK);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return null;
        }));
        final ContentStreamer.ContentException cause = assertInstanceOf(ContentStreamer.ContentException.class, thrown.getCause());
        assertFalse(cause.isDamaged());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private static void replace(final Connection connection, final long id, final byte[] stored) {
        try {
            EncryptedDataStore.write(connection, id, new ByteArrayInputStream(stored), stored.length, "SHA-512");
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private SanityStatus statusOf(final long id) {
        entityManager.clear();
        return entityManager.find(DataEntity.class, id).getSanityStatus();
    }

    private static byte[] stream(final ShareBean bean, final ContentDownload download) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        bean.streamContent(download, output);
        return output.toByteArray();
    }

    private String upload(final ShareBean bean, final byte[] token, final String name, final String mediaType, final byte[] content) {
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, name, 0);
        request.setMediaType(mediaType);
        final ProcessDataResponse registered = bean.processData(request);
        assertTrue(registered.isOk(), registered.getReturnMessage());
        final ProcessDataResponse uploaded = bean.uploadContent(token, registered.getDataId(), new ByteArrayInputStream(content), content.length, null);
        assertTrue(uploaded.isOk(), uploaded.getReturnMessage());
        return registered.getDataId();
    }

    private byte[] login(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        return prepareManagementBean().processMember(request).getSession();
    }
}
