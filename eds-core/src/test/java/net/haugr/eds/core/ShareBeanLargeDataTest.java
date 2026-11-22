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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.ConnectionSource;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.stream.ContentDownload;
import org.junit.jupiter.api.Test;

/**
 * <p>The JSON data path for large content, and copy/move of streamed content
 * (PLAN.md, S4).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ShareBeanLargeDataTest extends DatabaseSetup {

    private static final int INLINE = 1000;
    /** Connections of their own to the test database, as EDS has them. */
    // The in-memory test database has H2's default account.
    @SuppressWarnings({"findsecbugs:HARD_CODE_PASSWORD", "findbugs:DMI_EMPTY_DB_PASSWORD"})
    private static final ConnectionSource READERS = () -> DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");

    @Test
    void testInlineLimitForAddAndUpdate() {
        final ShareBean bean = prepareShareBean(limited());

        final ProcessDataResponse tooLarge = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "too large", INLINE + 1));
        assertEquals(ReturnCode.CONTENT_TOO_LARGE.getCode(), tooLarge.getReturnCode());

        final ProcessDataResponse added = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "small", INLINE));
        assertTrue(added.isOk(), added.getReturnMessage());
        final ProcessDataRequest update = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        update.setAction(Action.UPDATE);
        update.setDataId(added.getDataId());
        update.setData(generateData(INLINE + 1));
        assertEquals(ReturnCode.CONTENT_TOO_LARGE.getCode(), bean.processData(update).getReturnCode());
    }

    @Test
    void testFetchDataRefusesLargeContent() {
        final ShareBean bean = prepareShareBean(limited());
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, CIRCLE_1_ID, "large", generateData(5_000));

        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_1);
        request.setDataId(dataId);
        final FetchDataResponse response = bean.fetchData(request);
        assertEquals(ReturnCode.CONTENT_TOO_LARGE.getCode(), response.getReturnCode());
        assertTrue(response.getReturnMessage().contains("5000 bytes"), response.getReturnMessage());
        assertEquals(1, response.getMetadata().size());
        assertEquals(dataId, response.getMetadata().getFirst().getDataId());
        assertNull(response.getData());
    }

    /**
     * The Metadata gives the size of the content (PLAN.md, L1): of a
     * small object, of a large one, and in a folder listing; a folder has
     * no size.
     */
    @Test
    void testMetadataHasContentSize() {
        final ShareBean bean = prepareShareBean(limited());
        final byte[] token = login(MEMBER_1);
        final String largeId = upload(bean, token, CIRCLE_1_ID, "sized large", generateData(5_000));
        final ProcessDataResponse small = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "sized small", 300));
        assertTrue(small.isOk(), small.getReturnMessage());

        final FetchDataRequest fetchSmall = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchSmall.setDataId(small.getDataId());
        assertEquals(300L, bean.fetchData(fetchSmall).getMetadata().getFirst().getSize());

        final FetchDataRequest fetchLarge = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchLarge.setDataId(largeId);
        assertEquals(5_000L, bean.fetchData(fetchLarge).getMetadata().getFirst().getSize());

        final FetchDataRequest list = prepareRequest(FetchDataRequest.class, MEMBER_1);
        list.setCircleId(CIRCLE_1_ID);
        final FetchDataResponse listed = bean.fetchData(list);
        assertTrue(listed.isOk(), listed.getReturnMessage());
        boolean foundSmall = false;
        boolean foundLarge = false;
        for (final net.haugr.eds.api.dtos.Metadata metadata : listed.getMetadata()) {
            if (metadata.getDataId().equals(small.getDataId())) {
                assertEquals(300L, metadata.getSize());
                foundSmall = true;
            } else if (metadata.getDataId().equals(largeId)) {
                assertEquals(5_000L, metadata.getSize());
                foundLarge = true;
            } else if ("folder".equals(metadata.getTypeName())) {
                assertNull(metadata.getSize());
            }
        }
        assertTrue(foundSmall && foundLarge);
    }

    /**
     * <p>Copy and move of streamed content to another Circle, re-encrypted
     * chunk by chunk. Above the (small) inline limit, the source is read on a
     * Connection of its own while the target is written, so it must be
     * committed first; the test removes its Data Objects again.</p>
     */
    @Test
    void testCopyAndMoveOfStreamedContent() throws IOException {
        final ShareBean bean = new ShareBean(entityManager, limited(), READERS);
        final byte[] token = login(MEMBER_1);
        final byte[] content = generateData(300_000);
        final List<String> created = new ArrayList<>();

        try {
            final String dataId = upload(bean, token, CIRCLE_1_ID, "to copy", content);
            created.add(dataId);
            commit();

            final ProcessDataRequest copy = prepareRequest(ProcessDataRequest.class, MEMBER_1);
            copy.setAction(Action.COPY);
            copy.setDataId(dataId);
            copy.setTargetCircleId(CIRCLE_2_ID);
            final ProcessDataResponse copied = bean.processData(copy);
            assertTrue(copied.isOk(), copied.getReturnMessage());
            created.add(copied.getDataId());
            assertArrayEquals(content, download(bean, token, copied.getDataId()));
            assertArrayEquals(content, download(bean, token, dataId));
            commit();

            final ProcessDataRequest move = prepareRequest(ProcessDataRequest.class, MEMBER_1);
            move.setAction(Action.MOVE);
            move.setDataId(copied.getDataId());
            move.setTargetCircleId(CIRCLE_1_ID);
            move.setDataName("moved back");
            final ProcessDataResponse moved = bean.processData(move);
            assertTrue(moved.isOk(), moved.getReturnMessage());
            created.add(moved.getDataId());
            assertArrayEquals(content, download(bean, token, moved.getDataId()));
            assertFalse(bean.prepareContent(token, copied.getDataId(), null, null).isOk());
            final FetchDataRequest gone = prepareRequest(FetchDataRequest.class, MEMBER_1);
            gone.setDataId(copied.getDataId());
            assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), bean.fetchData(gone).getReturnCode());
            commit();
        } finally {
            removeCommitted(bean, created);
        }
    }

    /**
     * Without Connections for reading the source, large content isn't copied
     * at all, rather than in memory.
     */
    @Test
    void testCopyOfLargeContentNeedsReaders() {
        final ShareBean bean = prepareShareBean(limited());
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, CIRCLE_1_ID, "too large to copy", generateData(5_000));

        final ProcessDataRequest copy = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        copy.setAction(Action.COPY);
        copy.setDataId(dataId);
        copy.setTargetCircleId(CIRCLE_2_ID);
        assertEquals(ReturnCode.ERROR.getCode(), bean.processData(copy).getReturnCode());
    }

    /**
     * The ceiling for all stored content (eds.data.max.total.size) applies to
     * uploads, the JSON data and copies; a replaced content doesn't count, and
     * a move adds nothing.
     */
    @Test
    void testStorageCeiling() {
        final Settings settings = newSettings();
        final ShareBean bean = prepareShareBean(settings);
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, CIRCLE_1_ID, "below the ceiling", generateData(20_000));
        final long used = storedBytes();

        // Room for about 10.000 more bytes.
        settings.set(StandardSetting.DATA_MAX_TOTAL_SIZE.getKey(), String.valueOf(used + 10_000));

        final ProcessDataResponse registered = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "above the ceiling", 0));
        assertTrue(registered.isOk(), registered.getReturnMessage());
        final byte[] large = generateData(15_000);
        final ProcessDataResponse refused = bean.uploadContent(token, registered.getDataId(), new ByteArrayInputStream(large), large.length, null);
        assertEquals(ReturnCode.INSUFFICIENT_STORAGE.getCode(), refused.getReturnCode());
        assertEquals(507, ReturnCode.INSUFFICIENT_STORAGE.getHttpCode());
        // Without a length (a pipe), it is found while the content is read ...
        assertEquals(ReturnCode.INSUFFICIENT_STORAGE.getCode(), bean.uploadContent(token, registered.getDataId(), new ByteArrayInputStream(large), -1, null).getReturnCode());
        // ... or afterwards: 9.990 bytes are below the room, but not once
        // stored, with the header and the tag.
        assertEquals(ReturnCode.INSUFFICIENT_STORAGE.getCode(), bean.uploadContent(token, registered.getDataId(), new ByteArrayInputStream(generateData(9_990)), -1, null).getReturnCode());
        assertTrue(bean.uploadContent(token, registered.getDataId(), new ByteArrayInputStream(generateData(5_000)), -1, null).isOk());
        assertTrue(bean.uploadContent(token, registered.getDataId(), new ByteArrayInputStream(new byte[0]), 0, null).isOk());
        assertEquals(ReturnCode.INSUFFICIENT_STORAGE.getCode(), bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "inline above", 15_000)).getReturnCode());

        // Replacing the 20.000 bytes with 25.000 adds only about 5.000.
        final byte[] replacement = generateData(25_000);
        final ProcessDataResponse replaced = bean.uploadContent(token, dataId, new ByteArrayInputStream(replacement), replacement.length, null);
        assertTrue(replaced.isOk(), replaced.getReturnMessage());

        final ProcessDataRequest copy = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        copy.setAction(Action.COPY);
        copy.setDataId(dataId);
        copy.setTargetCircleId(CIRCLE_2_ID);
        assertEquals(ReturnCode.INSUFFICIENT_STORAGE.getCode(), bean.processData(copy).getReturnCode());

        final ProcessDataRequest move = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        move.setAction(Action.MOVE);
        move.setDataId(dataId);
        move.setTargetCircleId(CIRCLE_2_ID);
        final ProcessDataResponse moved = bean.processData(move);
        assertTrue(moved.isOk(), moved.getReturnMessage());
    }

    @Test
    void testDeleteStreamedContent() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = upload(bean, token, CIRCLE_1_ID, "deleted", generateData(200_000));

        final ProcessDataRequest delete = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        delete.setAction(Action.DELETE);
        delete.setDataId(dataId);
        assertTrue(bean.processData(delete).isOk());
        assertEquals(0L, (Long) entityManager.createQuery("select count(d) from DataEntity d where d.metadata.externalId = :id").setParameter("id", dataId).getSingleResult());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private void commit() {
        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();
    }

    private void removeCommitted(final ShareBean bean, final List<String> dataIds) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.clear();
        entityManager.getTransaction().begin();
        for (final String dataId : dataIds) {
            final ProcessDataRequest delete = prepareRequest(ProcessDataRequest.class, MEMBER_1);
            delete.setAction(Action.DELETE);
            delete.setDataId(dataId);
            // Moved objects are already gone.
            bean.processData(delete);
        }
        commit();
    }

    private long storedBytes() {
        return ((Number) entityManager.createNativeQuery("SELECT COALESCE(SUM(part_length), 0) FROM eds_data_parts").getSingleResult()).longValue();
    }

    private Settings limited() {
        final Settings settings = newSettings();
        settings.set(StandardSetting.DATA_MAX_INLINE_SIZE.getKey(), String.valueOf(INLINE));
        return settings;
    }

    private static byte[] download(final ShareBean bean, final byte[] token, final String dataId) throws IOException {
        final ContentDownload download = bean.prepareContent(token, dataId, null, null);
        assertTrue(download.isOk(), download.isOk() ? "" : download.error().getReturnMessage());
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        bean.streamContent(download, output);
        return output.toByteArray();
    }

    private String upload(final ShareBean bean, final byte[] token, final String circleId, final String name, final byte[] content) {
        final ProcessDataResponse registered = bean.processData(prepareAddDataRequest(MEMBER_1, circleId, name, 0));
        assertTrue(registered.isOk(), registered.getReturnMessage());
        assertTrue(bean.uploadContent(token, registered.getDataId(), new ByteArrayInputStream(content), content.length, null).isOk());
        return registered.getDataId();
    }

    private byte[] login(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        return prepareManagementBean().processMember(request).getSession();
    }
}
