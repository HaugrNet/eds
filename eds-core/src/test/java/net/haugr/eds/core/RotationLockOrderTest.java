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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.api.requests.EntryRequest;
import net.haugr.eds.api.responses.EntryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import javax.sql.DataSource;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.managers.RotationManager;
import net.haugr.eds.core.model.ConnectionSource;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.rotation.CircleRotations;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * <p>The order in which a delete locks the rows it deletes, against the
 * rotation of the Circle Secret, which runs beside the requests: it holds a
 * content row while it replaces the parts of it, and the Circle while it
 * ends. A delete which took the parts or the Trustees before it waited for
 * these rows would wait for the rotation while the rotation waits for it,
 * as the load test found. And a copy or move, which reads the content
 * and the secret it is on, against the rotation which replaces both.</p>
 *
 * <p>The tests run on a database of their own, as they commit: one
 * connection holds the row as the rotation does, while the delete runs in a
 * thread of its own; once the delete waits, the holder goes on with what
 * the rotation does next, which mustn't wait for the delete.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class RotationLockOrderTest extends DatabaseSetup {

    private static final String URL = "jdbc:h2:mem:delete-locks;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000";
    private static final EntityManagerFactory OWN = Persistence.createEntityManagerFactory("net.haugr.jpa",
            Map.of("jakarta.persistence.jdbc.url", URL));
    // How long the holder waits for a row, before it counts as a deadlock.
    private static final int HOLDER_TIMEOUT_MILLIS = 3000;
    private static final DataSource DATABASE = database();

    private static DataSource database() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(URL);
        dataSource.setUser("sa");
        // The rotations offered are kept by the Circle's internal ID, in the
        // JVM (CircleRotations), which the tests of the shared database use
        // as well; the Circles of this database get IDs of their own, so one
        // of them is never taken for a Circle there (ticket 24).
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE eds_circles ALTER COLUMN id RESTART WITH 1000001");
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return dataSource;
    }

    @Test
    void testAWriteOfAnEntryUnwrapsTheCircleSecretBeforeItWaitsForTheCircle() throws Exception {
        // Ticket 28: the unwrap of the Circle Secret (ECDH and ML-KEM) is the
        // longest part of a write; under the lock of the Circle, every other
        // writer of the Circle waited for it.
        final String circleId = createCircle("entries written early");
        final long circle = circleIdOf(circleId);
        final String feature = "early";
        final String declaration = "{\"feature\": \"early\", \"version\": 1, \"kinds\": {\"note\": {\"add\": \"WRITE\", \"size\": 100}}}";
        assertTrue(inOwnTransaction(em -> new FeatureBean(em, settings).putFeature(sessionIn(em, Constants.ADMIN_ACCOUNT), feature, declaration)).isOk());
        assertTrue(inOwnTransaction(em -> new FeatureBean(em, settings).setCircleFeature(sessionIn(em, MEMBER_1), circleId, feature, FeatureState.ON)).isOk());
        final byte[] token = inOwnTransaction(em -> sessionIn(em, MEMBER_1));

        // With the rotation due, each unwrap of the Circle Secret is offered
        // to the rotation, which shows when it happens.
        try (Connection connection = DATABASE.getConnection()) {
            update(connection, "UPDATE eds_circles SET rotate_circle_key_due = DATEADD('DAY', -1, CURRENT_TIMESTAMP) WHERE id = ?", circle);
        }
        final List<CircleRotations.Handoff> offered = new CopyOnWriteArrayList<>();
        CircleRotations.install(offered::add);
        try (Connection holder = holder()) {
            lock(holder, "SELECT id FROM eds_circles WHERE id = ? FOR UPDATE", circle);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<EntryResponse> added = executor.submit(() -> inOwnTransaction(em ->
                        new FeatureBean(em, settings).addEntry(token, circleId, feature, note("written while the Circle was locked"))));
                awaitBlocked(1, added);
                assertFalse(added.isDone(), "The write didn't wait for the Circle");
                assertEquals(1, offered.size(), "The Circle Secret wasn't unwrapped before the write waited for the Circle");

                holder.commit();
                final EntryResponse response = added.get(30, TimeUnit.SECONDS);
                assertTrue(response.isOk(), response.getReturnMessage());
            }
        } finally {
            CircleRotations.install(null);
            CircleRotations.forget(circle);
        }
    }

    private byte[] sessionIn(final EntityManager em, final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        final ProcessMemberResponse response = prepareManagementBean(em, settings).processMember(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getSession();
    }

    private static EntryRequest note(final String text) {
        final EntryRequest body = new EntryRequest();
        body.setKind("note");
        body.setContent(Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
        return body;
    }

    @AfterAll
    static void closeOwnDatabase() {
        OWN.close();
    }

    @Test
    void testDeletingACircleWaitsForTheContentRowTheRotationHolds() throws Exception {
        final String circleId = createCircleWithContent("held content");
        final long circle = circleIdOf(circleId);
        final long dataId = dataRowOf(circle);
        assertTrue(circle > 1_000_000, "The Circles of this database have IDs of their own: " + circle);

        try (Connection holder = holder()) {
            lock(holder, "SELECT key_id FROM eds_data WHERE id = ? FOR UPDATE", dataId);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<ProcessCircleResponse> deleted = executor.submit(() -> inOwnTransaction(em -> deleteCircle(em, circleId)));
                awaitBlocked(1, deleted);

                // The rotation replaces the parts of the row it holds.
                assertDoesNotThrow(() -> replaceParts(holder, dataId));
                holder.commit();

                final ProcessCircleResponse response = deleted.get(30, TimeUnit.SECONDS);
                assertTrue(response.isOk(), response.getReturnMessage());
            }
        }
        assertEquals(0L, single("SELECT COUNT(*) FROM eds_circles WHERE id = ?", circle));
        assertEquals(0L, single("SELECT COUNT(*) FROM eds_data WHERE id = ?", dataId));
    }

    @Test
    void testDeletingACircleWaitsForTheEndOfTheRotation() throws Exception {
        final String circleId = createCircleWithContent("ending");
        final long circle = circleIdOf(circleId);

        try (Connection holder = holder()) {
            lock(holder, "SELECT id FROM eds_circles WHERE id = ? FOR UPDATE", circle);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<ProcessCircleResponse> deleted = executor.submit(() -> inOwnTransaction(em -> deleteCircle(em, circleId)));
                awaitBlocked(1, deleted);

                // The end of the rotation changes the Trustees of the Circle.
                assertDoesNotThrow(() -> update(holder, "UPDATE eds_trustees SET previous_key_id = NULL WHERE circle_id = ?", circle));
                holder.commit();

                final ProcessCircleResponse response = deleted.get(30, TimeUnit.SECONDS);
                assertTrue(response.isOk(), response.getReturnMessage());
            }
        }
        assertEquals(0L, single("SELECT COUNT(*) FROM eds_circles WHERE id = ?", circle));
    }

    @Test
    void testDeletingTheContentWaitsForTheRowTheRotationHolds() throws Exception {
        final String circleId = createCircleWithContent("one object");
        final long circle = circleIdOf(circleId);
        final long dataId = dataRowOf(circle);
        final long metadataId = single("SELECT metadata_id FROM eds_data WHERE id = ?", dataId);

        try (Connection holder = holder()) {
            lock(holder, "SELECT key_id FROM eds_data WHERE id = ? FOR UPDATE", dataId);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<?> deleted = executor.submit(() -> inOwnTransaction(em -> {
                    new DataDao(em).deleteContent(em.find(MetadataEntity.class, metadataId));
                    return null;
                }));
                awaitBlocked(1, deleted);

                assertDoesNotThrow(() -> replaceParts(holder, dataId));
                holder.commit();
                deleted.get(30, TimeUnit.SECONDS);
            }
        }
        assertEquals(0L, single("SELECT COUNT(*) FROM eds_data WHERE id = ?", dataId));
        assertEquals(0L, single("SELECT COUNT(*) FROM eds_data_parts WHERE data_id = ?", dataId));
    }

    @Test
    void testAWriteToAnObjectDoesNotWaitForTheRowOfItsCircle() throws Exception {
        // The delete of a Circle locks its row, and then the rows of its
        // content; a write holds the rows of its object, and changes the
        // stored bytes of the Circle at its end. Those are a row of their own
        // (eds_circle_storage), so the write mustn't wait for the Circle's
        // row, or the two would deadlock (ticket 23).
        final String circleId = createCircleWithContent("written while the Circle goes");
        final long circle = circleIdOf(circleId);
        final long dataId = dataRowOf(circle);
        final long metadataId = single("SELECT metadata_id FROM eds_data WHERE id = ?", dataId);
        assertTrue(single("SELECT stored_bytes FROM eds_circle_storage WHERE circle_id = ?", circle) > 0);

        try (Connection holder = holder()) {
            lock(holder, "SELECT id FROM eds_circles WHERE id = ? FOR UPDATE", circle);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<?> deleted = executor.submit(() -> inOwnTransaction(em -> {
                    new DataDao(em).deleteContent(em.find(MetadataEntity.class, metadataId));
                    return null;
                }));
                assertDoesNotThrow(() -> deleted.get(HOLDER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
            }
            holder.rollback();
        }
        assertEquals(0L, single("SELECT stored_bytes FROM eds_circle_storage WHERE circle_id = ?", circle));
    }

    @Test
    void testTheRotationLeavesACircleAloneWhichWentWhileItWaited() throws Exception {
        final String circleId = createCircleWithContent("gone meanwhile");
        final long circle = circleIdOf(circleId);
        // With its real secret as the retiring one, so the end comes to the lock.
        final long key = single("SELECT MIN(id) FROM eds_keys WHERE circle_id = ?", circle);
        final RotationManager.Rotation rotation = new RotationManager.Rotation(circle, circleId, key, null, key, null, null);

        try (Connection holder = holder()) {
            // A delete of the Circle, which hasn't committed yet.
            update(holder, "DELETE FROM eds_data WHERE metadata_id IN (SELECT id FROM eds_metadata WHERE circle_id = ?)", circle);
            update(holder, "DELETE FROM eds_trustees WHERE circle_id = ?", circle);
            update(holder, "DELETE FROM eds_circles WHERE id = ?", circle);
            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                final Future<Long> resealed = executor.submit(() -> inOwnTransaction(em ->
                        new RotationManager(settings, em, null).resealEntries(rotation, 0, 10)));
                final Future<RotationManager.Outcome> ended = executor.submit(() -> inOwnTransaction(em ->
                        new RotationManager(settings, em, null).end(rotation)));
                // Either step may finish before it waits, if it fails.
                awaitBlocked(2, resealed, ended);
                holder.commit();

                assertEquals(-1L, resealed.get(30, TimeUnit.SECONDS));
                assertEquals(RotationManager.Outcome.DONE, ended.get(30, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void testAMoveReadsTheContentWithTheSecretItIsOn() throws Exception {
        CircleRotations.install(null);
        final String from = createCircleWithContent("moved from");
        final String to = createCircle("moved to");
        final long circle = circleIdOf(from);
        final long dataId = dataRowOf(circle);
        final String objectId = objectOf(dataId);
        final byte[] content = read(objectId);
        final RotationManager.Rotation rotation = beginRotation(from, circle, settings);

        // The move copies through a reader, as large content; the reader
        // waits until the rotation has re-encrypted the content, or waits
        // for the move itself.
        final Settings streamed = newSettings();
        streamed.set(StandardSetting.DATA_MAX_INLINE_SIZE.getKey(), "10");
        final CountDownLatch opened = new CountDownLatch(1);
        final CountDownLatch proceed = new CountDownLatch(1);
        final ConnectionSource readers = () -> {
            opened.countDown();
            try {
                assertTrue(proceed.await(30, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted.", e);
            }
            return DATABASE.getConnection();
        };
        final ProcessDataRequest move = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        move.setAction(Action.MOVE);
        move.setDataId(objectId);
        move.setTargetCircleId(to);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            final Future<ProcessDataResponse> moved = executor.submit(() -> inOwnTransaction(em -> new ShareBean(em, streamed, readers).processData(move)));
            assertTrue(opened.await(20, TimeUnit.SECONDS));
            final Future<Boolean> reencrypted = executor.submit(() -> inOwnTransaction(em -> new RotationManager(settings, em, null).reencrypt(rotation, dataId)));
            awaitBlocked(1, reencrypted);
            proceed.countDown();

            final ProcessDataResponse response = moved.get(30, TimeUnit.SECONDS);
            assertTrue(response.isOk(), response.getReturnMessage());
            // The rotation finds the content gone with the move.
            assertEquals(Boolean.FALSE, reencrypted.get(30, TimeUnit.SECONDS));
            assertArrayEquals(content, read(response.getDataId()));
        }
    }

    @Test
    void testDeletingAMemberLocksTheMemberBeforeItsCircles() throws Exception {
        // Ticket 1: the rotation of the MasterKey locks the Members, then the
        // Circles; the deletion of a Member must take them in the same order,
        // or the two wait for each other.
        final String circleId = createCircle("left by a member");
        final long circle = circleIdOf(circleId);
        final String leavingId = memberIn(circleId, "leaving member");
        final long member = single("SELECT id FROM eds_members WHERE external_id = ?", leavingId);

        try (Connection holder = holder()) {
            // As the rotation of the MasterKey: the Member first.
            lock(holder, "SELECT id FROM eds_members WHERE id = ? FOR UPDATE", member);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<ProcessMemberResponse> deleted = executor.submit(() -> inOwnTransaction(em -> deleteMember(em, leavingId)));
                awaitBlocked(1, deleted);

                // The deletion waits for the Member, and holds none of its Circles.
                assertDoesNotThrow(() -> lock(holder, "SELECT id FROM eds_circles WHERE id = ? FOR UPDATE", circle));
                holder.commit();

                final ProcessMemberResponse response = deleted.get(30, TimeUnit.SECONDS);
                assertTrue(response.isOk(), response.getReturnMessage());
            }
        }
    }

    @Test
    void testAMemberWhoBecameTheLastTrusteeMeanwhileIsNotDeleted() throws Exception {
        // The review of ticket 1: the other Trustee leaves while the deletion
        // waits for the Circle; the deletion checks again under the lock.
        final String circleId = createCircle("its last trustee");
        final long circle = circleIdOf(circleId);
        final String leavingId = memberIn(circleId, "last one left");

        try (Connection holder = holder()) {
            lock(holder, "SELECT id FROM eds_circles WHERE id = ? FOR UPDATE", circle);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<ProcessMemberResponse> deleted = executor.submit(() -> inOwnTransaction(em -> deleteMember(em, leavingId)));
                awaitBlocked(1, deleted);

                // member1 leaves the Circle meanwhile.
                update(holder, "DELETE FROM eds_trustees WHERE circle_id = ? AND member_id = (SELECT id FROM eds_members WHERE name = '" + MEMBER_1 + "')", circle);
                holder.commit();

                final ProcessMemberResponse response = deleted.get(30, TimeUnit.SECONDS);
                assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode(), response.getReturnMessage());
            }
        }
        assertEquals(1L, single("SELECT COUNT(*) FROM eds_trustees WHERE circle_id = ?", circle));
    }

    @Test
    void testAMemberDeletedMeanwhileIsToldAsGone() throws Exception {
        final String circleId = createCircle("member deleted twice");
        final String leavingId = memberIn(circleId, "deleted twice");
        final long member = single("SELECT id FROM eds_members WHERE external_id = ?", leavingId);

        try (Connection holder = holder()) {
            lock(holder, "SELECT id FROM eds_members WHERE id = ? FOR UPDATE", member);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<ProcessMemberResponse> second = executor.submit(() -> inOwnTransaction(em -> deleteMember(em, leavingId)));
                awaitBlocked(1, second);

                // The first deletion.
                update(holder, "DELETE FROM eds_members WHERE id = ?", member);
                holder.commit();

                final ProcessMemberResponse response = second.get(30, TimeUnit.SECONDS);
                assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode(), response.getReturnMessage());
            }
        }
    }

    @Test
    void testACircleDeletedWhileAMemberIsDeletedIsPassedOver() throws Exception {
        final String circleId = createCircle("gone while a member goes");
        final long circle = circleIdOf(circleId);
        final String leavingId = memberIn(circleId, "goes with a circle");

        try (Connection holder = holder()) {
            lock(holder, "SELECT id FROM eds_circles WHERE id = ? FOR UPDATE", circle);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<ProcessMemberResponse> deleted = executor.submit(() -> inOwnTransaction(em -> deleteMember(em, leavingId)));
                awaitBlocked(1, deleted);

                // The Circle is deleted meanwhile.
                update(holder, "DELETE FROM eds_trustees WHERE circle_id = ?", circle);
                update(holder, "DELETE FROM eds_metadata WHERE circle_id = ?", circle);
                update(holder, "DELETE FROM eds_circles WHERE id = ?", circle);
                holder.commit();

                final ProcessMemberResponse response = deleted.get(30, TimeUnit.SECONDS);
                assertTrue(response.isOk(), response.getReturnMessage());
            }
        }
        assertEquals(0L, single("SELECT COUNT(*) FROM eds_members WHERE external_id = ?", leavingId));
    }

    /** A new account, a Trustee with WRITE in the Circle beside member1; its ID. */
    private String memberIn(final String circleId, final String name) {
        final String memberId = inOwnTransaction(em -> {
            final ProcessMemberRequest create = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
            create.setAction(Action.CREATE);
            create.setNewAccountName(name);
            create.setNewCredential(credentialOf(name));
            final ProcessMemberResponse created = prepareManagementBean(em, settings).processMember(create);
            assertTrue(created.isOk(), created.getReturnMessage());
            return created.getMemberId();
        });
        assertTrue(inOwnTransaction(em -> {
            final ProcessTrusteeRequest add = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
            add.setAction(Action.ADD);
            add.setCircleId(circleId);
            add.setMemberId(memberId);
            add.setTrustLevel(TrustLevel.WRITE);
            return prepareManagementBean(em, settings).processTrustee(add);
        }).isOk());
        return memberId;
    }

    private ProcessMemberResponse deleteMember(final EntityManager em, final String memberId) {
        final ProcessMemberRequest delete = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        delete.setAction(Action.DELETE);
        delete.setMemberId(memberId);
        return prepareManagementBean(em, settings).processMember(delete);
    }

    @Test
    void testASecondDeleteOfACircleIsToldItIsGone() throws Exception {
        // Ticket 2: a delete which waited for another one finds the Circle gone.
        final String circleId = createCircle("deleted twice");
        final long circle = circleIdOf(circleId);

        try (Connection holder = holder()) {
            lock(holder, "SELECT id FROM eds_circles WHERE id = ? FOR UPDATE", circle);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<ProcessCircleResponse> second = executor.submit(() -> inOwnTransaction(em -> deleteCircle(em, circleId)));
                awaitBlocked(1, second);

                // The first delete, which held the Circle.
                update(holder, "DELETE FROM eds_trustees WHERE circle_id = ?", circle);
                update(holder, "DELETE FROM eds_metadata WHERE circle_id = ?", circle);
                update(holder, "DELETE FROM eds_circles WHERE id = ?", circle);
                holder.commit();

                final ProcessCircleResponse response = second.get(30, TimeUnit.SECONDS);
                assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode(), response.getReturnMessage());
            }
        }
    }

    @Test
    void testACopyOfASourceReplacedMeanwhileCopiesTheNewContent() throws Exception {
        // Ticket 3: an update replaces the content row while the copy waits for it.
        final String from = createCircleWithContent("copied from");
        final String to = createCircle("copied to");
        final long dataId = dataRowOf(circleIdOf(from));
        final String objectId = objectOf(dataId);
        final byte[] content = read(objectId);
        final ProcessDataRequest copy = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        copy.setAction(Action.COPY);
        copy.setDataId(objectId);
        copy.setTargetCircleId(to);

        try (Connection holder = holder()) {
            lock(holder, "SELECT key_id FROM eds_data WHERE id = ? FOR UPDATE", dataId);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<ProcessDataResponse> copied = executor.submit(() -> inOwnTransaction(em -> prepareShareBean(em, settings).processData(copy)));
                awaitBlocked(1, copied);

                // The update: a new content row of the same Data Object, the old one gone.
                final long replacement;
                try (PreparedStatement insert = holder.prepareStatement("INSERT INTO eds_data (metadata_id, key_id, checksum, sanity_status, sanity_checked, altered, added) "
                        + "SELECT metadata_id, key_id, checksum, sanity_status, sanity_checked, altered, added FROM eds_data WHERE id = ?", new String[] { "id" })) {
                    insert.setLong(1, dataId);
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) {
                            fail("No key of the new content row.");
                        }
                        replacement = keys.getLong(1);
                    }
                }
                try (PreparedStatement parts = holder.prepareStatement("INSERT INTO eds_data_parts (data_id, part_offset, part_length, part) "
                        + "SELECT ?, part_offset, part_length, part FROM eds_data_parts WHERE data_id = ?")) {
                    parts.setLong(1, replacement);
                    parts.setLong(2, dataId);
                    parts.executeUpdate();
                }
                update(holder, "DELETE FROM eds_data WHERE id = ?", dataId);
                holder.commit();

                final ProcessDataResponse response = copied.get(30, TimeUnit.SECONDS);
                assertTrue(response.isOk(), response.getReturnMessage());
                assertArrayEquals(content, read(response.getDataId()));
            }
        }
    }

    @Test
    void testAMoveTakesTheRowsOfTheObjectInTheOrderOfTheirIds() throws Exception {
        checkAgainstTheDeleteOfTheCircle("moved, thumbnail after", false, this::move);
        checkAgainstTheDeleteOfTheCircle("moved, thumbnail before", true, this::move);
    }

    @Test
    void testAnUpdateTakesTheRowsOfTheObjectInTheOrderOfTheirIds() throws Exception {
        checkAgainstTheDeleteOfTheCircle("updated, thumbnail after", false, this::update);
        checkAgainstTheDeleteOfTheCircle("updated, thumbnail before", true, this::update);
    }

    @Test
    void testADeleteTakesTheRowsOfTheObjectInTheOrderOfTheirIds() throws Exception {
        checkAgainstTheDeleteOfTheCircle("deleted, thumbnail after", false, this::delete);
        checkAgainstTheDeleteOfTheCircle("deleted, thumbnail before", true, this::delete);
    }

    @Test
    void testAnUploadTakesTheRowsOfTheObjectInTheOrderOfTheirIds() throws Exception {
        checkAgainstTheDeleteOfTheCircle("uploaded, thumbnail after", false, this::upload);
        checkAgainstTheDeleteOfTheCircle("uploaded, thumbnail before", true, this::upload);
    }

    private Function<EntityManager, ProcessDataResponse> move(final String objectId) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        request.setAction(Action.MOVE);
        request.setDataId(objectId);
        request.setTargetCircleId(createCircle("moved to, " + objectId));
        return processed(request);
    }

    private Function<EntityManager, ProcessDataResponse> update(final String objectId) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setDataId(objectId);
        request.setData(new byte[] { 1, 2, 3 });
        return processed(request);
    }

    private Function<EntityManager, ProcessDataResponse> delete(final String objectId) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        request.setAction(Action.DELETE);
        request.setDataId(objectId);
        return processed(request);
    }

    private Function<EntityManager, ProcessDataResponse> upload(final String objectId) {
        final ProcessMemberRequest login = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        login.setAction(Action.LOGIN);
        final ProcessMemberResponse session = inOwnTransaction(em -> prepareManagementBean(em, settings).processMember(login));
        assertTrue(session.isOk(), session.getReturnMessage());
        return em -> prepareShareBean(em, settings).uploadContent(session.getSession(), objectId, new ByteArrayInputStream(new byte[] { 1, 2, 3 }), 3, null);
    }

    private Function<EntityManager, ProcessDataResponse> processed(final ProcessDataRequest request) {
        return em -> prepareShareBean(em, settings).processData(request);
    }

    /**
     * The delete of the Circle takes the rows in the order of their IDs; a
     * request which replaces, moves or deletes a Data Object with a thumbnail
     * mustn't hold the one it takes second while it waits for the first.
     */
    private void checkAgainstTheDeleteOfTheCircle(final String name, final boolean thumbnailFirst,
                                                  final Function<String, Function<EntityManager, ProcessDataResponse>> requestOf) throws Exception {
        final String from = createCircleWithContent(name);
        final long dataId = dataRowOf(circleIdOf(from));
        final long thumbnail = addThumbnail(dataId, thumbnailFirst);
        final long first = thumbnailFirst ? thumbnail : dataId;
        final long second = thumbnailFirst ? dataId : thumbnail;
        final Function<EntityManager, ProcessDataResponse> request = requestOf.apply(objectOf(dataId));

        try (Connection holder = holder()) {
            lock(holder, "SELECT id FROM eds_data WHERE id = ? FOR UPDATE", first);
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<ProcessDataResponse> processed = executor.submit(() -> inOwnTransaction(request));
                awaitBlocked(1, processed);

                // The delete goes on with the second row, which the request mustn't hold.
                assertDoesNotThrow(() -> lock(holder, "SELECT id FROM eds_data WHERE id = ? FOR UPDATE", second), name);
                holder.rollback();

                final ProcessDataResponse response = processed.get(30, TimeUnit.SECONDS);
                assertTrue(response.isOk(), response.getReturnMessage());
            }
        }
        assertEquals(0L, single("SELECT COUNT(*) FROM eds_data WHERE id = ?", thumbnail));
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private ProcessCircleResponse deleteCircle(final EntityManager em, final String circleId) {
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.DELETE);
        request.setCircleId(circleId);
        return prepareManagementBean(em, settings).processCircle(request);
    }

    /** A Circle of member1 with an object, committed. */
    private String createCircleWithContent(final String name) {
        final String circleId = createCircle(name);
        final ProcessDataResponse added = inOwnTransaction(em ->
                prepareShareBean(em, settings).processData(prepareAddDataRequest(MEMBER_1, circleId, "object of " + name, 100)));
        assertTrue(added.isOk(), added.getReturnMessage());
        return circleId;
    }

    private String createCircle(final String name) {
        return inOwnTransaction(em -> {
            final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
            request.setAction(Action.CREATE);
            request.setCircleName(name);
            final ProcessCircleResponse response = prepareManagementBean(em, settings).processCircle(request);
            assertTrue(response.isOk(), response.getReturnMessage());
            return response.getCircleId();
        });
    }

    /** Starts a rotation of the Circle: the new secret is there, the content still on the retiring one. */
    private static RotationManager.Rotation beginRotation(final String circleId, final long circle, final Settings settings) {
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.ROTATE);
        request.setCircleId(circleId);
        final ProcessCircleResponse asked = inOwnTransaction(em -> new ManagementBean(em, null, settings, null, null).processCircle(request));
        assertTrue(asked.isOk(), asked.getReturnMessage());
        final CircleRotations.Handoff handoff = CircleRotations.pending(circle);
        assertNotNull(handoff);
        try {
            final RotationManager.Rotation rotation = inOwnTransaction(em -> new RotationManager(settings, em, null).begin(handoff));
            assertNotNull(rotation);
            return rotation;
        } finally {
            CircleRotations.finished(handoff);
        }
    }

    private byte[] read(final String dataId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_1);
        request.setDataId(dataId);
        final FetchDataResponse response = inOwnTransaction(em -> prepareShareBean(em, settings).fetchData(request));
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getData();
    }

    /**
     * A thumbnail of the Data Object of the content row, as an empty row of
     * its own, committed; with a lower ID than the content if asked for. A
     * thumbnail is normally added after its content, but the order of the
     * locks mustn't depend on it, and each of the two orders is the one in
     * which a request which took the rows one by one would hold one of them.
     */
    private static long addThumbnail(final long dataId, final boolean lower) throws SQLException {
        final long id = lower ? -dataId : dataId + 1_000_000;
        try (Connection connection = DATABASE.getConnection();
             PreparedStatement metadata = connection.prepareStatement("INSERT INTO eds_metadata (external_id, parent_id, circle_id, datatype_id, name) "
                     + "SELECT ?, m.id, m.circle_id, (SELECT id FROM eds_datatypes WHERE datatype_name = 'thumbnail'), 'thumbnail' "
                     + "FROM eds_metadata m JOIN eds_data d ON d.metadata_id = m.id WHERE d.id = ?");
             PreparedStatement data = connection.prepareStatement("INSERT INTO eds_data (id, metadata_id, key_id, checksum) "
                     + "SELECT ?, (SELECT MAX(id) FROM eds_metadata), key_id, 'empty' FROM eds_data WHERE id = ?")) {
            metadata.setString(1, UUID.randomUUID().toString());
            metadata.setLong(2, dataId);
            assertEquals(1, metadata.executeUpdate());
            data.setLong(1, id);
            data.setLong(2, dataId);
            assertEquals(1, data.executeUpdate());
        }
        return id;
    }

    private static String objectOf(final long dataId) throws SQLException {
        try (Connection connection = DATABASE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT m.external_id FROM eds_metadata m JOIN eds_data d ON d.metadata_id = m.id WHERE d.id = ?")) {
            statement.setLong(1, dataId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    fail("No Data Object for the content row " + dataId);
                }
                return result.getString(1);
            }
        }
    }

    private long circleIdOf(final String externalId) {
        final Long id = inOwnTransaction(em -> {
            final CircleEntity circle = new DataDao(em).find(CircleEntity.class, externalId);
            return (circle != null) ? circle.getId() : null;
        });
        assertNotNull(id);
        return id;
    }

    private static long dataRowOf(final long circle) throws SQLException {
        final long id = single("SELECT MAX(d.id) FROM eds_data d JOIN eds_metadata m ON m.id = d.metadata_id WHERE m.circle_id = ?", circle);
        assertEquals(1L, single("SELECT COUNT(*) FROM eds_data_parts WHERE data_id = ?", id));
        return id;
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

    /** A connection of its own, as the rotation's, which gives up on a lock soon. */
    private static Connection holder() throws SQLException {
        final Connection connection = DATABASE.getConnection();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCK_TIMEOUT " + HOLDER_TIMEOUT_MILLIS);
        }
        return connection;
    }

    private static void lock(final Connection connection, final String sql, final long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    fail("Nothing to lock.");
                }
            }
        }
    }

    private static void update(final Connection connection, final String sql, final long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    /** What the rotation does with the row it holds: new parts for the old. */
    private static void replaceParts(final Connection connection, final long dataId) throws SQLException {
        update(connection, "DELETE FROM eds_data_parts WHERE data_id = ?", dataId);
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO eds_data_parts (data_id, part_offset, part_length, part) VALUES (?, 0, 1, ?)")) {
            statement.setLong(1, dataId);
            statement.setBytes(2, new byte[] { 1 });
            statement.executeUpdate();
        }
    }

    private static long single(final String sql, final String id) throws SQLException {
        try (Connection connection = DATABASE.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    fail("No result of " + sql);
                }
                return result.getLong(1);
            }
        }
    }

    private static long single(final String sql, final long id) throws SQLException {
        try (Connection connection = DATABASE.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    fail("No result of " + sql);
                }
                return result.getLong(1);
            }
        }
    }

    /**
     * Waits until as many sessions wait for a lock, or one of the steps has
     * ended (e.g. failed before it came to wait).
     */
    private static void awaitBlocked(final int sessions, final Future<?>... steps) throws SQLException {
        final long until = System.currentTimeMillis() + 20_000;
        try (Connection connection = DATABASE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE BLOCKER_ID IS NOT NULL")) {
            while (System.currentTimeMillis() < until) {
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next() && (result.getLong(1) >= sessions)) {
                        return;
                    }
                }
                if (ended(steps)) {
                    return;
                }
            }
        }
        fail("The steps didn't come to wait for the lock.");
    }

    /** Whether a step has ended; waits a moment for the first one. */
    private static boolean ended(final Future<?>... steps) {
        try {
            steps[0].get(20, TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException _) {
            return Arrays.stream(steps).anyMatch(Future::isDone);
        } catch (ExecutionException _) {
            return true;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return true;
        }
    }
}
