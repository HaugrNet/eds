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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.Entry;
import net.haugr.eds.api.requests.EntryRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.EntryResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.managers.RotationManager;
import net.haugr.eds.core.managers.StatusManager;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.rotation.CircleRotations;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * <p>The stored bytes of each Circle (PLAN.md, Streaming, "The storage
 * ceiling: a total for each Circle"): after every path which adds or removes
 * parts, the total of each Circle is the sum of the parts of its objects;
 * the ceiling is checked against the totals, not the parts of other objects;
 * writes at the same time lose no change; and the inventory warns of a total
 * which differs.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class StoredBytesTest extends DatabaseSetup {

    private static final String BOARD = "board";
    private static final String DECLARATION = """
            {"feature": "board", "version": 1, "kinds": {
              "note":   {"add": "WRITE", "change": ["author"], "remove": ["author", "administrators"], "size": 1000},
              "vote":   {"add": "READ", "change": ["author"], "remove": ["author"], "about": "note", "keep": "member-about", "size": 100},
              "log":    {"add": "WRITE", "keep": "count:2", "size": 100}
            }}""";
    private static final byte[] IMAGE = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5 };

    /** A database of its own for the concurrent writes, as they commit. */
    private static final String OWN_URL = "jdbc:h2:mem:stored-bytes;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000";
    private static final EntityManagerFactory OWN = Persistence.createEntityManagerFactory("net.haugr.jpa",
            Map.of("jakarta.persistence.jdbc.url", OWN_URL));

    @AfterAll
    static void closeOwnDatabase() {
        OWN.close();
    }

    // =========================================================================
    // Each path which adds or removes parts
    // =========================================================================

    @Test
    void testInlineDataAddedAndReplaced() {
        final ShareBean bean = prepareShareBean();
        final long before = storedBytesOf(CIRCLE_1_ID);
        final ProcessDataResponse added = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "inline", 1_000));
        assertTrue(added.isOk(), added.getReturnMessage());
        assertTrue(storedBytesOf(CIRCLE_1_ID) > before + 1_000);
        assertTotalsMatchTheParts();

        final ProcessDataRequest update = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        update.setAction(Action.UPDATE);
        update.setDataId(added.getDataId());
        update.setData(generateData(300));
        assertTrue(bean.processData(update).isOk());
        assertTotalsMatchTheParts();
    }

    @Test
    void testStreamedUploadAndReplacement() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = register(bean, CIRCLE_1_ID, "streamed");

        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(70_000)), 70_000, null).isOk());
        assertTotalsMatchTheParts();
        // Replaced by shorter content, and by content of unknown length.
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(20_000)), 20_000, null).isOk());
        assertTotalsMatchTheParts();
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(90_000)), -1, null).isOk());
        assertTotalsMatchTheParts();
    }

    @Test
    void testCopyToAnotherCircle() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataResponse added = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "copied", 5_000));
        assertTrue(added.isOk(), added.getReturnMessage());
        final long target = storedBytesOf(CIRCLE_2_ID);

        final ProcessDataRequest copy = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        copy.setAction(Action.COPY);
        copy.setDataId(added.getDataId());
        copy.setTargetCircleId(CIRCLE_2_ID);
        assertTrue(bean.processData(copy).isOk());
        assertTrue(storedBytesOf(CIRCLE_2_ID) > target + 5_000);
        assertTotalsMatchTheParts();
    }

    @Test
    void testMoveToAnotherCircle() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final ProcessDataResponse added = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "moved", 5_000));
        assertTrue(added.isOk(), added.getReturnMessage());
        assertTrue(bean.storeThumbnail(token, added.getDataId(), "image/jpeg", IMAGE.clone()).isOk());
        final long source = storedBytesOf(CIRCLE_1_ID);
        final long target = storedBytesOf(CIRCLE_2_ID);

        final ProcessDataRequest move = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        move.setAction(Action.MOVE);
        move.setDataId(added.getDataId());
        move.setTargetCircleId(CIRCLE_2_ID);
        assertTrue(bean.processData(move).isOk());
        assertTrue(storedBytesOf(CIRCLE_1_ID) < source - 5_000);
        assertTrue(storedBytesOf(CIRCLE_2_ID) > target + 5_000);
        assertTotalsMatchTheParts();
    }

    @Test
    void testDeleteOfAnObject() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = register(bean, CIRCLE_1_ID, "deleted");
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(30_000)), 30_000, null).isOk());

        final ProcessDataRequest delete = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        delete.setAction(Action.DELETE);
        delete.setDataId(dataId);
        assertTrue(bean.processData(delete).isOk());
        assertTotalsMatchTheParts();
    }

    /**
     * The database deletes the content of a Data Object with it (ON DELETE
     * CASCADE); the DAO takes it from the total before, also for a caller
     * which didn't delete the content first.
     */
    @Test
    void testDeleteOfMetadataWithItsContent() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataResponse added = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "cascaded", 2_000));
        assertTrue(added.isOk(), added.getReturnMessage());
        // Without the content in the persistence context, which would save
        // the Metadata again via its cascade.
        entityManager.flush();
        entityManager.clear();
        final DataDao dataDao = new DataDao(entityManager);
        final MetadataEntity metadata = dataDao.find(MetadataEntity.class, added.getDataId());
        assertNotNull(metadata);

        dataDao.delete(metadata);
        entityManager.flush();
        assertEquals(0L, count("SELECT COUNT(*) FROM eds_metadata WHERE external_id = '" + added.getDataId() + '\''));
        assertTotalsMatchTheParts();
    }

    @Test
    void testThumbnails() {
        final ShareBean bean = prepareShareBean();
        final byte[] token = login(MEMBER_1);
        final String dataId = register(bean, CIRCLE_1_ID, "pictured");
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(10_000)), 10_000, null).isOk());

        // Stored, replaced, removed with new content, stored again, and
        // removed with its Data Object.
        assertTrue(bean.storeThumbnail(token, dataId, "image/jpeg", IMAGE.clone()).isOk());
        assertTotalsMatchTheParts();
        assertTrue(bean.storeThumbnail(token, dataId, "image/png", IMAGE.clone()).isOk());
        assertTotalsMatchTheParts();
        assertTrue(bean.uploadContent(token, dataId, new ByteArrayInputStream(generateData(5_000)), 5_000, null).isOk());
        assertTotalsMatchTheParts();
        assertTrue(bean.storeThumbnail(token, dataId, "image/webp", IMAGE.clone()).isOk());
        final ProcessDataRequest delete = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        delete.setAction(Action.DELETE);
        delete.setDataId(dataId);
        assertTrue(bean.processData(delete).isOk());
        assertTotalsMatchTheParts();
    }

    @Test
    void testEntries() {
        final FeatureBean features = features();
        final byte[] member1 = login(MEMBER_1);
        final byte[] member2 = login(MEMBER_2);

        final Entry note = added(features.addEntry(member1, CIRCLE_1_ID, BOARD, body("note", null, "a note")));
        assertTotalsMatchTheParts();
        // Changed, and a vote about it, replaced as a Member keeps one.
        assertTrue(features.changeEntry(member1, CIRCLE_1_ID, BOARD, note.getEntryId(), body(null, null, "a longer note than before")).isOk());
        assertTotalsMatchTheParts();
        added(features.addEntry(member2, CIRCLE_1_ID, BOARD, body("vote", note.getEntryId(), "yes")));
        added(features.addEntry(member2, CIRCLE_1_ID, BOARD, body("vote", note.getEntryId(), "no, rather not")));
        assertTotalsMatchTheParts();
        // Pruned beyond its count.
        for (int i = 0; i < 4; i++) {
            added(features.addEntry(member1, CIRCLE_1_ID, BOARD, body("log", null, "line " + i)));
        }
        assertTotalsMatchTheParts();
        // Removed, with the vote about it.
        assertTrue(features.removeEntry(member1, CIRCLE_1_ID, BOARD, note.getEntryId()).isOk());
        assertTotalsMatchTheParts();
    }

    /** The folder of a feature, with its entries as children. */
    @Test
    void testRemovalOfAFeatureWithItsEntries() {
        final FeatureBean features = features();
        final byte[] member1 = login(MEMBER_1);
        final Entry note = added(features.addEntry(member1, CIRCLE_1_ID, BOARD, body("note", null, "a note")));
        added(features.addEntry(member1, CIRCLE_1_ID, BOARD, body("vote", note.getEntryId(), "yes")));
        final long withEntries = storedBytesOf(CIRCLE_1_ID);

        assertTrue(features.removeCircleFeature(member1, CIRCLE_1_ID, BOARD).isOk());
        assertTrue(storedBytesOf(CIRCLE_1_ID) < withEntries);
        assertTotalsMatchTheParts();
    }

    @Test
    void testANewCircleHasItsRowOfStoredBytes() {
        // Without its row, a change of the stored bytes would be lost.
        final String circleId = createCircle("new, stored bytes");
        assertEquals(0L, storedBytesOf(circleId));
        assertTrue(prepareShareBean(newSettings()).processData(prepareAddDataRequest(MEMBER_1, circleId, "first", 3_000)).isOk());

        assertTrue(storedBytesOf(circleId) > 3_000);
        assertTotalsMatchTheParts();
    }

    @Test
    void testRotation() {
        final Settings settings = newSettings();
        final String circleId = createCircle("rotated, stored bytes");
        final ShareBean bean = prepareShareBean(settings);
        assertTrue(bean.processData(prepareAddDataRequest(MEMBER_1, circleId, "one", 3_000)).isOk());
        assertTrue(bean.processData(prepareAddDataRequest(MEMBER_1, circleId, "two", 70_000)).isOk());
        final long before = storedBytesOf(circleId);

        final ProcessCircleRequest rotate = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        rotate.setAction(Action.ROTATE);
        rotate.setCircleId(circleId);
        assertTrue(prepareManagementBean(settings).processCircle(rotate).isOk());
        final CircleRotations.Handoff handoff = CircleRotations.pending(circleOf(circleId).getId());
        assertNotNull(handoff);
        final RotationManager.Rotation rotation = new RotationManager(settings, entityManager, null).begin(handoff);
        assertNotNull(rotation);
        CircleRotations.finished(handoff);
        new RotationBean(entityManager, settings, null).rotate(new CircleRotations.Handoff(rotation.circleId(), rotation.externalId(),
                rotation.keyId(), rotation.current(), rotation.previousKeyId(), rotation.previous()));

        assertEquals(0L, count("SELECT COUNT(*) FROM eds_data d JOIN eds_keys k ON k.id = d.key_id WHERE k.id = " + rotation.previousKeyId()));
        // The rotation keeps the stored length.
        assertEquals(before, storedBytesOf(circleId));
        assertTotalsMatchTheParts();
    }

    @Test
    void testDeleteOfACircle() {
        final String circleId = createCircle("deleted, stored bytes");
        assertTrue(prepareShareBean().processData(prepareAddDataRequest(MEMBER_1, circleId, "gone with it", 1_000)).isOk());
        final ProcessCircleRequest delete = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        delete.setAction(Action.DELETE);
        delete.setCircleId(circleId);
        final ProcessCircleResponse response = prepareManagementBean().processCircle(delete);
        assertTrue(response.isOk(), response.getReturnMessage());
        assertTotalsMatchTheParts();
    }

    // =========================================================================
    // The ceiling, concurrent writes and the inventory
    // =========================================================================

    /**
     * The ceiling is checked against the totals of the Circles: parts of
     * another object, which no total counts (as if written behind EDS's
     * back), don't stop a write, as they did when every part was summed.
     */
    @Test
    void testTheCeilingDoesNotReadThePartsOfOtherObjects() {
        final Settings settings = newSettings();
        settings.set(StandardSetting.DATA_MAX_TOTAL_SIZE.getKey(), String.valueOf(storedBytesOfAll() + 1_000_000L));
        final ShareBean bean = prepareShareBean(settings);
        final ProcessDataResponse other = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "other", 100));
        assertTrue(other.isOk(), other.getReturnMessage());
        addUncountedParts(other.getDataId(), 2);

        final ProcessDataResponse added = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "written", 1_000));
        assertTrue(added.isOk(), added.getReturnMessage());
        final byte[] token = login(MEMBER_1);
        final ProcessDataResponse uploaded = bean.uploadContent(token, added.getDataId(), new ByteArrayInputStream(generateData(2_000)), 2_000, null);
        assertTrue(uploaded.isOk(), uploaded.getReturnMessage());
        assertTrue(bean.uploadContent(token, added.getDataId(), new ByteArrayInputStream(generateData(2_000)), -1, null).isOk());

        // The totals themselves are still counted.
        final ProcessDataResponse refused = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "too much", 1_100_000));
        assertEquals(ReturnCode.INSUFFICIENT_STORAGE.getCode(), refused.getReturnCode());
    }

    /**
     * Writes to one Circle at the same time, each in a transaction of its
     * own which commits: no change of the total is lost. They run on a
     * database of their own, as they commit.
     */
    @Test
    void testConcurrentWritesToOneCircleLoseNothing() throws Exception {
        final int writers = 8;
        final int writes = 6;
        final List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(writers)) {
            for (int writer = 0; writer < writers; writer++) {
                final int number = writer;
                results.add(executor.submit(() -> {
                    for (int i = 0; i < writes; i++) {
                        final String name = "concurrent " + number + '/' + i;
                        final int bytes = 1_000 + (number * 10) + i;
                        final ProcessDataResponse response = inOwnTransaction(em -> new ShareBean(em, settings)
                                .processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, name, bytes)));
                        assertTrue(response.isOk(), response.getReturnMessage());
                    }
                    return Boolean.TRUE;
                }));
            }
            for (final Future<Boolean> result : results) {
                assertEquals(Boolean.TRUE, result.get(120, TimeUnit.SECONDS));
            }
        }

        final List<String> differing = inOwnTransaction(StoredBytesTest::differingTotals);
        assertTrue(differing.isEmpty(), String.join("; ", differing));
        final long objects = inOwnTransaction(em -> ((Number) em.createNativeQuery("SELECT COUNT(*) FROM eds_metadata WHERE name LIKE 'concurrent %'")
                .getSingleResult()).longValue());
        assertEquals((long) writers * writes, objects);
    }

    @Test
    void testTheInventoryWarnsOfATotalWhichDiffers() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataResponse added = bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "counted", 100));
        assertTrue(added.isOk(), added.getReturnMessage());
        final StatusManager status = new StatusManager(settings, entityManager);
        final long circle = circleOf(CIRCLE_2_ID).getId();
        try (LogCapture log = LogCapture.of(StatusManager.class)) {
            status.inventory();
            assertTrue(log.warnings().stream().noneMatch(warning -> warning.contains("internal id " + circle + " are")), String.join("; ", log.warnings()));
        }

        final long stored = storedBytesOf(CIRCLE_2_ID);
        addUncountedParts(added.getDataId(), 1);
        try (LogCapture log = LogCapture.of(StatusManager.class)) {
            final Map<String, Double> inventory = status.inventory();
            assertTrue(log.warnings().contains("The stored bytes of the Circle with the internal id " + circle + " are " + stored
                    + ", but the parts of its objects have " + (stored + Integer.MAX_VALUE) + " bytes."), String.join("; ", log.warnings()));
            // The storage in use is the sum of the totals; nothing is corrected.
            assertEquals((double) storedBytesOfAll(), inventory.get("storageUsed"));
        }
        assertEquals(stored, storedBytesOf(CIRCLE_2_ID));
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    /** Every Circle's total is the sum of the parts of its objects. */
    private void assertTotalsMatchTheParts() {
        final List<String> differing = differingTotals(entityManager);
        assertTrue(differing.isEmpty(), String.join("; ", differing));
    }

    private static List<String> differingTotals(final EntityManager em) {
        // A Circle without its row of stored bytes shows -1.
        final List<?> rows = em.createNativeQuery("SELECT c.name, COALESCE(s.stored_bytes, -1), "
                + "(SELECT COALESCE(SUM(p.part_length), 0) FROM eds_metadata m JOIN eds_data d ON d.metadata_id = m.id "
                + "JOIN eds_data_parts p ON p.data_id = d.id WHERE m.circle_id = c.id) FROM eds_circles c "
                + "LEFT JOIN eds_circle_storage s ON s.circle_id = c.id").getResultList();
        final List<String> differing = new ArrayList<>();
        for (final Object row : rows) {
            final Object[] values = (Object[]) row;
            if (((Number) values[1]).longValue() != ((Number) values[2]).longValue()) {
                differing.add(values[0] + ": " + values[1] + " stored bytes, " + values[2] + " in parts");
            }
        }
        return differing;
    }

    private long storedBytesOf(final String circleId) {
        return ((Number) entityManager.createNativeQuery("SELECT s.stored_bytes FROM eds_circle_storage s JOIN eds_circles c ON c.id = s.circle_id WHERE c.external_id = ?")
                .setParameter(1, circleId).getSingleResult()).longValue();
    }

    private long storedBytesOfAll() {
        return count("SELECT COALESCE(SUM(stored_bytes), 0) FROM eds_circle_storage");
    }

    private long count(final String sql) {
        return ((Number) entityManager.createNativeQuery(sql).getSingleResult()).longValue();
    }

    /**
     * Adds parts to the content of a Data Object which no total counts, each
     * of the greatest length a part may claim; the bytes themselves are few,
     * as only part_length is summed.
     */
    private void addUncountedParts(final String dataId, final int parts) {
        final long id = count("SELECT d.id FROM eds_data d JOIN eds_metadata m ON m.id = d.metadata_id WHERE m.external_id = '" + dataId + '\'');
        for (int i = 1; i <= parts; i++) {
            entityManager.createNativeQuery("INSERT INTO eds_data_parts (data_id, part_offset, part_length, part) VALUES (?, ?, ?, ?)")
                    .setParameter(1, id)
                    .setParameter(2, i * 10_000_000_000L)
                    .setParameter(3, Integer.MAX_VALUE)
                    .setParameter(4, new byte[] { 0 })
                    .executeUpdate();
        }
    }

    private CircleEntity circleOf(final String circleId) {
        return dao.find(CircleEntity.class, circleId);
    }

    private String createCircle(final String name) {
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.CREATE);
        request.setCircleName(name);
        final ProcessCircleResponse response = prepareManagementBean().processCircle(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getCircleId();
    }

    private static String register(final ShareBean bean, final String circleId, final String name) {
        final ProcessDataResponse registered = bean.processData(prepareAddDataRequest(MEMBER_1, circleId, name, 0));
        assertTrue(registered.isOk(), registered.getReturnMessage());
        return registered.getDataId();
    }

    private byte[] login(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        final ProcessMemberResponse response = prepareManagementBean().processMember(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getSession();
    }

    /** The stone offers the board, and circle1 has it switched on. */
    private FeatureBean features() {
        final FeatureBean features = new FeatureBean(entityManager, settings);
        assertTrue(features.putFeature(login(Constants.ADMIN_ACCOUNT), BOARD, DECLARATION).isOk());
        assertTrue(features.setCircleFeature(login(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.ON).isOk());
        return features;
    }

    private static Entry added(final EntryResponse response) {
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getEntries().getFirst();
    }

    private static EntryRequest body(final String kind, final String about, final String text) {
        final EntryRequest body = new EntryRequest();
        body.setKind(kind);
        body.setAbout(about);
        body.setContent(Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
        return body;
    }

    private static <T> T inOwnTransaction(final Function<EntityManager, T> work) {
        try (EntityManager em = OWN.createEntityManager()) {
            em.getTransaction().begin();
            try {
                final T result = work.apply(em);
                em.getTransaction().commit();
                return result;
            } finally {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            }
        }
    }
}
