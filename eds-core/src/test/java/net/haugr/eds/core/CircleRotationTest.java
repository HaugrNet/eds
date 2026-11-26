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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.dtos.Circle;
import net.haugr.eds.api.dtos.Entry;
import net.haugr.eds.api.dtos.Trustee;
import net.haugr.eds.api.dtos.TrusteeSecret;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.EntryRequest;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.responses.EntryResponse;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.enums.Status;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.features.EntryQuery;
import net.haugr.eds.core.managers.RotationManager;
import net.haugr.eds.core.managers.StatusManager;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.KeyEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import net.haugr.eds.core.rotation.CircleRotations;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.setup.fakes.FakeUserTransaction;
import net.haugr.eds.core.stream.ContentDownload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>The rotation of a Circle Secret (PLAN.md, Crypto C6), through the beans,
 * on the test database: when it is due, what starts it, and that everything
 * reads and writes as before, during and after it.</p>
 *
 * <p>Without a runner, an offer of the secrets is kept by
 * {@link CircleRotations}; the tests take it and run the rotation in their
 * own transaction.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class CircleRotationTest extends DatabaseSetup {

    private static final String BOARD = "board";
    private static final String DECLARATION = """
            {"feature": "board", "version": 1, "kinds": {
              "note": {"add": "WRITE", "change": ["author"], "remove": ["author", "administrators"], "size": 100},
              "vote": {"add": "READ", "change": ["author"], "about": "note", "keep": "member-about", "size": 10}
            }}""";

    /**
     * Settings of this test's own, as other tests in the same JVM may change
     * the shared ones; the rooms are new for each test, so nothing else
     * offers their secrets.
     */
    private final Settings mine = newSettings();

    @BeforeEach
    void noRunner() {
        CircleRotations.install(null);
    }

    // =========================================================================
    // When a rotation is due, and what starts it
    // =========================================================================

    @Test
    void testNewCircleIsDueAfterThePeriod() {
        final String circleId = createCircle(MEMBER_1, "rotating");
        final LocalDateTime due = circle(circleId).getRotateCircleKeyDue();
        assertNotNull(due);
        assertTrue(due.isAfter(Utilities.newDate().plusDays(364)));

        mine.set(StandardSetting.CIRCLE_SECRET_ROTATION_DAYS.getKey(), "0");
        assertNull(circle(createCircle(MEMBER_1, "never rotating")).getRotateCircleKeyDue());
    }

    @Test
    void testRemovalStartsTheRotationAtTheNextRead() {
        final String room = room("testRemovalStartsTheRotationAtTheNextRead");
        final Stored first = add(MEMBER_1, room, "first", 300);
        final Stored second = add(MEMBER_2, room, "second", 70_000);
        final long before = activeKey(room).getId();

        // Nothing is due, so a read offers nothing.
        assertArrayEquals(first.bytes(), read(MEMBER_1, first.dataId()));
        assertFalse(offered(room));

        assertTrue(bean().processTrustee(remove(MEMBER_1, room, MEMBER_3_ID)).isOk());
        assertTrue(circle(room).isRotationDue(Utilities.newDate()));
        assertArrayEquals(first.bytes(), read(MEMBER_2, first.dataId()));
        assertTrue(offered(room));
        rotate(room);

        final KeyEntity after = activeKey(room);
        assertNotEquals(before, after.getId());
        assertNull(find(KeyEntity.class, before), "The retiring secret goes at the end.");
        assertEquals(List.of(after.getId()), keyIds(room));
        assertEquals(2, dataOnKey(after));
        for (final TrusteeEntity trustee : trustees(room)) {
            assertEquals(after.getId(), trustee.getKey().getId());
            assertNull(trustee.getPreviousKey());
            assertNull(trustee.getPreviousCircleKey());
        }
        assertArrayEquals(first.bytes(), read(MEMBER_1, first.dataId()));
        assertArrayEquals(second.bytes(), read(MEMBER_2, second.dataId()));
        assertTrue(circle(room).getRotateCircleKeyDue().isAfter(Utilities.newDate().plusDays(364)));
        assertFalse(CircleRotations.isRunning(circle(room).getId()));
    }

    @Test
    void testPeriodicRotationWithoutAPeriodAfterwards() {
        final String other = room("testPeriodicRotationWithoutAPeriodAfterwards, the other");
        final Stored stored = add(MEMBER_1, other, "periodic", 1000);
        setDue(other, Utilities.newDate().minusMinutes(1));
        mine.set(StandardSetting.CIRCLE_SECRET_ROTATION_DAYS.getKey(), "0");

        assertArrayEquals(stored.bytes(), read(MEMBER_3, stored.dataId()));
        rotate(other);

        assertNull(circle(other).getRotateCircleKeyDue());
        assertArrayEquals(stored.bytes(), read(MEMBER_2, stored.dataId()));
    }

    @Test
    void testTheSystemAdministratorMakesItDueButCantStartIt() {
        final String room = room("testTheSystemAdministratorMakesItDueButCantStartIt");
        final String other = room("testTheSystemAdministratorMakesItDueButCantStartIt, the other");
        final ManagementBean bean = bean();
        assertTrue(bean.processTrustee(remove(Constants.ADMIN_ACCOUNT, room, MEMBER_3_ID)).isOk());
        assertTrue(circle(room).isRotationDue(Utilities.newDate()));

        // The System Administrator isn't a Trustee, and can't read the secret.
        assertTrue(bean.fetchCircles(prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT)).isOk());
        assertFalse(offered(room));

        final ProcessCircleResponse asked = bean.processCircle(rotateRequest(Constants.ADMIN_ACCOUNT, other));
        assertTrue(asked.isOk(), asked.getReturnMessage());
        assertTrue(circle(other).isRotationDue(Utilities.newDate()));
        assertFalse(offered(other));
    }

    @Test
    void testACircleAdministratorAsksForARotation() {
        final String room = room("testACircleAdministratorAsksForARotation");
        final ManagementBean bean = bean();
        final ProcessCircleResponse refused = bean.processCircle(rotateRequest(MEMBER_2, room));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), refused.getReturnCode(), refused.getReturnMessage());
        assertFalse(circle(room).isRotationDue(Utilities.newDate()));

        final long before = activeKey(room).getId();
        final ProcessCircleResponse asked = bean.processCircle(rotateRequest(MEMBER_1, room));
        assertTrue(asked.isOk(), asked.getReturnMessage());
        assertEquals("The Circle Secret of the Circle 'testACircleAdministratorAsksForARotation' is being rotated.", asked.getReturnMessage());
        assertTrue(offered(room), "The administrator's own secret starts it at once.");
        rotate(room);
        assertEquals(1, keyIds(room).size());
        assertNotEquals(before, activeKey(room).getId());
    }

    @Test
    void testDeletingAMemberMakesTheirCirclesDue() {
        final ProcessMemberRequest delete = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        delete.setAction(Action.DELETE);
        delete.setMemberId(MEMBER_3_ID);
        assertTrue(bean().processMember(delete).isOk());

        final LocalDateTime now = Utilities.newDate();
        assertTrue(circle(CIRCLE_1_ID).isRotationDue(now));
        assertTrue(circle(CIRCLE_2_ID).isRotationDue(now));
        assertTrue(circle(CIRCLE_3_ID).isRotationDue(now));
    }

    // =========================================================================
    // During a rotation
    // =========================================================================

    @Test
    void testReadingAndWritingDuringTheRotation() {
        final String room = room("testReadingAndWritingDuringTheRotation");
        final String other = room("testReadingAndWritingDuringTheRotation, the other");
        final Stored old = add(MEMBER_1, room, "old", 500);
        final Stored moving = add(MEMBER_1, room, "moving", 600);
        final RotationManager.Rotation rotation = begin(room);
        assertEquals(Status.RETIRING, find(KeyEntity.class, rotation.previousKeyId()).getStatus());

        // The objects not yet re-encrypted read with the retiring secret,
        // new ones are written with the new.
        assertArrayEquals(old.bytes(), read(MEMBER_3, old.dataId()));
        final Stored fresh = add(MEMBER_2, room, "fresh", 700);
        assertEquals(rotation.keyId(), dataEntity(fresh.dataId()).getKey().getId());

        // A move out of the Circle reads with the retiring secret.
        final ProcessDataRequest move = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        move.setAction(Action.MOVE);
        move.setDataId(moving.dataId());
        move.setTargetCircleId(other);
        final ProcessDataResponse moved = share().processData(move);
        assertTrue(moved.isOk(), moved.getReturnMessage());
        assertArrayEquals(moving.bytes(), read(MEMBER_1, moved.getDataId()));

        // A Trustee added now gets both secrets, and reads everything.
        final ProcessTrusteeRequest addTrustee = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        addTrustee.setAction(Action.ADD);
        addTrustee.setCircleId(room);
        addTrustee.setMemberId(MEMBER_4_ID);
        addTrustee.setTrustLevel(TrustLevel.READ);
        assertTrue(bean().processTrustee(addTrustee).isOk());
        assertArrayEquals(old.bytes(), read(MEMBER_4, old.dataId()));
        assertArrayEquals(fresh.bytes(), read(MEMBER_4, fresh.dataId()));
    }

    @Test
    void testTheRotationIsToldWhileItRunsAndAfter() {
        final String room = room("testTheRotationIsToldWhileItRunsAndAfter");
        final Stored old = add(MEMBER_1, room, "old", 500);
        final Map<String, Double> before = new StatusManager(mine, entityManager).inventory();
        begin(room);
        final Stored fresh = add(MEMBER_2, room, "fresh", 700);

        // The circle tells the rotation under way.
        final Circle listed = listed(MEMBER_1, room);
        assertNotNull(listed.getRotation());
        assertEquals(2, listed.getRotation().getObjects());
        assertEquals(1, listed.getRotation().getObjectsDone());
        final Map<String, Double> inventory = new StatusManager(mine, entityManager).inventory();
        assertEquals(before.get("rotationsRunning") + 1, inventory.get("rotationsRunning"));
        assertEquals(before.get("rotationObjects") + 1, inventory.get("rotationObjects"));

        // A Trustee's read hands both secrets over, and the rotation ends.
        assertArrayEquals(fresh.bytes(), read(MEMBER_2, fresh.dataId()));
        rotate(room);
        assertArrayEquals(old.bytes(), read(MEMBER_3, old.dataId()));
        final Circle after = listed(MEMBER_1, room);
        assertNull(after.getRotation());
        assertNotNull(after.getSecretRotated());
        assertNotNull(after.getRotationDue());
        assertEquals(before.get("rotationsRunning"), new StatusManager(mine, entityManager).inventory().get("rotationsRunning"));
    }

    @Test
    void testAnInterruptedRotationContinues() {
        final String room = room("testAnInterruptedRotationContinues");
        final Stored one = add(MEMBER_1, room, "one", 100);
        final Stored two = add(MEMBER_1, room, "two", 200);
        final RotationManager.Rotation rotation = begin(room);
        final RotationManager manager = new RotationManager(mine, entityManager, null);
        assertTrue(manager.reencrypt(rotation, dataEntity(one.dataId()).getId()));
        assertFalse(manager.reencrypt(rotation, dataEntity(one.dataId()).getId()), "An object on the new secret is left alone.");
        assertEquals(RotationManager.Outcome.MORE, manager.end(rotation));
        // The stone stops: nothing is left of the job but what is stored.
        CircleRotations.forget(circle(room).getId());

        assertArrayEquals(two.bytes(), read(MEMBER_3, two.dataId()));
        final CircleRotations.Handoff handoff = CircleRotations.pending(circle(room).getId());
        assertNotNull(handoff.previous());
        rotate(room);
        assertEquals(1, keyIds(room).size());
        assertArrayEquals(one.bytes(), read(MEMBER_1, one.dataId()));
        assertArrayEquals(two.bytes(), read(MEMBER_1, two.dataId()));
    }

    @Test
    void testARemovalDuringTheRotationRotatesAgain() {
        final String other = room("testARemovalDuringTheRotationRotatesAgain, the other");
        final Stored stored = add(MEMBER_1, other, "again", 100);
        final RotationManager.Rotation rotation = begin(other);
        assertTrue(bean().processTrustee(remove(MEMBER_1, other, MEMBER_3_ID)).isOk());

        // The job has the new secret, so the next rotation follows at once.
        new RotationBean(entityManager, mine, null).rotate(handoff(rotation));
        final KeyEntity active = activeKey(other);
        assertNotEquals(rotation.keyId(), active.getId());
        assertEquals(List.of(active.getId()), keyIds(other));
        assertFalse(circle(other).isRotationDue(Utilities.newDate()));
        assertArrayEquals(stored.bytes(), read(MEMBER_2, stored.dataId()));
    }

    @Test
    void testAMasterKeyRotationDuringTheCircleRotation() {
        final String room = room("testAMasterKeyRotationDuringTheCircleRotation");
        final Stored stored = add(MEMBER_1, room, "masterkey", 400);
        begin(room);

        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(NEW_MASTERKEY_SECRET.getBytes(StandardCharsets.UTF_8));
        request.setCredential(credentialOf(Constants.ADMIN_ACCOUNT));
        assertTrue(bean().masterKey(request).isOk());

        assertArrayEquals(stored.bytes(), read(MEMBER_2, stored.dataId()));
        rotate(room);
        assertArrayEquals(stored.bytes(), read(MEMBER_3, stored.dataId()));
    }

    @Test
    void testANewPassphraseDuringTheRotation() {
        final String room = room("testANewPassphraseDuringTheRotation");
        final Stored stored = add(MEMBER_1, room, "passphrase", 400);
        begin(room);

        final ProcessMemberRequest update = prepareRequest(ProcessMemberRequest.class, MEMBER_2);
        update.setAction(Action.UPDATE);
        update.setNewCredential(NEW_PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        final ProcessMemberResponse updated = bean().processMember(update);
        assertTrue(updated.isOk(), updated.getReturnMessage());

        final FetchDataRequest fetch = prepareRequest(FetchDataRequest.class, MEMBER_2);
        fetch.setCredential(NEW_PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        fetch.setDataId(stored.dataId());
        final FetchDataResponse fetched = share().fetchData(fetch);
        assertTrue(fetched.isOk(), fetched.getReturnMessage());
        assertArrayEquals(stored.bytes(), fetched.getData());
    }

    @Test
    void testPendingInvitationsIntoTheCircleGo() {
        final String room = room("testPendingInvitationsIntoTheCircleGo");
        final ProcessMemberRequest invite = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        invite.setAction(Action.INVITE);
        invite.setNewAccountName("guest");
        invite.setCircleId(room);
        final ProcessMemberResponse invited = bean().processMember(invite);
        assertTrue(invited.isOk(), invited.getReturnMessage());
        assertEquals(1L, invitations(room));

        begin(room);
        assertEquals(0L, invitations(room));
    }

    @Test
    void testTrusteeRowsCantBeSwapped() {
        final String room = room("testTrusteeRowsCantBeSwapped");
        final Stored stored = add(MEMBER_2, room, "swapped", 100);
        begin(room);
        final TrusteeEntity trustee = trustees(room).stream()
                .filter(t -> MEMBER_2_ID.equals(t.getMember().getExternalId())).findFirst().orElseThrow();
        final String current = trustee.getCircleKey();
        trustee.setCircleKey(trustee.getPreviousCircleKey());
        trustee.setPreviousCircleKey(current);
        save(trustee);

        // Each wrapped secret is bound to its version, so neither opens in
        // the place of the other.
        final FetchDataRequest fetch = prepareRequest(FetchDataRequest.class, MEMBER_2);
        fetch.setDataId(stored.dataId());
        assertEquals(ReturnCode.CRYPTO_ERROR.getCode(), share().fetchData(fetch).getReturnCode());
        final ProcessDataRequest add = prepareAddDataRequest(MEMBER_2, room, "written", 10);
        assertEquals(ReturnCode.CRYPTO_ERROR.getCode(), share().processData(add).getReturnCode());
    }

    @Test
    void testAnObjectWhichFailsItsCheckHoldsTheRotationBack() {
        final String room = room("testAnObjectWhichFailsItsCheckHoldsTheRotationBack");
        final Stored damaged = add(MEMBER_1, room, "damaged", 100);
        final Stored intact = add(MEMBER_1, room, "intact", 100);
        entityManager.flush();
        // The last byte of the stored content is changed, so its tag fails.
        final long damagedRow = dataEntity(damaged.dataId()).getId();
        final byte[] part = (byte[]) entityManager.createNativeQuery("SELECT part FROM eds_data_parts WHERE data_id = ? ORDER BY part_offset DESC")
                .setParameter(1, damagedRow).setMaxResults(1).getSingleResult();
        part[part.length - 1] ^= 1;
        entityManager.createNativeQuery("UPDATE eds_data_parts SET part = ? WHERE data_id = ?")
                .setParameter(1, part).setParameter(2, damagedRow).executeUpdate();
        entityManager.clear();

        assertTrue(bean().processCircle(rotateRequest(MEMBER_1, room)).isOk());
        rotate(room);
        assertEquals(2, keyIds(room).size(), "The retiring secret stays for the damaged object.");
        assertEquals(SanityStatus.FAILED, dataEntity(damaged.dataId()).getSanityStatus());
        assertEquals(activeKey(room).getId(), dataEntity(intact.dataId()).getKey().getId());

        // The next try waits, instead of every request starting one.
        assertArrayEquals(intact.bytes(), read(MEMBER_2, intact.dataId()));
        assertFalse(offered(room));

        // Once the damaged object is gone, and the wait is over, it ends.
        final ProcessDataRequest delete = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        delete.setAction(Action.DELETE);
        delete.setDataId(damaged.dataId());
        assertTrue(share().processData(delete).isOk());
        CircleRotations.forget(circle(room).getId());
        assertArrayEquals(intact.bytes(), read(MEMBER_2, intact.dataId()));
        rotate(room);
        assertEquals(1, keyIds(room).size());
    }

    @Test
    void testTheETagStaysAsTheContentDoesnt() {
        final String room = room("testTheETagStaysAsTheContentDoesnt");
        final long before = activeKey(room).getId();
        final byte[] token = session(MEMBER_1);
        final Stored stored = add(MEMBER_1, room, "etag", 10);
        final byte[] content = generateData(70_000);
        final ProcessDataResponse uploaded = share().uploadContent(token, stored.dataId(), new ByteArrayInputStream(content), content.length, null);
        assertTrue(uploaded.isOk(), uploaded.getReturnMessage());

        assertTrue(bean().processCircle(rotateRequest(MEMBER_1, room)).isOk());
        rotate(room);
        assertNotEquals(before, dataEntity(stored.dataId()).getKey().getId());

        // A client which read the content before replaces it with If-Match.
        final byte[] next = generateData(500);
        final ProcessDataResponse replaced = share().uploadContent(session(MEMBER_1), stored.dataId(), new ByteArrayInputStream(next), next.length, uploaded.getEtag());
        assertTrue(replaced.isOk(), replaced.getReturnMessage());
    }

    /**
     * A download prepared on the retiring secret still streams, when the
     * content was re-encrypted with the new secret before it is streamed;
     * the ETag stays the same.
     */
    @Test
    void testDownloadPreparedBeforeTheContentIsReencrypted() throws IOException {
        final String room = room("testDownloadPreparedBeforeTheContentIsReencrypted");
        final Stored stored = add(MEMBER_1, room, "download", 10);
        final byte[] content = generateData(70_000);
        final ProcessDataResponse uploaded = share().uploadContent(session(MEMBER_1), stored.dataId(), new ByteArrayInputStream(content), content.length, null);
        assertTrue(uploaded.isOk(), uploaded.getReturnMessage());
        final RotationManager.Rotation rotation = begin(room);

        final ContentDownload download = share().prepareContent(session(MEMBER_3), stored.dataId(), null, null);
        assertTrue(download.isOk(), () -> download.error().getReturnMessage());
        assertEquals(rotation.previousKeyId(), download.context().circleSecretVersion());
        assertEquals(uploaded.getEtag(), download.etag());
        assertTrue(new RotationManager(mine, entityManager, null).reencrypt(rotation, dataEntity(stored.dataId()).getId()));
        entityManager.flush();

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        share().streamContent(download, output);
        assertArrayEquals(content, output.toByteArray());
    }

    @Test
    void testEntriesDuringTheRotation() {
        final String room = room("testEntriesDuringTheRotation");
        final FeatureBean features = new FeatureBean(entityManager, mine);
        assertTrue(features.putFeature(session(Constants.ADMIN_ACCOUNT), BOARD, DECLARATION).isOk());
        assertTrue(features.setCircleFeature(session(MEMBER_1), room, BOARD, FeatureState.ON).isOk());
        final Entry note = features.addEntry(session(MEMBER_2), room, BOARD, entry("note", null, "before")).getEntries().getFirst();
        begin(room);

        // Its content is still on the retiring secret, its author on the new.
        final Entry read = features.entries(session(MEMBER_3), room, BOARD, EntryQuery.all()).getEntries().getFirst();
        assertEquals("before", new String(Base64.getDecoder().decode(read.getContent()), StandardCharsets.UTF_8));
        assertEquals(MEMBER_2_ID, read.getMemberId());
        final EntryResponse changed = features.changeEntry(session(MEMBER_2), room, BOARD, note.getEntryId(), entry(null, null, "during"));
        assertTrue(changed.isOk(), changed.getReturnMessage());
        assertEquals("during", new String(Base64.getDecoder().decode(changed.getEntries().getFirst().getContent()), StandardCharsets.UTF_8));
    }

    // =========================================================================
    // What is sealed with the Circle Secret besides the data
    // =========================================================================

    @Test
    void testExternalKeyAndSecretsAreSealedAnew() {
        final String room = room("testExternalKeyAndSecretsAreSealedAnew");
        final ManagementBean bean = bean();
        final ProcessCircleRequest update = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        update.setAction(Action.UPDATE);
        update.setCircleId(room);
        update.setCircleKey("the external key");
        assertTrue(bean.processCircle(update).isOk());
        final ProcessTrusteeRequest secrets = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        secrets.setAction(Action.SECRETS);
        secrets.setCircleId(room);
        secrets.setSecrets(List.of(secret(MEMBER_1_ID, "for member1"), secret(MEMBER_2_ID, "for member2")));
        assertTrue(bean.processTrustee(secrets).isOk());

        assertTrue(bean.processCircle(rotateRequest(MEMBER_1, room)).isOk());
        rotate(room);

        assertEquals("the external key", listed(MEMBER_2, room).getCircleKey());
        final FetchTrusteeRequest fetch = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        fetch.setCircleId(room);
        final List<Trustee> listed = bean.fetchTrustees(fetch).getTrustees();
        assertEquals("for member1", listed.stream().filter(t -> MEMBER_1_ID.equals(t.getMemberId())).findFirst().orElseThrow().getSecret());
        assertEquals("for member2", listed.stream().filter(t -> MEMBER_2_ID.equals(t.getMemberId())).findFirst().orElseThrow().getSecret());
    }

    @Test
    void testEntriesKeepTheirAuthorsAndOwners() {
        final String room = room("testEntriesKeepTheirAuthorsAndOwners");
        final FeatureBean features = new FeatureBean(entityManager, mine);
        assertTrue(features.putFeature(session(Constants.ADMIN_ACCOUNT), BOARD, DECLARATION).isOk());
        assertTrue(features.setCircleFeature(session(MEMBER_1), room, BOARD, FeatureState.ON).isOk());
        final Entry note = features.addEntry(session(MEMBER_2), room, BOARD, entry("note", null, "a note")).getEntries().getFirst();
        assertTrue(features.addEntry(session(MEMBER_3), room, BOARD, entry("vote", note.getEntryId(), "yes")).isOk());

        assertTrue(bean().processCircle(rotateRequest(MEMBER_1, room)).isOk());
        rotate(room);

        final List<Entry> entries = features.entries(session(MEMBER_1), room, BOARD, EntryQuery.all()).getEntries();
        final Entry readNote = entries.stream().filter(e -> "note".equals(e.getKind())).findFirst().orElseThrow();
        assertEquals(MEMBER_2_ID, readNote.getMemberId());
        assertEquals("a note", new String(Base64.getDecoder().decode(readNote.getContent()), StandardCharsets.UTF_8));
        // Only the author changes a note, so the owner tag still finds it;
        // a second vote of the same Member replaces the first.
        final EntryResponse changed = features.changeEntry(session(MEMBER_2), room, BOARD, note.getEntryId(), entry(null, null, "changed"));
        assertTrue(changed.isOk(), changed.getReturnMessage());
        assertTrue(features.addEntry(session(MEMBER_3), room, BOARD, entry("vote", note.getEntryId(), "no")).isOk());
        assertEquals(1, features.entries(session(MEMBER_1), room, BOARD, EntryQuery.all().kind("vote")).getEntries().size());
    }

    @Test
    void testDeletingACircleTakesItsSecrets() {
        final String circleId = createCircle(MEMBER_1, "short lived");
        add(MEMBER_1, circleId, "content", 100);
        begin(circleId);
        final long circle = circle(circleId).getId();

        final ProcessCircleRequest delete = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        delete.setAction(Action.DELETE);
        delete.setCircleId(circleId);
        final ProcessCircleResponse deleted = bean().processCircle(delete);
        assertTrue(deleted.isOk(), deleted.getReturnMessage());
        entityManager.flush();
        assertEquals(0L, entityManager.createQuery("select count(k.id) from KeyEntity k where k.circle.id = :circle", Long.class)
                .setParameter("circle", circle).getSingleResult());
    }

    // =========================================================================
    // When there is nothing to do, or the rotation can't go on
    // =========================================================================

    @Test
    void testTheRotationHasItsOwnSteps() {
        final RotationManager manager = new RotationManager(mine, entityManager, null);
        final Authentication request = new Authentication();

        final EDSException cause = assertThrows(EDSException.class, () -> manager.perform(request));
        assertEquals(ReturnCode.ILLEGAL_SERVICE, cause.getReturnCode());
    }

    @Test
    void testAnOfferWithNothingToDoIsLeftAlone() {
        final String room = room("testAnOfferWithNothingToDoIsLeftAlone");
        final CircleEntity circle = circle(room);
        final long active = activeKey(room).getId();
        final RotationManager manager = new RotationManager(mine, entityManager, null);

        // A Circle which is gone, a secret which was rotated since the
        // request read it, and a Circle whose rotation isn't due.
        assertNull(manager.begin(new CircleRotations.Handoff(-1L, "gone", active, null, null, null)));
        assertNull(manager.begin(new CircleRotations.Handoff(circle.getId(), room, active + 1000, null, null, null)));
        assertNull(manager.begin(new CircleRotations.Handoff(circle.getId(), room, active, null, null, null)));
        assertEquals(List.of(active), keyIds(room));

        // Under way, but with another retiring secret than the one handed over.
        final RotationManager.Rotation rotation = begin(room);
        assertNull(manager.begin(new CircleRotations.Handoff(circle.getId(), room, rotation.keyId(), rotation.current(),
                rotation.previousKeyId() + 1000, rotation.previous())));

        // The bean frees the Circle all the same.
        final CircleRotations.Handoff gone = new CircleRotations.Handoff(-1L, "gone", active, null, null, null);
        new RotationBean(entityManager, mine, null).rotate(gone);
        assertFalse(CircleRotations.isRunning(-1L));
    }

    @Test
    void testWhatIsGoneMeanwhileIsLeftAlone() {
        final String room = room("testWhatIsGoneMeanwhileIsLeftAlone");
        final RotationManager.Rotation rotation = begin(room);
        final RotationManager manager = new RotationManager(mine, entityManager, null);

        // A content row which is gone, and a Circle which is gone.
        assertFalse(manager.reencrypt(rotation, -1L));
        final RotationManager.Rotation deleted = new RotationManager.Rotation(-1L, "gone", rotation.keyId(), rotation.current(),
                rotation.previousKeyId(), rotation.previous(), rotation.started());
        assertEquals(-1L, manager.resealEntries(deleted, 0, 10));
        assertEquals(RotationManager.Outcome.DONE, manager.end(deleted));
        // A retiring secret which is gone, of the real Circle.
        final RotationManager.Rotation ended = new RotationManager.Rotation(rotation.circleId(), rotation.externalId(), rotation.keyId(), rotation.current(),
                rotation.previousKeyId() + 1000, rotation.previous(), rotation.started());
        assertEquals(RotationManager.Outcome.DONE, manager.end(ended));
        assertEquals(2, keyIds(room).size(), "Nothing of the real Circle was touched.");
    }

    @Test
    void testEntriesSealedWithTheNewSecretAreLeftAsTheyAre() {
        final String room = room("testEntriesSealedWithTheNewSecretAreLeftAsTheyAre");
        final FeatureBean features = new FeatureBean(entityManager, mine);
        assertTrue(features.putFeature(session(Constants.ADMIN_ACCOUNT), BOARD, DECLARATION).isOk());
        assertTrue(features.setCircleFeature(session(MEMBER_1), room, BOARD, FeatureState.ON).isOk());
        final Entry note = features.addEntry(session(MEMBER_2), room, BOARD, entry("note", null, "twice")).getEntries().getFirst();
        final RotationManager.Rotation rotation = begin(room);
        final RotationManager manager = new RotationManager(mine, entityManager, null);

        // The second pass finds the entry sealed already, and goes on all the same.
        final long last = manager.resealEntries(rotation, 0, 10);
        assertTrue(last > 0);
        assertEquals(last, manager.resealEntries(rotation, 0, 10));
        assertEquals(-1L, manager.resealEntries(rotation, last, 10));

        final Entry read = features.entries(session(MEMBER_3), room, BOARD, EntryQuery.all()).getEntries().getFirst();
        assertEquals(note.getEntryId(), read.getEntryId());
        assertEquals(MEMBER_2_ID, read.getMemberId());
    }

    @Test
    void testATrusteeOnTheRetiringSecretOnlyGetsTheNewOne() {
        final String room = room("testATrusteeOnTheRetiringSecretOnlyGetsTheNewOne");
        final Stored stored = add(MEMBER_1, room, "joined", 100);
        final ProcessTrusteeRequest secrets = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        secrets.setAction(Action.SECRETS);
        secrets.setCircleId(room);
        secrets.setSecrets(List.of(secret(MEMBER_3_ID, "for member3")));
        assertTrue(bean().processTrustee(secrets).isOk());
        // Sealed with the secret which retires.
        final byte[] own = member3(room).getExternalKey().clone();
        final RotationManager.Rotation rotation = begin(room);
        final RotationManager manager = new RotationManager(mine, entityManager, null);
        assertTrue(manager.reencrypt(rotation, dataEntity(stored.dataId()).getId()));

        // Member2 and member3 hold the retiring secret only, as if they had
        // joined while the rotation started; member3 has a secret of their own.
        final KeyEntity retiring = find(KeyEntity.class, rotation.previousKeyId());
        for (final TrusteeEntity trustee : trustees(room)) {
            if (!MEMBER_1_ID.equals(trustee.getMember().getExternalId())) {
                trustee.setKey(retiring);
                trustee.setCircleKey(trustee.getPreviousCircleKey());
                if (MEMBER_3_ID.equals(trustee.getMember().getExternalId())) {
                    trustee.setExternalKey(own);
                }
                save(trustee);
            }
        }
        // Nor is a next rotation set yet.
        setDue(room, null);

        assertEquals(RotationManager.Outcome.DONE, manager.end(rotation));
        entityManager.flush();
        entityManager.clear();

        assertEquals(List.of(rotation.keyId()), keyIds(room));
        for (final TrusteeEntity trustee : trustees(room)) {
            assertEquals(rotation.keyId(), trustee.getKey().getId());
        }
        assertArrayEquals(stored.bytes(), read(MEMBER_2, stored.dataId()));
        assertArrayEquals(stored.bytes(), read(MEMBER_3, stored.dataId()));
        final FetchTrusteeRequest fetch = prepareRequest(FetchTrusteeRequest.class, MEMBER_3);
        fetch.setCircleId(room);
        assertEquals("for member3", bean().fetchTrustees(fetch).getTrustees().stream()
                .filter(t -> MEMBER_3_ID.equals(t.getMemberId())).findFirst().orElseThrow().getSecret());
        assertTrue(circle(room).getRotateCircleKeyDue().isAfter(Utilities.newDate().plusDays(364)));
    }

    @Test
    void testATrusteeSecretWhichCantBeOpenedHoldsTheEndBack() {
        final String room = room("testATrusteeSecretWhichCantBeOpenedHoldsTheEndBack");
        final RotationManager.Rotation rotation = begin(room);
        final TrusteeEntity trustee = member3(room);
        trustee.setKey(find(KeyEntity.class, rotation.previousKeyId()));
        trustee.setExternalKey(new byte[] { 1, 2, 3 });
        save(trustee);

        new RotationBean(entityManager, mine, null).rotate(handoff(rotation));

        assertEquals(2, keyIds(room).size(), "The retiring secret stays.");
        assertEquals(Status.RETIRING, find(KeyEntity.class, rotation.previousKeyId()).getStatus());
        assertFalse(CircleRotations.isRunning(rotation.circleId()));
    }

    @Test
    void testAnObjectWhichCantBeCopiedNowIsTriedAgainLater() {
        final String room = room("testAnObjectWhichCantBeCopiedNowIsTriedAgainLater");
        final Stored stored = add(MEMBER_1, room, "large", 1000);
        final long before = activeKey(room).getId();
        // Too large to be copied in memory, and no connection to read it on:
        // the failure isn't the object's, so it isn't marked.
        mine.set(StandardSetting.DATA_MAX_INLINE_SIZE.getKey(), "100");

        assertTrue(bean().processCircle(rotateRequest(MEMBER_1, room)).isOk());
        rotate(room);

        assertEquals(before, dataEntity(stored.dataId()).getKey().getId());
        assertEquals(2, keyIds(room).size(), "The retiring secret stays for the object.");
        assertNotEquals(SanityStatus.FAILED, dataEntity(stored.dataId()).getSanityStatus());
        mine.set(StandardSetting.DATA_MAX_INLINE_SIZE.getKey(), StandardSetting.DATA_MAX_INLINE_SIZE.getValue());
        assertArrayEquals(stored.bytes(), read(MEMBER_2, stored.dataId()));
        assertFalse(offered(room), "The next try waits.");
    }

    @Test
    void testAnInterruptedRotationStopsAndFreesTheCircle() {
        final String room = room("testAnInterruptedRotationStopsAndFreesTheCircle");
        final FakeUserTransaction transaction = new FakeUserTransaction();
        final RotationBean rotations = new RotationBean(entityManager, transaction, null, mine, null);
        assertTrue(bean().processCircle(rotateRequest(MEMBER_1, room)).isOk());
        final CircleRotations.Handoff handoff = CircleRotations.pending(circle(room).getId());
        assertNotNull(handoff);

        // The stone stops while the rotation waits for the requests which
        // read the retiring secret.
        Thread.currentThread().interrupt();
        rotations.rotate(handoff);

        assertTrue(Thread.interrupted(), "The interruption is kept for the caller.");
        assertTrue(transaction.commits() > 0);
        assertFalse(CircleRotations.isRunning(handoff.circleId()));
        assertEquals(2, keyIds(room).size(), "The next job continues from what is stored.");
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private record Stored(String dataId, byte[] bytes) {
    }

    private ManagementBean bean() {
        return prepareManagementBean(mine);
    }

    private ShareBean share() {
        return prepareShareBean(mine);
    }

    /**
     * A room of this test: member1 administers it, member2 writes and
     * member3 reads.
     */
    private String room(final String name) {
        final String circleId = createCircle(MEMBER_1, name);
        addTrustee(circleId, MEMBER_2_ID, TrustLevel.WRITE);
        addTrustee(circleId, MEMBER_3_ID, TrustLevel.READ);
        return circleId;
    }

    private void addTrustee(final String circleId, final String memberId, final TrustLevel level) {
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setMemberId(memberId);
        request.setTrustLevel(level);
        final ProcessTrusteeResponse response = bean().processTrustee(request);
        assertTrue(response.isOk(), response.getReturnMessage());
    }

    private Stored add(final String account, final String circleId, final String name, final int bytes) {
        final ProcessDataRequest request = prepareAddDataRequest(account, circleId, name, bytes);
        final byte[] copy = Arrays.copyOf(request.getData(), request.getData().length);
        final ProcessDataResponse response = share().processData(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return new Stored(response.getDataId(), copy);
    }

    private byte[] read(final String account, final String dataId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, account);
        request.setDataId(dataId);
        final FetchDataResponse response = share().fetchData(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getData();
    }

    private String createCircle(final String account, final String name) {
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, account);
        request.setAction(Action.CREATE);
        request.setCircleName(name);
        final ProcessCircleResponse response = bean().processCircle(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getCircleId();
    }

    private static ProcessTrusteeRequest remove(final String account, final String circleId, final String memberId) {
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, account);
        request.setAction(Action.REMOVE);
        request.setCircleId(circleId);
        request.setMemberId(memberId);
        return request;
    }

    private static ProcessCircleRequest rotateRequest(final String account, final String circleId) {
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, account);
        request.setAction(Action.ROTATE);
        request.setCircleId(circleId);
        return request;
    }

    private static TrusteeSecret secret(final String memberId, final String secret) {
        final TrusteeSecret entry = new TrusteeSecret();
        entry.setMemberId(memberId);
        entry.setSecret(secret);
        return entry;
    }

    private static EntryRequest entry(final String kind, final String about, final String text) {
        final EntryRequest body = new EntryRequest();
        body.setKind(kind);
        body.setAbout(about);
        body.setContent(Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
        return body;
    }

    private byte[] session(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        final ProcessMemberResponse response = bean().processMember(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getSession();
    }

    private Circle listed(final String account, final String circleId) {
        final FetchCircleResponse response = bean().fetchCircles(prepareRequest(FetchCircleRequest.class, account));
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getCircles().stream().filter(c -> circleId.equals(c.getCircleId())).findFirst().orElseThrow();
    }

    private TrusteeEntity member3(final String circleId) {
        return trustees(circleId).stream()
                .filter(t -> MEMBER_3_ID.equals(t.getMember().getExternalId())).findFirst().orElseThrow();
    }

    private static CircleRotations.Handoff handoff(final RotationManager.Rotation rotation) {
        return new CircleRotations.Handoff(rotation.circleId(), rotation.externalId(), rotation.keyId(), rotation.current(),
                rotation.previousKeyId(), rotation.previous());
    }

    private CircleEntity circle(final String externalId) {
        return dao.find(CircleEntity.class, externalId);
    }

    private void setDue(final String circleId, final LocalDateTime due) {
        final CircleEntity circle = circle(circleId);
        circle.setRotateCircleKeyDue(due);
        save(circle);
    }

    private boolean offered(final String circleId) {
        final CircleRotations.Handoff handoff = CircleRotations.pending(circle(circleId).getId());
        // The offers are kept by the internal ID, in the JVM: one of another
        // test's database under the same ID isn't this Circle's (ticket 24).
        if ((handoff != null) && !circleId.equals(handoff.externalId())) {
            throw new IllegalStateException("The offer is of another Circle under the same ID.");
        }
        return handoff != null;
    }

    /** Runs the rotation the last read handed the secrets to. */
    private void rotate(final String circleId) {
        final CircleRotations.Handoff handoff = CircleRotations.pending(circle(circleId).getId());
        assertNotNull(handoff, "A Trustee's request should have handed the secrets over.");
        new RotationBean(entityManager, mine, null).rotate(handoff);
        entityManager.flush();
        entityManager.clear();
    }

    /** Starts a rotation of the Circle, without re-encrypting anything yet. */
    private RotationManager.Rotation begin(final String circleId) {
        assertTrue(bean().processCircle(rotateRequest(admin(circleId), circleId)).isOk());
        final CircleRotations.Handoff handoff = CircleRotations.pending(circle(circleId).getId());
        assertNotNull(handoff);
        final RotationManager.Rotation rotation = new RotationManager(mine, entityManager, null).begin(handoff);
        assertNotNull(rotation);
        CircleRotations.finished(handoff);
        entityManager.flush();
        return rotation;
    }

    private String admin(final String circleId) {
        return trustees(circleId).stream().filter(t -> t.getTrustLevel() == TrustLevel.ADMIN)
                .findFirst().orElseThrow().getMember().getName();
    }

    private KeyEntity activeKey(final String circleId) {
        return entityManager.createQuery("select k from KeyEntity k where k.circle.externalId = :circle and k.status = :status", KeyEntity.class)
                .setParameter("circle", circleId).setParameter("status", Status.ACTIVE).getSingleResult();
    }

    private List<Long> keyIds(final String circleId) {
        return entityManager.createQuery("select k.id from KeyEntity k where k.circle.externalId = :circle order by k.id", Long.class)
                .setParameter("circle", circleId).getResultList();
    }

    private long dataOnKey(final KeyEntity key) {
        return entityManager.createQuery("select count(d.id) from DataEntity d where d.key = :key", Long.class)
                .setParameter("key", key).getSingleResult();
    }

    private DataEntity dataEntity(final String dataId) {
        return entityManager.createQuery("select d from DataEntity d where d.metadata.externalId = :id", DataEntity.class)
                .setParameter("id", dataId).getSingleResult();
    }

    private List<TrusteeEntity> trustees(final String circleId) {
        return entityManager.createQuery("select t from TrusteeEntity t where t.circle.externalId = :circle order by t.id", TrusteeEntity.class)
                .setParameter("circle", circleId).getResultList();
    }

    private long invitations(final String circleId) {
        return entityManager.createQuery("select count(i.id) from InvitationEntity i where i.circle.externalId = :circle", Long.class)
                .setParameter("circle", circleId).getSingleResult();
    }
}
