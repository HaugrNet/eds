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
package net.haugr.eds.quarkus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>The features and the socket (FEATURES.md) in the running stone, with real
 * WebSockets: a feature is declared and switched on with the resource API,
 * two connections work with its entries, and both are told what changed,
 * whoever changed it and by which way. This is where the wiring is proven:
 * the endpoint, the transactions, and that nothing is told before it is
 * committed.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@QuarkusTest
final class EdsSocketTest {

    private static final String ADMIN_CREDENTIAL = encode("Admin-Passphrase-For-Testing-Only");
    private static final String MEMBER_CREDENTIAL = encode("socket member pjvgr.yxupm-37i23.8kq4h_9s");
    private static final long TIMEOUT_SECONDS = 10;
    private static final String DECLARATION = """
            {"feature":"notes","version":1,"signals":0,"kinds":{\
            "note":{"add":"WRITE","change":["author"],"remove":["author","administrators"],"close":["administrators"],"size":1000},\
            "seen":{"add":"READ","change":["author"],"keep":"member","size":32}}}""";

    @TestHTTPResource("api/v1/socket")
    URI socketUri;

    @BeforeEach
    void apiRoot() {
        // The REST API is below Constants.REST_API; Quarkus only knows the root
        // of the application.
        RestAssured.basePath = Constants.REST_API;
    }

    @Test
    void testFeaturesOverRestAndSocket() throws Exception {
        final String admin = login("admin", ADMIN_CREDENTIAL);
        final String aliceId = createMember(admin, "socket-alice");
        final String bobId = createMember(admin, "socket-bob");
        final String alice = login("socket-alice", MEMBER_CREDENTIAL);
        final String bob = login("socket-bob", MEMBER_CREDENTIAL);
        final String circleId = offerAndSwitchOn(admin, alice, bob, bobId);
        final Scene scene = new Scene(aliceId, alice, bobId, bob, circleId);

        // The steps of the story, one after the other on the same two connections.
        try (Client a = connect(); Client b = connect()) {
            authenticateBoth(scene, a, b);
            joinBoth(scene, a, b);
            final String noteId = addNote(scene, a, b);
            readerAndRollback(scene, a, b);
            signals(scene, a, b);
            changeAndClose(scene, a, b, noteId);
            pages(scene, b, noteId);
            filesAndSwitchedOff(scene, a, b, noteId);
            removedAndLoggedOut(scene, a, b);
            assertNull(a.frames.poll(300, TimeUnit.MILLISECONDS));
        }

        removeFeatureAndScene(scene, admin);
    }

    /** Both authenticate; Alice's answer carries her ID and the reference of her request. */
    private static void authenticateBoth(final Scene scene, final Client a, final Client b) {
        final JsonPath ready = a.expect(a.send("""
                {"type": "AUTH", "token": "%s", "ref": "1"}
                """.formatted(scene.alice())), "READY");
        assertEquals(scene.aliceId(), ready.getString("memberId"));
        assertEquals("1", ready.getString("ref"));
        b.expect(b.send("""
                {"type": "AUTH", "token": "%s"}
                """.formatted(scene.bob())), "READY");
    }

    /**
     * The feature is removed from the Circle with its entries, and the stone
     * stops offering it; then the Circle and the two Members go.
     */
    private static void removeFeatureAndScene(final Scene scene, final String admin) {
        final String circleId = scene.circleId();
        as(admin).when().delete("/features/{feature}", "notes").then().statusCode(ReturnCode.ILLEGAL_ACTION.getHttpCode());
        as(scene.alice()).when().delete("/circles/{circleId}/features/{feature}", circleId, "notes").then().statusCode(200);
        as(scene.alice()).when().get("/circles/{circleId}/features", circleId).then().statusCode(200).body("features.size()", equalTo(0));
        as(admin).when().delete("/features/{feature}", "notes").then().statusCode(200);
        as(scene.alice()).when().delete("/circles/{circleId}", circleId).then().statusCode(200);
        as(admin).when().delete("/members/{memberId}", scene.aliceId()).then().statusCode(200);
        as(admin).when().delete("/members/{memberId}", scene.bobId()).then().statusCode(200);
    }

    /** Who and where the steps of {@link #testFeaturesOverRestAndSocket()} are. */
    private record Scene(String aliceId, String alice, String bobId, String bob, String circleId) {
    }

    /** The stone offers the feature, Alice makes a Circle which Bob may read, and she switches it on; the Circle. */
    private static String offerAndSwitchOn(final String admin, final String alice, final String bob, final String bobId) {
        // The stone offers the feature; only the System Administrator decides that.
        as(alice).body(declaration()).when().put("/features/{feature}", "notes")
                .then().statusCode(ReturnCode.AUTHORIZATION_WARNING.getHttpCode());
        as(admin).body(declaration()).when().put("/features/{feature}", "notes")
                .then().statusCode(200).body("features[0].name", equalTo("notes"));
        as(bob).when().get("/features").then().statusCode(200).body("features[0].version", equalTo(1));

        // Alice's Circle, Bob may read it; she switches the feature on.
        final String circleId = as(alice).body("""
                        {"circleName": "Socket Circle"}
                        """).when().post("/circles")
                .then().statusCode(200).extract().path("circleId");
        as(alice).body("""
                        {"memberId": "%s", "trustLevel": "READ"}
                        """.formatted(bobId)).when().post("/circles/{circleId}/trustees", circleId)
                .then().statusCode(200);
        as(bob).body("""
                        {"state": "ON"}
                        """).when().put("/circles/{circleId}/features/{feature}", circleId, "notes")
                .then().statusCode(ReturnCode.AUTHORIZATION_WARNING.getHttpCode());
        as(alice).body("""
                        {"state": "ON"}
                        """).when().put("/circles/{circleId}/features/{feature}", circleId, "notes")
                .then().statusCode(200).body("features[0].state", equalTo("ON"));
        return circleId;
    }

    /** Both join; Bob is told who is present, Alice that he came. */
    private static void joinBoth(final Scene scene, final Client a, final Client b) {
        final String circleId = scene.circleId();
        final String aliceId = scene.aliceId();
        final String bobId = scene.bobId();
        final JsonPath joined = a.expect(a.send(join(circleId)), "JOINED");
        assertEquals("notes", joined.getString("features[0].name"));
        assertEquals("ON", joined.getString("features[0].state"));
        assertEquals(List.of(aliceId), joined.getList("present"));
        // Bob joins: he gets who is present, and Alice is told that he came.
        final JsonPath second = b.expect(b.send(join(circleId)), "JOINED");
        assertEquals(Stream.of(aliceId, bobId).sorted().toList(), second.getList("present"));
        final JsonPath came = a.expect(a.next(), "PRESENT");
        assertEquals(bobId, came.getString("memberId"));
        assertEquals("socket-bob", came.getString("accountName"));
        assertEquals(circleId, came.getString("circleId"));
    }

    /** Alice adds a note; the note's ID. */
    private static String addNote(final Scene scene, final Client a, final Client b) {
        final String circleId = scene.circleId();
        // Alice adds a note on the socket. The change is announced when
        // it is committed, so she hears of it before her answer; Bob
        // hears of it too.
        final JsonPath announced = a.expect(a.send("""
                {"type": "ADD", "circleId": "%s", "feature": "notes", "kind": "note", "content": "%s", "ref": "2"}
                """.formatted(circleId, encode("Bring wine"))), "ADDED");
        final JsonPath done = a.expect(a.next(), "DONE");
        assertEquals("2", done.getString("ref"));
        final String noteId = done.getString("entries[0].entryId");
        assertNotNull(noteId);
        assertEquals(noteId, announced.getString("entry.entryId"));
        final JsonPath heard = b.expect(b.next(), "ADDED");
        assertEquals(noteId, heard.getString("entry.entryId"));
        assertEquals(encode("Bring wine"), heard.getString("entry.content"));
        assertEquals("socket-alice", heard.getString("entry.accountName"));
        assertEquals(scene.aliceId(), heard.getString("entry.memberId"));
        assertEquals(1, heard.getInt("entry.position"));
        return noteId;
    }

    private static void readerAndRollback(final Scene scene, final Client a, final Client b) {
        final String circleId = scene.circleId();
        final String bob = scene.bob();
        // Bob may only read: no note from him, but his own marker, which
        // the kind allows with READ. This one comes through the resource
        // API, and both connections are told all the same.
        final JsonPath refused = b.expect(b.send("""
                {"type": "ADD", "circleId": "%s", "feature": "notes", "kind": "note", "content": "", "ref": "3"}
                """.formatted(circleId)), "ERROR");
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), refused.getInt("returnCode"));
        assertEquals("3", refused.getString("ref"));
        as(bob).body("""
                        {"kind": "seen", "content": "%s"}
                        """.formatted(encode("1"))).when().post("/circles/{circleId}/features/{feature}/entries", circleId, "notes")
                .then().statusCode(200).body("entries[0].position", equalTo(2));
        assertEquals("seen", a.expect(a.next(), "ADDED").getString("entry.kind"));
        assertEquals("socket-bob", b.expect(b.next(), "ADDED").getString("entry.accountName"));

        // A request which fails is rolled back, and nobody hears of it.
        as(bob).body("""
                        {"kind": "note", "content": ""}
                        """).when().post("/circles/{circleId}/features/{feature}/entries", circleId, "notes")
                .then().statusCode(ReturnCode.AUTHORIZATION_WARNING.getHttpCode());
    }

    private static void signals(final Scene scene, final Client a, final Client b) {
        final String circleId = scene.circleId();
        // A signal reaches the other, not the sender, and isn't stored.
        b.post("""
                {"type": "SIGNAL", "circleId": "%s", "feature": "notes", "signal": "reading"}
                """.formatted(circleId));
        final JsonPath signal = a.expect(a.next(), "SIGNAL");
        assertEquals("reading", signal.getString("signal"));
        assertEquals(scene.bobId(), signal.getString("memberId"));
        assertNull(signal.getString("to"));
        // And one for a single Member says so.
        b.post("""
                {"type": "SIGNAL", "circleId": "%s", "feature": "notes", "to": "%s", "signal": "for you"}
                """.formatted(circleId, scene.aliceId()));
        final JsonPath directed = a.expect(a.next(), "SIGNAL");
        assertEquals("for you", directed.getString("signal"));
        assertEquals(scene.aliceId(), directed.getString("to"));
        assertEquals(2, b.expect(b.send("""
                {"type": "COUNT", "circleId": "%s", "feature": "notes"}
                """.formatted(circleId)), "COUNTED").getInt("count"));
    }

    private static void changeAndClose(final Scene scene, final Client a, final Client b, final String noteId) {
        final String circleId = scene.circleId();
        final String alice = scene.alice();
        // Alice changes her note, and closes it as the administrator.
        a.expect(a.send("""
                {"type": "CHANGE", "circleId": "%s", "feature": "notes", "entryId": "%s", "content": "%s"}
                """.formatted(circleId, noteId, encode("Bring two"))), "CHANGED");
        a.expect(a.next(), "DONE");
        assertEquals(encode("Bring two"), b.expect(b.next(), "CHANGED").getString("entry.content"));
        as(alice).body("""
                        {"closed": true}
                        """).when().put("/circles/{circleId}/features/{feature}/entries/{entryId}/closed", circleId, "notes", noteId)
                .then().statusCode(200).body("entries[0].closed", equalTo(true));
        assertTrue(a.expect(a.next(), "CHANGED").getBoolean("entry.closed"));
        b.expect(b.next(), "CHANGED");
        as(alice).when().delete("/circles/{circleId}/features/{feature}/entries/{entryId}", circleId, "notes", noteId)
                .then().statusCode(ReturnCode.ILLEGAL_ACTION.getHttpCode());
    }

    private static void pages(final Scene scene, final Client b, final String noteId) {
        final String circleId = scene.circleId();
        final String bob = scene.bob();
        // The page of entries, on the socket and over REST.
        final JsonPath page = b.expect(b.send("""
                {"type": "LIST", "circleId": "%s", "feature": "notes", "oldestFirst": true}
                """.formatted(circleId)), "ENTRIES");
        assertEquals(2, page.getList("entries").size());
        assertEquals(encode("Bring two"), page.getString("entries[0].content"));
        as(bob).when().get("/circles/{circleId}/features/{feature}/entries?kind=note&content=false", circleId, "notes")
                .then().statusCode(200).body("entries[0].entryId", equalTo(noteId)).body("entries[0].content", equalTo(null));
        as(bob).when().get("/circles/{circleId}/features/{feature}/count?kind=seen", circleId, "notes")
                .then().statusCode(200).body("count", equalTo(1));
        // An entry is a Data Object too: its content is read like a file's.
        final byte[] content = as(bob).when().get("/data/{dataId}/content", noteId).then().statusCode(200).extract().asByteArray();
        assertEquals("Bring two", new String(content, StandardCharsets.UTF_8));
    }

    private static void filesAndSwitchedOff(final Scene scene, final Client a, final Client b, final String noteId) {
        final String circleId = scene.circleId();
        final String alice = scene.alice();
        // The files of the Circle changed.
        as(alice).body("""
                        {"circleId": "%s", "dataName": "a file", "data": "%s"}
                        """.formatted(circleId, encode("content"))).when().post("/data")
                .then().statusCode(200);
        assertEquals(circleId, a.expect(a.next(), "FILES").getString("circleId"));
        b.expect(b.next(), "FILES");

        // Switched off, the feature is hidden, and those who joined are told.
        as(alice).body("""
                        {"state": "OFF"}
                        """).when().put("/circles/{circleId}/features/{feature}", circleId, "notes")
                .then().statusCode(200);
        assertEquals("notes", a.expect(a.next(), "FEATURE").getString("feature"));
        b.expect(b.next(), "FEATURE");
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), b.expect(b.send("""
                {"type": "LIST", "circleId": "%s", "feature": "notes"}
                """.formatted(circleId)), "ERROR").getInt("returnCode"));
        as(scene.bob()).when().get("/data/{dataId}/content", noteId).then().statusCode(ReturnCode.IDENTIFICATION_WARNING.getHttpCode());
    }

    private static void removedAndLoggedOut(final Scene scene, final Client a, final Client b) throws Exception {
        final String circleId = scene.circleId();
        final String alice = scene.alice();
        final String bobId = scene.bobId();
        // Bob is removed from the Circle: his connection is told that it
        // left, and hears nothing more of it.
        as(alice).when().delete("/circles/{circleId}/trustees/{memberId}", circleId, bobId).then().statusCode(200);
        final JsonPath left = b.expect(b.next(), "LEFT");
        assertEquals(circleId, left.getString("circleId"));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), left.getInt("returnCode"));
        assertEquals(bobId, a.expect(a.next(), "ABSENT").getString("memberId"));
        as(alice).body("""
                        {"state": "ON"}
                        """).when().put("/circles/{circleId}/features/{feature}", circleId, "notes")
                .then().statusCode(200);
        a.expect(a.next(), "FEATURE");
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), b.expect(b.send("""
                {"type": "COUNT", "circleId": "%s", "feature": "notes", "ref": "9"}
                """.formatted(circleId)), "ERROR").getInt("returnCode"));

        // A logout ends the connection of its Session.
        given().contentType(ContentType.JSON).body("""
                        {"credentialType": "SESSION", "credential": "%s"}
                        """.formatted(scene.bob())).when().post("/members/logout")
                .then().statusCode(200).body("returnCode", equalTo(200));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), b.expect(b.next(), "ERROR").getInt("returnCode"));
        assertEquals(1008, b.closeCode());
    }

    /**
     * Entries added at the same time, by several connections and of kinds
     * which add and which replace, each get a position of their own.
     */
    @Test
    void testEntriesAddedAtOnceGetTheirOwnPositions() throws Exception {
        final String admin = login("admin", ADMIN_CREDENTIAL);
        final String memberId = createMember(admin, "socket-busy");
        final String member = login("socket-busy", MEMBER_CREDENTIAL);
        as(admin).body(declaration()).when().put("/features/{feature}", "notes").then().statusCode(200);
        final String circleId = as(member).body("""
                        {"circleName": "Busy Circle"}
                        """).when().post("/circles")
                .then().statusCode(200).extract().path("circleId");
        as(member).body("""
                        {"state": "ON"}
                        """).when().put("/circles/{circleId}/features/{feature}", circleId, "notes")
                .then().statusCode(200);

        final int each = 15;
        try (Client a = connect(); Client b = connect(); Client c = connect()) {
            authenticateAndJoin(a, member, circleId);
            authenticateAndJoin(b, member, circleId);
            authenticateAndJoin(c, member, circleId);
            // Notes are added; the marker of the member is replaced each time.
            for (int i = 0; i < each; i++) {
                a.post("""
                        {"type": "ADD", "circleId": "%s", "feature": "notes", "kind": "note", "content": "%s"}
                        """.formatted(circleId, encode("a" + i)));
                b.post("""
                        {"type": "ADD", "circleId": "%s", "feature": "notes", "kind": "seen", "content": "%s"}
                        """.formatted(circleId, encode("b" + i)));
                c.post("""
                        {"type": "ADD", "circleId": "%s", "feature": "notes", "kind": "note", "content": "%s"}
                        """.formatted(circleId, encode("c" + i)));
            }
            // Every request is answered, and none failed.
            awaitDone(a, each);
            awaitDone(b, each);
            awaitDone(c, each);
        }
        final java.util.List<Integer> positions = as(member).when()
                .get("/circles/{circleId}/features/{feature}/entries?limit=200&content=false", circleId, "notes")
                .then().statusCode(200).extract().path("entries.position");
        // All notes, and the one marker.
        assertEquals((2 * each) + 1, positions.size());
        assertEquals(positions.size(), new java.util.HashSet<>(positions).size());

        as(member).when().delete("/circles/{circleId}", circleId).then().statusCode(200);
        as(admin).when().delete("/features/{feature}", "notes").then().statusCode(200);
        as(admin).when().delete("/members/{memberId}", memberId).then().statusCode(200);
    }

    private static void authenticateAndJoin(final Client client, final String session, final String circleId) {
        client.expect(client.send("""
                {"type": "AUTH", "token": "%s"}
                """.formatted(session)), "READY");
        client.expect(client.send(join(circleId)), "JOINED");
    }

    /** Reads the frames of a connection until the given number of requests is done; none may fail. */
    private static void awaitDone(final Client client, final int requests) {
        int done = 0;
        while (done < requests) {
            final JsonPath frame = JsonPath.from(client.next());
            assertNotEquals("ERROR", frame.getString("type"), frame.prettify());
            if ("DONE".equals(frame.getString("type"))) {
                done++;
            }
        }
    }

    @Test
    void testTheFirstFrameMustAuthenticate() throws Exception {
        try (Client client = connect()) {
            final JsonPath error = client.expect(client.send("""
                    {"type": "JOIN", "circleId": "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a"}
                    """), "ERROR");
            assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), error.getInt("returnCode"));
            assertEquals(1008, client.closeCode());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static String declaration() {
        return "{\"declaration\": \"" + DECLARATION.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    private static String join(final String circleId) {
        return "{\"type\": \"JOIN\", \"circleId\": \"" + circleId + "\"}";
    }

    private Client connect() throws Exception {
        final URI uri = URI.create(socketUri.toString().replaceFirst("^http", "ws"));
        final Client client = new Client();
        client.socket = HttpClient.newHttpClient().newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .buildAsync(uri, client)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return client;
    }

    private static String createMember(final String admin, final String account) {
        return as(admin).body("""
                        {"newAccountName": "%s", "newCredential": "%s"}
                        """.formatted(account, MEMBER_CREDENTIAL))
                .when().post("/members")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("memberId");
    }

    private static RequestSpecification as(final String session) {
        return given().header("Authorization", "Bearer " + session).contentType(ContentType.JSON);
    }

    private static String login(final String account, final String credential) {
        return given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "%s", "credential": "%s"}
                        """.formatted(account, credential))
                .when().post("/members/login")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("session");
    }

    private static String encode(final String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A connection with the JDK client, collecting the text frames.
     */
    private static final class Client implements WebSocket.Listener, AutoCloseable {

        private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        private final CompletableFuture<Integer> closed = new CompletableFuture<>();
        private final StringBuilder partial = new StringBuilder();
        private WebSocket socket = null;

        String send(final String frame) {
            socket.sendText(frame, true).join();
            return next();
        }

        /** Sends a frame which isn't answered, like SIGNAL. */
        void post(final String frame) {
            socket.sendText(frame, true).join();
        }

        String next() {
            try {
                final String frame = frames.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertNotNull(frame, "No frame within " + TIMEOUT_SECONDS + " seconds.");
                return frame;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        JsonPath expect(final String frame, final String type) {
            final JsonPath json = JsonPath.from(frame);
            assertEquals(type, json.getString("type"), frame);
            return json;
        }

        int closeCode() throws Exception {
            return closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        @Override
        public CompletionStage<?> onText(final WebSocket webSocket, final CharSequence data, final boolean last) {
            partial.append(data);
            if (last) {
                frames.add(partial.toString());
                partial.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(final WebSocket webSocket, final int statusCode, final String reason) {
            closed.complete(statusCode);
            return null;
        }

        @Override
        public void onError(final WebSocket webSocket, final Throwable error) {
            closed.completeExceptionally(error);
        }

        @Override
        public void close() {
            if (!socket.isOutputClosed()) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            }
        }
    }
}
