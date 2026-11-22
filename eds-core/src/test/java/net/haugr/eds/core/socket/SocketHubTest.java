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
package net.haugr.eds.core.socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.responses.CircleFeatureResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.FeatureBean;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.managers.FeatureManager;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.setup.SerializedEntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>The socket (FEATURES.md): connections authenticate, join Circles, work
 * with the entries of their features, and are told what changed. The
 * connections of these tests are in memory; the hub and the features are the
 * real ones, on the test database.</p>
 *
 * <p>In the test data, member1 administers circle1, member2 may write in
 * it, member3 may read, and member4 isn't in it.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class SocketHubTest extends DatabaseSetup {

    private static final long WAIT_SECONDS = 20;
    private static final String BOARD = "board";
    private static final String DECLARATION = """
            {"feature": "board", "version": 1, "signals": 500, "kinds": {
              "note": {"add": "WRITE", "change": ["author"], "remove": ["author"], "close": ["administrators"], "size": 100},
              "big":  {"add": "WRITE", "size": 20000},
              "reply": {"add": "WRITE", "about": "note", "size": 100}
            }}""";
    private static final String FAST = """
            {"feature": "fast", "version": 1, "signals": 0, "kinds": {"note": {"add": "WRITE", "size": 100}}}""";
    private static final String QUIET = """
            {"feature": "quiet", "version": 1, "kinds": {"note": {"add": "WRITE", "size": 100}}}""";

    private final AtomicReference<SocketHub> listening = new AtomicReference<>();
    private final AtomicInteger ids = new AtomicInteger();
    private final MovingClock clock = new MovingClock();
    private EntityManager shared = null;
    private FeatureBean features = null;
    private SocketHub hub = null;

    @BeforeEach
    void start() {
        // Outside CDI the features tell the hub at once what changed.
        // The hub works on threads of its own, which must see the changes
        // of the test's transaction, so they share its Entity Manager; in
        // the stone each has one of its own.
        shared = SerializedEntityManager.of(entityManager);
        features = new FeatureBean(shared, settings, event -> listening.get().featureChanged(event));
        hub = new SocketHub(features, 200, clock, SocketHub.AUTH_TIMEOUT_SECONDS * 1000);
        listening.set(hub);
        assertTrue(features.putFeature(session(Constants.ADMIN_ACCOUNT), BOARD, DECLARATION).isOk());
        assertTrue(features.putFeature(session(Constants.ADMIN_ACCOUNT), "quiet", QUIET).isOk());
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.ON).isOk());
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, "quiet", FeatureState.ON).isOk());
        assertTrue(features.putFeature(session(Constants.ADMIN_ACCOUNT), "fast", FAST).isOk());
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, "fast", FeatureState.ON).isOk());
    }

    @AfterEach
    void stop() {
        hub.shutdown();
    }

    // =========================================================================
    // Authenticating and joining
    // =========================================================================

    @Test
    void testTheFirstFrameAuthenticates() {
        final Peer peer = open();

        peer.tell("{\"type\":\"AUTH\",\"ref\":\"1\",\"token\":\"" + Base64.getEncoder().encodeToString(session(MEMBER_2)) + "\"}");

        final JsonObject ready = peer.expect("READY");
        assertEquals("1", ready.getString("ref"));
        assertEquals(MEMBER_2_ID, ready.getString("memberId"));
        assertEquals(MEMBER_2, ready.getString("accountName"));
        // Only once.
        peer.tell("{\"type\":\"AUTH\",\"ref\":\"2\",\"token\":\"" + Base64.getEncoder().encodeToString(session(MEMBER_2)) + "\"}");
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), peer.expect("ERROR").getInt("returnCode"));
        assertEquals(1, hub.connections());
    }

    @Test
    void testWithoutAuthenticationTheConnectionIsClosed() throws InterruptedException {
        final Peer first = open();
        first.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), first.expect("ERROR").getInt("returnCode"));
        assertTrue(first.closed.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(SocketConnection.POLICY_VIOLATION, first.code.get());

        // A token which isn't a Session's is answered, and the connection may try again.
        final Peer second = open();
        second.tell("{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(new byte[32]) + "\"}");
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), second.expect("ERROR").getInt("returnCode"));
        second.tell("{\"type\":\"AUTH\",\"token\":\"\"}");
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), second.expect("ERROR").getInt("returnCode"));
        assertEquals(1, hub.connections());
    }

    @Test
    void testWithoutAuthWithinTheDeadlineTheConnectionIsClosed() throws InterruptedException {
        final SocketHub hurried = new SocketHub(features, 200, clock, 100);
        try {
            final Peer peer = new Peer(true);
            hurried.open(peer);

            final JsonObject refused = peer.expect("ERROR");
            assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), refused.getInt("returnCode"));
            assertTrue(refused.getString("returnMessage").startsWith("No AUTH frame within"), refused.toString());
            assertTrue(peer.closed.await(WAIT_SECONDS, TimeUnit.SECONDS));
            assertEquals(SocketConnection.POLICY_VIOLATION, peer.code.get());
            assertEquals(0, hurried.connections());
        } finally {
            hurried.shutdown();
        }
    }

    @Test
    void testAnExpiredSessionEndsTheConnection() throws InterruptedException {
        final Peer asking = joined(MEMBER_1);
        final Peer hearing = joined(MEMBER_3);
        final Peer idle = authenticated(MEMBER_2);
        clock.advance(Duration.ofMinutes(settings.getSessionTimeout() + 1L));

        // A frame of an expired Session is refused.
        asking.tell(frame("COUNT", "\"feature\":\"" + BOARD + "\",\"kind\":\"note\""));
        assertExpired(asking);

        // An announcement isn't sent to it, which ends it as well.
        final net.haugr.eds.api.requests.EntryRequest body = new net.haugr.eds.api.requests.EntryRequest();
        body.setKind("note");
        body.setContent(b64("after the end"));
        assertTrue(features.addEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, body).isOk());
        assertExpired(hearing);

        // A connection which sends nothing is ended by the sweep.
        hub.closeExpired();
        assertExpired(idle);
        assertEquals(0, hub.connections());
    }

    private static void assertExpired(final Peer peer) throws InterruptedException {
        final JsonObject refused = peer.expect("ERROR");
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), refused.getInt("returnCode"));
        assertEquals("The Session has expired.", refused.getString("returnMessage"));
        assertTrue(peer.closed.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(SocketConnection.POLICY_VIOLATION, peer.code.get());
    }

    @Test
    void testAFrameAfterTheSessionEndedClosesTheConnection() throws InterruptedException {
        final Peer peer = joined(MEMBER_2);
        final ProcessMemberRequest logout = prepareSessionRequest(ProcessMemberRequest.class, peer.token);
        logout.setAction(Action.LOGOUT);
        assertTrue(prepareManagementBean(shared, settings).processMember(logout).isOk());
        shared.flush();

        // The hub isn't told; the next frame finds that the Session is gone.
        peer.tell(frame("COUNT", "\"feature\":\"" + BOARD + "\",\"kind\":\"note\""));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), peer.expect("ERROR").getInt("returnCode"));
        assertTrue(peer.closed.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(SocketConnection.POLICY_VIOLATION, peer.code.get());
    }

    @Test
    void testASignalMayBeEmpty() {
        final Peer writer = joined(MEMBER_2);
        final Peer reader = joined(MEMBER_3);

        writer.tell(frame("SIGNAL", "\"feature\":\"fast\""));
        assertEquals("", reader.expect("SIGNAL").getString("signal"));
    }

    @Test
    void testAChangeOfNoCircleIsToldToNobody() {
        final Peer reader = joined(MEMBER_3);

        hub.dataChanged(new DataChange(null, MEMBER_1_ID));
        hub.dataChanged(new DataChange(CIRCLE_1_ID, MEMBER_1_ID));
        assertEquals(CIRCLE_1_ID, reader.expect("FILES").getString("circleId"));
    }

    @Test
    void testTheRotationOfTheCircleSecretIsTold() {
        final Peer reader = joined(MEMBER_3);

        hub.rotationChanged(new RotationChange(CIRCLE_1_ID, false));
        assertEquals(CIRCLE_1_ID, reader.expect("ROTATING").getString("circleId"));
        hub.rotationChanged(new RotationChange(CIRCLE_1_ID, true));
        assertEquals(CIRCLE_1_ID, reader.expect("ROTATED").getString("circleId"));
    }

    @Test
    void testTheTokenIsBase64() {
        final Peer peer = open();

        peer.tell("{\"type\":\"AUTH\",\"token\":\"not *Base64*\"}");
        final JsonObject refused = peer.expect("ERROR");
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), refused.getInt("returnCode"));
        assertEquals("The token is not Base64.", refused.getString("returnMessage"));

        // The URL safe alphabet is taken too; this one names no Session.
        peer.tell("{\"type\":\"AUTH\",\"token\":\"____-___\"}");
        final JsonObject unknown = peer.expect("ERROR");
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), unknown.getInt("returnCode"));
        assertNotEquals("The token is not Base64.", unknown.getString("returnMessage"));
        assertEquals(1, hub.connections());
    }

    @Test
    void testJoiningACircleTellsItsFeatures() {
        final Peer reader = authenticated(MEMBER_3);
        final Peer outsider = authenticated(MEMBER_4);

        reader.tell("{\"type\":\"JOIN\",\"ref\":\"j\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        final JsonObject joined = reader.expect("JOINED");
        assertEquals("j", joined.getString("ref"));
        assertEquals(CIRCLE_1_ID, joined.getString("circleId"));
        assertEquals(3, joined.getJsonArray("features").size());
        assertEquals(BOARD, joined.getJsonArray("features").getJsonObject(0).getString("name"));
        assertEquals("ON", joined.getJsonArray("features").getJsonObject(0).getString("state"));

        // Not a Trustee, not joined; and without joining, nothing is asked.
        outsider.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), outsider.expect("ERROR").getInt("returnCode"));
        outsider.tell(frame("LIST", "\"feature\":\"board\""));
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), outsider.expect("ERROR").getInt("returnCode"));
        outsider.tell("{\"type\":\"JOIN\",\"circleId\":\"not an id\"}");
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), outsider.expect("ERROR").getInt("returnCode"));
    }

    // =========================================================================
    // Entries
    // =========================================================================

    @Test
    void testAnAddedEntryIsAnnouncedToAllWhoJoined() {
        final Peer writer = joined(MEMBER_2);
        final Peer reader = joined(MEMBER_3);
        final Peer admin = joined(MEMBER_1);

        // The answer to the one who asked, the announcement to all.
        writer.tell(frame("ADD", "\"ref\":\"a\",\"feature\":\"board\",\"kind\":\"note\",\"name\":\"first\",\"content\":\"" + b64("hello") + "\""));
        final JsonObject done = writer.answer("ADDED");
        assertEquals("a", done.getString("ref"));
        final JsonObject entry = done.getJsonArray("entries").getJsonObject(0);
        final String entryId = entry.getString("entryId");
        assertEquals("note", entry.getString("kind"));
        assertEquals(MEMBER_2_ID, entry.getString("memberId"));
        assertEquals(1, entry.getJsonNumber("position").longValue());
        for (final Peer peer : new Peer[] { reader, admin }) {
            final JsonObject added = peer.expect("ADDED");
            assertEquals(CIRCLE_1_ID, added.getString("circleId"));
            assertEquals(BOARD, added.getString("feature"));
            assertEquals(entryId, added.getJsonObject("entry").getString("entryId"));
            assertEquals(b64("hello"), added.getJsonObject("entry").getString("content"));
            assertEquals(MEMBER_2, added.getJsonObject("entry").getString("accountName"));
            assertFalse(added.containsKey("ref"));
        }
    }

    @Test
    void testAChangedOrClosedEntryIsAnnouncedToAllWhoJoined() {
        final Peer writer = joined(MEMBER_2);
        final Peer reader = joined(MEMBER_3);
        final Peer admin = joined(MEMBER_1);
        final String entryId = added(writer, reader, admin);

        // Changed, by its author.
        writer.tell(frame("CHANGE", "\"feature\":\"board\",\"entryId\":\"" + entryId + "\",\"content\":\"" + b64("hello again") + "\""));
        writer.answer("CHANGED");
        assertEquals(b64("hello again"), reader.expect("CHANGED").getJsonObject("entry").getString("content"));
        admin.expect("CHANGED");

        // Closed, by an administrator.
        admin.tell(frame("CLOSE", "\"feature\":\"board\",\"entryId\":\"" + entryId + "\",\"closed\":true"));
        assertTrue(admin.answer("CHANGED").getJsonArray("entries").getJsonObject(0).getBoolean("closed"));
        assertTrue(reader.expect("CHANGED").getJsonObject("entry").getBoolean("closed"));
        writer.expect("CHANGED");
        admin.tell(frame("CLOSE", "\"feature\":\"board\",\"entryId\":\"" + entryId + "\",\"closed\":false"));
        admin.answer("CHANGED");
        reader.expect("CHANGED");
        writer.expect("CHANGED");
    }

    @Test
    void testARemovedEntryIsAnnouncedWithoutItsContent() {
        final Peer writer = joined(MEMBER_2);
        final Peer reader = joined(MEMBER_3);
        final Peer admin = joined(MEMBER_1);
        final Peer elsewhere = authenticated(MEMBER_2);
        final String entryId = added(writer, reader, admin);
        writer.tell(frame("CHANGE", "\"feature\":\"board\",\"entryId\":\"" + entryId + "\",\"content\":\"" + b64("hello again") + "\""));
        writer.answer("CHANGED");
        reader.expect("CHANGED");
        admin.expect("CHANGED");

        writer.tell(frame("REMOVE", "\"feature\":\"board\",\"entryId\":\"" + entryId + "\""));
        writer.answer("REMOVED");
        final JsonObject removed = reader.expect("REMOVED");
        assertEquals(entryId, removed.getJsonObject("entry").getString("entryId"));
        assertFalse(removed.getJsonObject("entry").containsKey("content"));
        admin.expect("REMOVED");

        // A connection which didn't join the Circle heard nothing of it all.
        elsewhere.tell("{\"type\":\"JOIN\",\"ref\":\"late\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals("late", elsewhere.expect("JOINED").getString("ref"));
    }


    @Test
    void testTheRulesOfTheFeatureHoldOnTheSocket() {
        final Peer writer = joined(MEMBER_2);
        final Peer reader = joined(MEMBER_3);

        // A note needs WRITE.
        reader.tell(frame("ADD", "\"ref\":\"no\",\"feature\":\"board\",\"kind\":\"note\",\"content\":\"" + b64("x") + "\""));
        final JsonObject refused = reader.expect("ERROR");
        assertEquals("no", refused.getString("ref"));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), refused.getInt("returnCode"));

        writer.tell(frame("ADD", "\"feature\":\"board\",\"kind\":\"note\",\"content\":\"" + b64("mine") + "\""));
        final String entryId = writer.answer("ADDED").getJsonArray("entries").getJsonObject(0).getString("entryId");
        reader.expect("ADDED");
        // Only its author changes it; only administrators close it.
        reader.tell(frame("CHANGE", "\"feature\":\"board\",\"entryId\":\"" + entryId + "\",\"content\":\"" + b64("no") + "\""));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), reader.expect("ERROR").getInt("returnCode"));
        writer.tell(frame("CLOSE", "\"feature\":\"board\",\"entryId\":\"" + entryId + "\",\"closed\":true"));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), writer.expect("ERROR").getInt("returnCode"));

        // What isn't a request.
        writer.tell(frame("ADD", "\"feature\":\"Not A Feature\",\"kind\":\"note\",\"content\":\"\""));
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), writer.expect("ERROR").getInt("returnCode"));
        writer.tell(frame("CHANGE", "\"feature\":\"board\",\"entryId\":\"nope\",\"content\":\"\""));
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), writer.expect("ERROR").getInt("returnCode"));
        writer.tell(frame("ADDED", "\"feature\":\"board\""));
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), writer.expect("ERROR").getInt("returnCode"));
        writer.tell("this is not JSON");
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), writer.expect("ERROR").getInt("returnCode"));
        writer.tell("{\"type\":\"NOPE\"}");
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), writer.expect("ERROR").getInt("returnCode"));
        writer.tell(frame("LIST", "\"feature\":\"board\",\"limit\":\"ten\""));
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), writer.expect("ERROR").getInt("returnCode"));
        assertEquals(2, hub.connections());
    }

    @Test
    void testListingCountingAndReading() {
        final Peer writer = joined(MEMBER_2);
        final Peer reader = joined(MEMBER_3);
        for (int i = 1; i <= 3; i++) {
            writer.tell(frame("ADD", "\"feature\":\"board\",\"kind\":\"note\",\"content\":\"" + b64("note " + i) + "\""));
            writer.answer("ADDED");
            reader.expect("ADDED");
        }

        reader.tell(frame("LIST", "\"ref\":\"l\",\"feature\":\"board\",\"kind\":\"note\",\"oldestFirst\":true,\"after\":1,\"limit\":5"));
        final JsonObject page = reader.expect("ENTRIES");
        assertEquals("l", page.getString("ref"));
        assertEquals(2, page.getJsonArray("entries").size());
        assertEquals(b64("note 2"), page.getJsonArray("entries").getJsonObject(0).getString("content"));
        assertEquals(3, page.getJsonArray("entries").getJsonObject(1).getJsonNumber("position").longValue());

        reader.tell(frame("LIST", "\"feature\":\"board\",\"bare\":true,\"limit\":1"));
        final JsonObject bare = reader.expect("ENTRIES").getJsonArray("entries").getJsonObject(0);
        assertFalse(bare.containsKey("content"));

        reader.tell(frame("COUNT", "\"ref\":\"c\",\"feature\":\"board\",\"kind\":\"note\",\"after\":1"));
        final JsonObject counted = reader.expect("COUNTED");
        assertEquals("c", counted.getString("ref"));
        assertEquals(2, counted.getJsonNumber("count").longValue());

        reader.tell(frame("GET", "\"feature\":\"board\",\"entryId\":\"" + bare.getString("entryId") + "\""));
        assertEquals(b64("note 3"), reader.expect("DONE").getJsonArray("entries").getJsonObject(0).getString("content"));
    }

    @Test
    void testALongContentIsAnnouncedWithoutIt() {
        final Peer writer = joined(MEMBER_2);
        final Peer reader = joined(MEMBER_3);
        final String large = Base64.getEncoder().encodeToString(new byte[15000]);
        assertTrue(large.length() > Constants.MAX_ANNOUNCED_CONTENT);

        writer.tell(frame("ADD", "\"feature\":\"board\",\"kind\":\"big\",\"content\":\"" + large + "\""));
        writer.answer("ADDED");

        final JsonObject added = reader.expect("ADDED").getJsonObject("entry");
        assertFalse(added.containsKey("content"));
        // It is fetched instead.
        reader.tell(frame("GET", "\"feature\":\"board\",\"entryId\":\"" + added.getString("entryId") + "\""));
        assertEquals(large, reader.expect("DONE").getJsonArray("entries").getJsonObject(0).getString("content"));
    }

    // =========================================================================
    // Signals
    // =========================================================================

    @Test
    void testSignalsArePassedOnAndNotStored() {
        final Peer writer = joined(MEMBER_2);
        final Peer reader = joined(MEMBER_3);
        final Peer admin = joined(MEMBER_1);

        // Every Trustee may signal; the others hear it, the sender doesn't.
        reader.tell(frame("SIGNAL", "\"feature\":\"board\",\"signal\":\"typing\""));
        for (final Peer peer : new Peer[] { writer, admin }) {
            final JsonObject signal = peer.expect("SIGNAL");
            assertEquals("typing", signal.getString("signal"));
            assertEquals(MEMBER_3_ID, signal.getString("memberId"));
            assertEquals(MEMBER_3, signal.getString("accountName"));
            assertEquals(BOARD, signal.getString("feature"));
            assertEquals(CIRCLE_1_ID, signal.getString("circleId"));
        }
        // One which comes too soon is dropped; the next one after the interval isn't.
        reader.tell(frame("SIGNAL", "\"feature\":\"board\",\"signal\":\"too soon\""));
        // The frames of a connection are handled in order, so once the count
        // is answered, the signal before it was dropped; then the interval of
        // the board (500 ms) passes on the clock of the hub.
        reader.tell(frame("COUNT", "\"feature\":\"board\""));
        assertEquals(0, reader.expect("COUNTED").getJsonNumber("count").longValue());
        clock.advance(Duration.ofMillis(700));
        reader.tell(frame("SIGNAL", "\"feature\":\"board\",\"signal\":\"again\""));
        assertEquals("again", writer.expect("SIGNAL").getString("signal"));
        assertEquals("again", admin.expect("SIGNAL").getString("signal"));

        // Nothing was stored, and the sender got nothing back.
        reader.tell(frame("COUNT", "\"feature\":\"board\""));
        assertEquals(0, reader.expect("COUNTED").getJsonNumber("count").longValue());

        // Too long, and a feature which declares no signals.
        writer.tell(frame("SIGNAL", "\"feature\":\"board\",\"signal\":\"" + "x".repeat(Constants.MAX_SIGNAL_LENGTH + 1) + "\""));
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), writer.expect("ERROR").getInt("returnCode"));
        writer.tell(frame("SIGNAL", "\"feature\":\"quiet\",\"signal\":\"psst\""));
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), writer.expect("ERROR").getInt("returnCode"));
    }

    // =========================================================================
    // Who is present, and signals to one Member
    // =========================================================================

    @Test
    void testJoiningTellsWhoIsPresent() {
        final Peer first = authenticated(MEMBER_2);
        first.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals(List.of(MEMBER_2_ID), present(first.next("JOINED")));

        // The second Member gets the list, and the first is told.
        final Peer second = authenticated(MEMBER_3);
        second.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals(sorted(MEMBER_2_ID, MEMBER_3_ID), present(second.next("JOINED")));
        final JsonObject came = first.next("PRESENT");
        assertEquals(MEMBER_3_ID, came.getString("memberId"));
        assertEquals(MEMBER_3, came.getString("accountName"));
        assertEquals(CIRCLE_1_ID, came.getString("circleId"));

        // A further connection of a Member who is present changes nothing,
        // and neither does joining again.
        final Peer further = authenticated(MEMBER_3);
        further.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals(sorted(MEMBER_2_ID, MEMBER_3_ID), present(further.next("JOINED")));
        second.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        second.next("JOINED");
        // One of the two leaves: the Member is still there.
        second.tell("{\"type\":\"LEAVE\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        second.next("LEFT");
        first.tell(frame("COUNT", "\"feature\":\"board\""));
        first.next("COUNTED");

        // The last one leaves: the Member went.
        further.tell("{\"type\":\"LEAVE\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        further.next("LEFT");
        final JsonObject went = first.next("ABSENT");
        assertEquals(MEMBER_3_ID, went.getString("memberId"));
        assertEquals(MEMBER_3, went.getString("accountName"));
        assertEquals(CIRCLE_1_ID, went.getString("circleId"));

        // Who left hears no more of it; who comes back is told anew.
        second.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals(sorted(MEMBER_2_ID, MEMBER_3_ID), present(second.next("JOINED")));
        assertEquals(MEMBER_3_ID, first.next("PRESENT").getString("memberId"));
    }

    @Test
    void testAMemberGoesWithTheirLastConnection() throws InterruptedException {
        final Peer watcher = joined(MEMBER_1);
        final Peer closing = joined(MEMBER_2);
        final Peer removed = joined(MEMBER_3);
        final Peer loggedOut = joined(member5InCircle());
        for (final String memberId : new String[] { MEMBER_2_ID, MEMBER_3_ID, MEMBER_5_ID }) {
            assertEquals(memberId, watcher.next("PRESENT").getString("memberId"));
        }

        // The connection closes.
        hub.closed(closing.id());
        assertEquals(MEMBER_2_ID, watcher.next("ABSENT").getString("memberId"));

        // The access to the Circle is taken away.
        final ProcessTrusteeRequest remove = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        remove.setAction(Action.REMOVE);
        remove.setCircleId(CIRCLE_1_ID);
        remove.setMemberId(MEMBER_3_ID);
        assertTrue(prepareManagementBean(shared, settings).processTrustee(remove).isOk());
        shared.flush();
        hub.accessChanged(AccessChange.INSTANCE);
        assertEquals(MEMBER_3_ID, watcher.next("ABSENT").getString("memberId"));
        removed.expect("LEFT");

        // The Session ends.
        final ProcessMemberRequest logout = prepareSessionRequest(ProcessMemberRequest.class, loggedOut.token);
        logout.setAction(Action.LOGOUT);
        assertTrue(prepareManagementBean(shared, settings).processMember(logout).isOk());
        shared.flush();
        hub.accessChanged(AccessChange.INSTANCE);
        assertEquals(MEMBER_5_ID, watcher.next("ABSENT").getString("memberId"));
        assertTrue(loggedOut.closed.await(WAIT_SECONDS, TimeUnit.SECONDS));

        // Only the watcher is left.
        watcher.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals(List.of(MEMBER_1_ID), present(watcher.next("JOINED")));
    }

    @Test
    void testJoiningUnseen() {
        final Peer watcher = joined(MEMBER_2);
        final Peer unseen = authenticated(MEMBER_3);

        // Unseen, the Member isn't in the list and nobody is told; they see the others.
        unseen.tell("{\"type\":\"JOIN\",\"unseen\":true,\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals(List.of(MEMBER_2_ID), present(unseen.next("JOINED")));
        watcher.tell(frame("COUNT", "\"feature\":\"board\""));
        watcher.next("COUNTED");
        final Peer third = joined(MEMBER_1);
        assertEquals(MEMBER_1_ID, unseen.next("PRESENT").getString("memberId"));
        assertEquals(MEMBER_1_ID, watcher.next("PRESENT").getString("memberId"));

        // Joining again decides anew: shown, and unseen again.
        unseen.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals(sorted(MEMBER_1_ID, MEMBER_2_ID, MEMBER_3_ID), present(unseen.next("JOINED")));
        assertEquals(MEMBER_3_ID, watcher.next("PRESENT").getString("memberId"));
        assertEquals(MEMBER_3_ID, third.next("PRESENT").getString("memberId"));
        unseen.tell("{\"type\":\"JOIN\",\"unseen\":true,\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals(sorted(MEMBER_1_ID, MEMBER_2_ID), present(unseen.next("JOINED")));
        assertEquals(MEMBER_3_ID, watcher.next("ABSENT").getString("memberId"));

        // A connection which closes unseen tells nothing.
        hub.closed(unseen.id());
        watcher.tell(frame("COUNT", "\"feature\":\"board\""));
        watcher.next("COUNTED");
    }

    @Test
    void testASignalToOneMember() {
        final Peer sender = joined(MEMBER_2);
        final Peer other = joined(MEMBER_1);
        final Peer one = joined(MEMBER_3);
        final Peer unseen = authenticated(MEMBER_3);
        unseen.tell("{\"type\":\"JOIN\",\"unseen\":true,\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        unseen.expect("JOINED");
        final Peer own = joined(MEMBER_2);

        // Every connection of the Member gets it, shown or not; nobody else does.
        sender.tell(frame("SIGNAL", "\"feature\":\"fast\",\"to\":\"" + MEMBER_3_ID + "\",\"signal\":\"for you\""));
        for (final Peer peer : new Peer[] { one, unseen }) {
            final JsonObject signal = peer.expect("SIGNAL");
            assertEquals("for you", signal.getString("signal"));
            assertEquals(MEMBER_2_ID, signal.getString("memberId"));
            assertEquals(MEMBER_3_ID, signal.getString("to"));
        }
        // The sender's other connections, by naming the sender; not the sender's own.
        sender.tell(frame("SIGNAL", "\"feature\":\"fast\",\"to\":\"" + MEMBER_2_ID + "\",\"signal\":\"for me\""));
        assertEquals("for me", own.expect("SIGNAL").getString("signal"));

        // Without a least time between two, none is dropped; one for everyone has no "to".
        sender.tell(frame("SIGNAL", "\"feature\":\"fast\",\"signal\":\"for all\""));
        for (final Peer peer : new Peer[] { other, one, unseen, own }) {
            final JsonObject signal = peer.expect("SIGNAL");
            assertEquals("for all", signal.getString("signal"), "Only the signal for everyone reaches everyone.");
            assertFalse(signal.containsKey("to"));
        }
        sender.tell(frame("COUNT", "\"feature\":\"board\""));
        sender.expect("COUNTED");

        // A Member who isn't there gets nothing, and it isn't an error; a "to" which is no ID is one.
        sender.tell(frame("SIGNAL", "\"feature\":\"fast\",\"to\":\"" + MEMBER_5_ID + "\",\"signal\":\"nobody\""));
        sender.tell(frame("SIGNAL", "\"feature\":\"fast\",\"to\":\"someone\",\"signal\":\"x\""));
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), sender.expect("ERROR").getInt("returnCode"));
    }

    // =========================================================================
    // What else is told
    // =========================================================================

    @Test
    void testChangesByOtherRequestsAreToldToo() {
        final Peer reader = joined(MEMBER_3);

        // An entry added through the resource API.
        final net.haugr.eds.api.requests.EntryRequest body = new net.haugr.eds.api.requests.EntryRequest();
        body.setKind("note");
        body.setContent(b64("by REST"));
        assertTrue(features.addEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, body).isOk());
        assertEquals(b64("by REST"), reader.expect("ADDED").getJsonObject("entry").getString("content"));

        // A feature switched.
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.READONLY).isOk());
        final JsonObject feature = reader.expect("FEATURE");
        assertEquals(BOARD, feature.getString("feature"));
        assertEquals(CIRCLE_1_ID, feature.getString("circleId"));
        assertFalse(feature.containsKey("entry"));

        // The files of the Circle.
        hub.dataChanged(new DataChange(CIRCLE_1_ID, MEMBER_1_ID));
        final JsonObject files = reader.expect("FILES");
        assertEquals(CIRCLE_1_ID, files.getString("circleId"));
        assertEquals(MEMBER_1_ID, files.getString("dataId"));
        hub.dataChanged(new DataChange(CIRCLE_2_ID, MEMBER_1_ID));
    }

    @Test
    void testLeavingEndsTheAnnouncements() {
        final Peer writer = joined(MEMBER_2);
        final Peer reader = joined(MEMBER_3);

        reader.tell("{\"type\":\"LEAVE\",\"ref\":\"bye\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals("bye", reader.expect("LEFT").getString("ref"));
        writer.tell(frame("ADD", "\"feature\":\"board\",\"kind\":\"note\",\"content\":\"" + b64("unheard") + "\""));
        writer.answer("ADDED");

        // The next thing the reader hears is its own answer.
        reader.tell("{\"type\":\"JOIN\",\"ref\":\"back\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals("back", reader.expect("JOINED").getString("ref"));
    }

    @Test
    void testAccessEndsPromptly() throws InterruptedException {
        final Peer writer = joined(MEMBER_2);
        final Peer reader = joined(MEMBER_3);
        final Peer gone = joined(member5InCircle());

        // member3 is removed from the Circle.
        final ProcessTrusteeRequest remove = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        remove.setAction(Action.REMOVE);
        remove.setCircleId(CIRCLE_1_ID);
        remove.setMemberId(MEMBER_3_ID);
        assertTrue(prepareManagementBean(shared, settings).processTrustee(remove).isOk());
        // In the stone the hub is told after the commit; here the change
        // is flushed, so the hub's thread reads it.
        shared.flush();
        hub.accessChanged(AccessChange.INSTANCE);

        final JsonObject left = reader.expect("LEFT");
        assertEquals(CIRCLE_1_ID, left.getString("circleId"));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), left.getInt("returnCode"));
        reader.tell(frame("LIST", "\"feature\":\"board\""));
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), reader.expect("ERROR").getInt("returnCode"));

        // A Session which ended closes its connection.
        final ProcessMemberRequest logout = prepareSessionRequest(ProcessMemberRequest.class, gone.token);
        logout.setAction(Action.LOGOUT);
        assertTrue(prepareManagementBean(shared, settings).processMember(logout).isOk());
        shared.flush();
        hub.accessChanged(AccessChange.INSTANCE);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), gone.expect("ERROR").getInt("returnCode"));
        assertTrue(gone.closed.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(SocketConnection.POLICY_VIOLATION, gone.code.get());

        // The others go on.
        writer.tell(frame("COUNT", "\"feature\":\"board\""));
        writer.expect("COUNTED");
    }

    @Test
    void testAConnectionWhichDoesNotReadIsClosed() throws InterruptedException {
        final Peer deaf = new Peer(false);
        hub.open(deaf);
        // The answer to the first frame blocks the connection.
        hub.receive(deaf.id(), "not a frame");
        assertTrue(deaf.sending.await(WAIT_SECONDS, TimeUnit.SECONDS));
        for (int i = 0; i <= SocketHub.MAX_PENDING_FRAMES; i++) {
            hub.receive(deaf.id(), "not a frame");
        }

        assertTrue(deaf.closed.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(SocketConnection.TRY_AGAIN_LATER, deaf.code.get());
        assertEquals(0, hub.connections());
    }

    @Test
    void testAnEntryAboutAnotherIsAddedAndListed() {
        final Peer writer = joined(MEMBER_2);
        final String noteId = added(writer);

        writer.tell(frame("ADD", "\"feature\":\"board\",\"kind\":\"reply\",\"about\":\"" + noteId + "\",\"content\":\"" + b64("a reply") + "\""));
        final JsonObject reply = writer.answer("ADDED").getJsonArray("entries").getJsonObject(0);
        assertEquals(noteId, reply.getString("about"));

        // Listed by what it is about; an about which is no ID is refused.
        writer.tell(frame("LIST", "\"feature\":\"board\",\"about\":\"" + noteId + "\""));
        final JsonObject page = writer.expect("ENTRIES");
        assertEquals(1, page.getJsonArray("entries").size());
        assertEquals(reply.getString("entryId"), page.getJsonArray("entries").getJsonObject(0).getString("entryId"));
        writer.tell(frame("LIST", "\"feature\":\"board\",\"about\":\"nope\""));
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), writer.expect("ERROR").getInt("returnCode"));
    }

    // =========================================================================
    // What a frame lacks, and what fails
    // =========================================================================

    @Test
    void testAFrameMustNameWhatItIsAbout() {
        final Peer peer = open();

        // An AUTH frame without a token, which may be tried again.
        peer.tell("{\"type\":\"AUTH\"}");
        final JsonObject refused = peer.expect("ERROR");
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), refused.getInt("returnCode"));
        assertEquals("The AUTH frame has no token.", refused.getString("returnMessage"));
        // The URL safe alphabet, known by an underscore alone.
        peer.tell("{\"type\":\"AUTH\",\"token\":\"____\"}");
        assertNotEquals("The token is not Base64.", peer.expect("ERROR").getString("returnMessage"));

        // A JOIN without a Circle.
        peer.token = session(MEMBER_3);
        peer.tell("{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(peer.token) + "\"}");
        peer.expect("READY");
        peer.tell("{\"type\":\"JOIN\",\"ref\":\"j\"}");
        final JsonObject noCircle = peer.expect("ERROR");
        assertEquals("j", noCircle.getString("ref"));
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), noCircle.getInt("returnCode"));
        assertEquals(1, hub.connections());
    }

    @Test
    void testAFrameOfAnUnknownConnectionIsIgnored() {
        final Peer peer = authenticated(MEMBER_3);

        assertDoesNotThrow(() -> hub.receive("unknown", "{\"type\":\"AUTH\"}"));
        assertEquals(1, hub.connections());
        // The known connection goes on, and heard nothing in between.
        peer.tell("{\"type\":\"JOIN\",\"ref\":\"j\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals("j", peer.expect("JOINED").getString("ref"));
    }

    @Test
    void testAFailureOfTheStoneIsAnswered() {
        replace(new SocketHub(new FeatureBean(shared, settings) {
            @Override
            public CircleFeatureResponse circleFeatures(final byte[] token, final String circleId) {
                throw new IllegalStateException("The database is gone.");
            }
        }, 200, clock, SocketHub.AUTH_TIMEOUT_SECONDS * 1000));
        final Peer peer = authenticated(MEMBER_3);

        peer.tell("{\"type\":\"JOIN\",\"ref\":\"j\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        final JsonObject failed = peer.expect("ERROR");
        assertEquals("j", failed.getString("ref"));
        assertEquals(ReturnCode.ERROR.getCode(), failed.getInt("returnCode"));
        assertEquals("The database is gone.", failed.getString("returnMessage"));
        assertEquals(1, hub.connections());
    }

    @Test
    void testASessionWithoutAnExpiryIsTakenAsExpired() throws InterruptedException {
        replace(new SocketHub(new FeatureBean(shared, settings) {
            @Override
            public FeatureManager.Identity identify(final byte[] token) {
                return new FeatureManager.Identity(MEMBER_3_ID, MEMBER_3, null);
            }
        }, 200, clock, SocketHub.AUTH_TIMEOUT_SECONDS * 1000));
        final Peer peer = open();

        peer.tell("{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(new byte[32]) + "\"}");
        assertEquals(MEMBER_3_ID, peer.expect("READY").getString("memberId"));
        peer.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertExpired(peer);
        assertEquals(0, hub.connections());
    }

    @Test
    void testTheSweepKeepsSessionsWhichHaveNotExpired() {
        final Peer peer = joined(MEMBER_3);
        // Nor does it close one which hasn't authenticated yet.
        final Peer waiting = open();

        hub.closeExpired();
        assertEquals(2, hub.connections());
        peer.tell(frame("COUNT", "\"feature\":\"board\""));
        assertEquals(0, peer.expect("COUNTED").getJsonNumber("count").longValue());
        waiting.tell("{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(session(MEMBER_2)) + "\"}");
        assertEquals(MEMBER_2_ID, waiting.expect("READY").getString("memberId"));
    }

    @Test
    void testOnlyConnectionsWithoutAuthAreClosedAtTheDeadline() throws InterruptedException {
        // The timer runs one task after the other, in the order of their
        // time: when the last connection is closed, the deadlines of those
        // before it have passed.
        replace(new SocketHub(features, 200, clock, 2000));
        final byte[] token = session(MEMBER_3);
        final Peer authenticating = open();
        authenticating.tell("{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(token) + "\"}");
        authenticating.expect("READY");
        final Peer leaving = open();
        hub.closed(leaving.id());
        final Peer silent = open();

        assertTrue(silent.expect("ERROR").getString("returnMessage").startsWith("No AUTH frame within"));
        assertTrue(silent.closed.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(1, hub.connections());
        // The authenticated one heard nothing of it; the closed one neither.
        authenticating.tell("{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(token) + "\"}");
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), authenticating.expect("ERROR").getInt("returnCode"));
        assertTrue(leaving.frames.isEmpty());
    }

    @Test
    void testAnAccessCheckPassesOverConnectionsWithoutAuth() throws InterruptedException {
        final Peer waiting = open();
        final Peer gone = joined(member5InCircle());
        final ProcessMemberRequest logout = prepareSessionRequest(ProcessMemberRequest.class, gone.token);
        logout.setAction(Action.LOGOUT);
        assertTrue(prepareManagementBean(shared, settings).processMember(logout).isOk());
        shared.flush();

        hub.accessChanged(AccessChange.INSTANCE);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), gone.expect("ERROR").getInt("returnCode"));
        assertTrue(gone.closed.await(WAIT_SECONDS, TimeUnit.SECONDS));

        // The connection which hadn't authenticated yet still may.
        waiting.tell("{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(session(MEMBER_3)) + "\"}");
        assertEquals(MEMBER_3_ID, waiting.expect("READY").getString("memberId"));
        assertEquals(1, hub.connections());
    }

    @Test
    void testAFailedAccessCheckEndsOnlyAConnectionWhichLostItsAccess() throws InterruptedException {
        final AtomicReference<RuntimeException> failure = new AtomicReference<>();
        final ManualWork work = new ManualWork();
        replace(new SocketHub(new FeatureBean(shared, settings) {
            @Override
            public Set<String> readableCircles(final byte[] token) {
                throw failure.get();
            }
        }, new Given<>(work)));
        final Peer peer = open();
        peer.token = session(MEMBER_3);
        final String auth = "{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(peer.token) + "\"}";
        peer.tell(auth);
        work.drain();
        peer.expect("READY");

        // A check which fails for another reason keeps the connection.
        for (final RuntimeException kept : new RuntimeException[] {
                new EDSException(ReturnCode.DATABASE_ERROR, "The database is busy."), new IllegalStateException("Something broke.") }) {
            failure.set(kept);
            hub.accessChanged(AccessChange.INSTANCE);
            work.next().run();
            peer.tell(auth);
            work.drain();
            assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), peer.expect("ERROR").getInt("returnCode"), kept.getMessage());
        }

        // One which finds the access gone ends it.
        failure.set(new EDSException(ReturnCode.AUTHORIZATION_WARNING, "No more."));
        hub.accessChanged(AccessChange.INSTANCE);
        work.next().run();
        final JsonObject ended = peer.expect("ERROR");
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), ended.getInt("returnCode"));
        assertEquals("No more.", ended.getString("returnMessage"));
        assertTrue(peer.closed.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(0, hub.connections());
    }

    @Test
    void testAConnectionWhichClosedWhileItsJoinWaitedJoinsNothing() {
        final ManualWork work = manualWork();
        final Peer closing = open();
        closing.tell("{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(session(MEMBER_3)) + "\"}");
        closing.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");

        // Closed before its frames are handled, which still are.
        hub.closed(closing.id());
        work.drain();
        assertEquals(0, hub.connections());

        // Nobody else sees it present.
        final Peer watcher = open();
        watcher.tell("{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(session(MEMBER_1)) + "\"}");
        watcher.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        work.drain();
        watcher.expect("READY");
        assertEquals(List.of(MEMBER_1_ID), present(watcher.next("JOINED")));
        assertTrue(closing.frames.isEmpty());
    }

    @Test
    void testWhatWaitsForAConnectionWhichClosedDoesNothing() {
        final ManualWork work = manualWork();
        final Peer joining = open();
        final Peer checked = open();
        for (final Peer peer : new Peer[] { joining, checked }) {
            peer.tell("{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(session(MEMBER_3)) + "\"}");
            work.drain();
            peer.expect("READY");
        }

        // A frame waits while its connection closes: the token is gone, so
        // it asks for nothing.
        joining.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        final Runnable join = work.next();
        hub.closed(joining.id());
        join.run();
        // And so does a check of the access.
        hub.accessChanged(AccessChange.INSTANCE);
        final Runnable check = work.next();
        hub.closed(checked.id());
        check.run();
        work.drain();

        assertEquals(0, hub.connections());
        assertTrue(joining.frames.isEmpty());
        assertTrue(checked.frames.isEmpty());
    }

    @Test
    void testAFrameWhichTheExecutorRefusesIsDropped() {
        final ManualWork work = manualWork();
        final Peer peer = open();
        final String auth = "{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(session(MEMBER_3)) + "\"}";

        work.refusing = true;
        peer.tell(auth);
        work.refusing = false;
        peer.tell(auth);
        work.drain();

        // Only the second AUTH was handled: the next one is refused as a repetition.
        peer.expect("READY");
        peer.tell(auth);
        work.drain();
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), peer.expect("ERROR").getInt("returnCode"));
    }

    @Test
    void testTheHubRunsWithOrWithoutAnExecutorOfTheRuntime() {
        // Without one, each frame gets a virtual thread.
        replace(new SocketHub(features, new Given<>(null)));
        assertJoins(authenticated(MEMBER_3));
        replace(new SocketHub(features, 200));
        assertJoins(authenticated(MEMBER_2));
    }

    private static void assertJoins(final Peer peer) {
        peer.tell("{\"type\":\"JOIN\",\"ref\":\"j\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        assertEquals("j", peer.expect("JOINED").getString("ref"));
    }

    @Test
    void testAHubOutsideCdiDoesNothing() {
        final SocketHub proxy = new SocketHub();

        assertDoesNotThrow(() -> proxy.accessChanged(AccessChange.INSTANCE));
        assertDoesNotThrow(proxy::shutdown);
        assertEquals(0, proxy.connections());
    }

    @Test
    void testAnAccessChangeAfterTheShutdownIsNotChecked() {
        final Peer peer = authenticated(MEMBER_3);
        hub.shutdown();

        assertDoesNotThrow(() -> hub.accessChanged(AccessChange.INSTANCE));
        assertDoesNotThrow(() -> hub.accessChanged(AccessChange.INSTANCE));
        assertEquals(0, hub.connections());
        assertTrue(peer.frames.isEmpty());
    }

    @Test
    void testAConnectionWhichSendsTooMuchIsClosed() throws InterruptedException {
        final Peer flooding = authenticated(MEMBER_3);

        hub.receive(flooding.id(), "x".repeat((int) SocketHub.MAX_PENDING_RECEIVED + 1));
        assertTrue(flooding.closed.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(SocketConnection.TRY_AGAIN_LATER, flooding.code.get());
        assertEquals(0, hub.connections());
    }

    @Test
    void testAFailedSendDoesNotEndTheConnection() {
        final Peer failing = new Peer(true, true);
        hub.open(failing);
        final String auth = "{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(session(MEMBER_3)) + "\"}";

        failing.tell(auth);
        failing.expect("READY");
        // The next answer is sent all the same.
        failing.tell(auth);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), failing.expect("ERROR").getInt("returnCode"));
        assertEquals(1, hub.connections());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    /** member5 isn't in circle1 in the test data; here it is added, to read. */
    private String member5InCircle() {
        final ProcessTrusteeRequest add = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        add.setAction(Action.ADD);
        add.setCircleId(CIRCLE_1_ID);
        add.setMemberId(MEMBER_5_ID);
        add.setTrustLevel(net.haugr.eds.api.common.TrustLevel.READ);
        assertTrue(prepareManagementBean(shared, settings).processTrustee(add).isOk());
        return MEMBER_5;
    }

    /** The hub is replaced by another one, which the features tell too. */
    private void replace(final SocketHub other) {
        hub.shutdown();
        hub = other;
        listening.set(other);
    }

    /** The hub is replaced by one whose frames are handled when the test says so. */
    private ManualWork manualWork() {
        final ManualWork work = new ManualWork();
        replace(new SocketHub(features, new Given<>(work)));
        return work;
    }

    private Peer open() {
        final Peer peer = new Peer(true);
        hub.open(peer);
        return peer;
    }

    private Peer authenticated(final String account) {
        final Peer peer = open();
        peer.token = session(account);
        peer.tell("{\"type\":\"AUTH\",\"token\":\"" + Base64.getEncoder().encodeToString(peer.token) + "\"}");
        peer.expect("READY");
        return peer;
    }

    private Peer joined(final String account) {
        final Peer peer = authenticated(account);
        peer.tell("{\"type\":\"JOIN\",\"circleId\":\"" + CIRCLE_1_ID + "\"}");
        peer.expect("JOINED");
        return peer;
    }

    /** The writer adds a note, of which the others are told; its id. */
    private static String added(final Peer writer, final Peer... others) {
        writer.tell(frame("ADD", "\"feature\":\"board\",\"kind\":\"note\",\"content\":\"" + b64("hello") + "\""));
        final String entryId = writer.answer("ADDED").getJsonArray("entries").getJsonObject(0).getString("entryId");
        for (final Peer peer : others) {
            peer.expect("ADDED");
        }
        return entryId;
    }

    /** The Members a JOINED frame names as present. */
    private static List<String> present(final JsonObject joined) {
        return joined.getJsonArray("present").getValuesAs(JsonString.class).stream().map(JsonString::getString).toList();
    }

    private static List<String> sorted(final String... memberIds) {
        return Arrays.stream(memberIds).sorted().toList();
    }

    private static String frame(final String type, final String fields) {
        return "{\"type\":\"" + type + "\",\"circleId\":\"" + CIRCLE_1_ID + "\"," + fields + "}";
    }

    private static String b64(final String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] session(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        final ProcessMemberResponse response = prepareManagementBean(shared, settings).processMember(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getSession();
    }

    /** A connection in memory, which keeps what it is sent. */
    private final class Peer implements SocketConnection {

        private final String id = "peer-" + ids.incrementAndGet();
        private final boolean reads;
        private final boolean fails;
        private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        private final CountDownLatch sending = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final AtomicInteger code = new AtomicInteger();
        private byte[] token = null;

        private Peer(final boolean reads) {
            this(reads, false);
        }

        /** A peer which fails to send keeps what it was to be sent all the same. */
        private Peer(final boolean reads, final boolean fails) {
            this.reads = reads;
            this.fails = fails;
        }

        /** Sends a frame to the hub, as a client would. */
        private void tell(final String text) {
            hub.receive(id, text);
        }

        /**
         * The answer to a request which changed something. The change is
         * announced when it is committed, which is before the answer is
         * sent, so the one who asked hears the announcement first.
         */
        private JsonObject answer(final String announcement) {
            expect(announcement);
            return expect("DONE");
        }

        /**
         * The next frame, which must be of the given type. Who came and
         * went is passed over, as most tests aren't about it; those which
         * are use {@link #next(String)}.
         */
        private JsonObject expect(final String type) {
            for (;;) {
                final JsonObject frame = next(null);
                final String found = frame.getString("type");
                if (type.equals(found) || !("PRESENT".equals(found) || "ABSENT".equals(found))) {
                    assertEquals(type, found, frame.toString());
                    return frame;
                }
            }
        }

        /** The very next frame, which must be of the given type, if one is given. */
        private JsonObject next(final String type) {
            final String text;
            try {
                text = frames.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            assertNotNull(text, "No frame came, a " + type + " was expected.");
            try (JsonReader reader = Json.createReader(new StringReader(text))) {
                final JsonObject frame = reader.readObject();
                if (type != null) {
                    assertEquals(type, frame.getString("type"), text);
                }
                return frame;
            }
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public void send(final String text) throws IOException {
            frames.add(text);
            sending.countDown();
            if (fails) {
                throw new IOException("The peer is gone.");
            }
            if (!reads) {
                // A peer which doesn't read: the send blocks until the
                // connection is closed.
                try {
                    closed.await();
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public void close(final int closeCode, final String reason) {
            code.set(closeCode);
            closed.countDown();
        }
    }

    /** A clock which the test moves forward. */
    private static final class MovingClock extends Clock {

        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.now());

        private void advance(final Duration duration) {
            now.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    /** An Executor whose tasks run when the test runs them, on its thread. */
    private static final class ManualWork implements Executor {

        private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
        private volatile boolean refusing = false;

        @Override
        public void execute(final Runnable task) {
            if (refusing) {
                throw new RejectedExecutionException("The pool is gone.");
            }
            tasks.add(task);
        }

        /** The next task, which may be given by another thread. */
        private Runnable next() {
            final Runnable task;
            try {
                task = tasks.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            assertNotNull(task, "No task came.");
            return task;
        }

        /** Runs the tasks until none waits, also those which they give. */
        private void drain() {
            for (Runnable task = tasks.poll(); task != null; task = tasks.poll()) {
                task.run();
            }
        }
    }
}
