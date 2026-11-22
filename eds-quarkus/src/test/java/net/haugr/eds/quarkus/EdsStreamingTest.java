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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.SubmissionPublisher;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.SplittableRandom;
import net.haugr.eds.api.common.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>The content upload over HTTP (PLAN.md, S2): Bearer authentication,
 * the HTTP status codes, the ETag header, and the stream transaction in the
 * Quarkus runtime.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@QuarkusTest
final class EdsStreamingTest {

    private static final String ADMIN_CREDENTIAL = encode("Admin-Passphrase-For-Testing-Only");
    private static final String MEMBER_CREDENTIAL = encode("stream member pjvgr.yxupm-37i23.8kq4h_9s");
    private static final String CONTENT = "/data/{dataId}/content";

    @BeforeEach
    void apiRoot() {
        // The REST API is below Constants.REST_API; Quarkus only knows the root
        // of the application.
        RestAssured.basePath = Constants.REST_API;
    }

    @Test
    void testUploadOverHttp() throws IOException, InterruptedException {
        final String circleId = prepareMemberAndCircle();
        final String token = given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "stream-member", "credential": "%s"}
                        """.formatted(MEMBER_CREDENTIAL))
                .when().post("/members/login")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("session");
        final String dataId = given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "stream-member", "credential": "%s", "circleId": "%s", "dataName": "streamed"}
                        """.formatted(MEMBER_CREDENTIAL, circleId))
                .when().post("/data/addData")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("dataId");

        // Upload, more than three chunks, with a partial last chunk.
        final byte[] content = bytes(200_000);
        final Response uploaded = given().header("Authorization", "Bearer " + token)
                .contentType("application/octet-stream").body(content)
                .when().put(CONTENT, dataId);
        uploaded.then().statusCode(200).body("returnCode", equalTo(200)).body("dataId", equalTo(dataId)).header("ETag", notNullValue());
        final String etag = uploaded.getHeader("ETag");
        assertEquals(etag, '"' + uploaded.jsonPath().getString("etag").replace("\"", "") + '"');
        assertArrayEquals(content, fetch(dataId));

        // If-Match with the current ETag replaces; a stale one is refused.
        final byte[] second = bytes(1_000);
        final String newEtag = given().header("Authorization", "Bearer " + token).header("If-Match", etag)
                .contentType("application/octet-stream").body(second)
                .when().put(CONTENT, dataId)
                .then().statusCode(200).extract().header("ETag");
        assertNotEquals(etag, newEtag);
        given().header("Authorization", "Bearer " + token).header("If-Match", etag)
                .contentType("application/octet-stream").body(bytes(10))
                .when().put(CONTENT, dataId)
                .then().statusCode(412).body("returnCode", equalTo(412));
        assertArrayEquals(second, fetch(dataId));

        // Errors with their HTTP status.
        given().contentType("application/octet-stream").body(bytes(10))
                .when().put(CONTENT, dataId)
                .then().statusCode(403).body("returnCode", equalTo(403));
        given().header("Authorization", "Bearer " + token)
                .contentType("application/octet-stream").body(bytes(10))
                .when().put(CONTENT, "4c5bc2c6-0fa5-4c3a-9d1e-1a2b3c4d5e6f")
                .then().statusCode(404);
        // A streamed body without Content-Length (chunked transfer), as from
        // a pipe: it is read to its end. The JDK client is used, as
        // RestAssured would set the length itself.
        final byte[] piped = bytes(150_000);
        final HttpResponse<String> chunked = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + RestAssured.port + "/" + RestAssured.basePath.replaceAll("^/", "") + "/data/" + dataId + "/content"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/octet-stream")
                        .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(piped)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, chunked.statusCode(), chunked.body());
        assertTrue(chunked.headers().firstValue("ETag").isPresent(), "ETag");
        assertArrayEquals(piped, fetch(dataId));
        given().header("Authorization", "Bearer " + token)
                .contentType("application/octet-stream").body(second)
                .when().put(CONTENT, dataId)
                .then().statusCode(200);

        // An upload which the client aborts half way (it announces 300000
        // bytes, sends 100000 and fails): the transaction is rolled back,
        // and the previous content stays.
        try (SubmissionPublisher<ByteBuffer> publisher = new SubmissionPublisher<>()) {
            final var aborted = HttpClient.newHttpClient().sendAsync(HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:" + RestAssured.port + "/" + RestAssured.basePath.replaceAll("^/", "") + "/data/" + dataId + "/content"))
                            .header("Authorization", "Bearer " + token)
                            .header("Content-Type", "application/octet-stream")
                            .PUT(HttpRequest.BodyPublishers.fromPublisher(publisher, 300_000))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            for (int i = 0; i < 10; i++) {
                publisher.submit(ByteBuffer.wrap(bytes(10_000)));
            }
            publisher.closeExceptionally(new IOException("client aborted"));
            // Waits for the end of the request; that the client fails it is expected.
            aborted.exceptionally(error -> null).join();
        }
        assertArrayEquals(second, fetch(dataId));
        assertArrayEquals(second, fetch(dataId));
    }

    private static String prepareMemberAndCircle() {
        return prepareMemberAndCircle("stream-member", "Stream Circle");
    }

    private static String prepareMemberAndCircle(final String member, final String circle) {
        final String memberId = given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "admin", "credential": "%s", "newAccountName": "%s", "newCredential": "%s"}
                        """.formatted(ADMIN_CREDENTIAL, member, MEMBER_CREDENTIAL))
                .when().post("/members/createMember")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("memberId");

        return given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "admin", "credential": "%s", "circleName": "%s", "memberId": "%s"}
                        """.formatted(ADMIN_CREDENTIAL, circle, memberId))
                .when().post("/circles/createCircle")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("circleId");
    }

    @Test
    void testDownloadOverHttp() {
        final String circleId = prepareMemberAndCircle("download-member", "Download Circle");
        final String token = login("download-member");
        final String dataId = given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "download-member", "credential": "%s", "circleId": "%s", "dataName": "clip.mp4", "mediaType": "video/mp4"}
                        """.formatted(MEMBER_CREDENTIAL, circleId))
                .when().post("/data/addData")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("dataId");
        final byte[] content = bytes(200_000);
        final String etag = given().header("Authorization", "Bearer " + token)
                .contentType("application/octet-stream").body(content)
                .when().put(CONTENT, dataId)
                .then().statusCode(200).extract().header("ETag");

        // Full content, with the serving headers.
        final Response full = given().header("Authorization", "Bearer " + token).when().get(CONTENT, dataId);
        full.then().statusCode(200)
                .header("Content-Length", String.valueOf(content.length))
                .header("Content-Type", "video/mp4")
                .header("Accept-Ranges", "bytes")
                .header("ETag", etag)
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", org.hamcrest.Matchers.endsWith("sandbox"))
                .header("Cross-Origin-Resource-Policy", "same-origin")
                .header("Referrer-Policy", "no-referrer");
        assertTrue(full.getHeader("Content-Disposition").startsWith("inline;"));
        assertArrayEquals(content, full.asByteArray());

        // A range across two chunks, and the suffix range.
        final Response partial = given().header("Authorization", "Bearer " + token).header("Range", "bytes=65530-65545").when().get(CONTENT, dataId);
        partial.then().statusCode(206).header("Content-Range", "bytes 65530-65545/200000").header("Content-Length", "16");
        assertArrayEquals(Arrays.copyOfRange(content, 65530, 65546), partial.asByteArray());
        final Response suffix = given().header("Authorization", "Bearer " + token).header("Range", "bytes=-10").when().get(CONTENT, dataId);
        suffix.then().statusCode(206).header("Content-Range", "bytes 199990-199999/200000");
        assertArrayEquals(Arrays.copyOfRange(content, 199_990, 200_000), suffix.asByteArray());

        // If-Range with a stale ETag gives the full content.
        given().header("Authorization", "Bearer " + token).header("Range", "bytes=0-9").header("If-Range", "\"stale\"")
                .when().get(CONTENT, dataId).then().statusCode(200).header("Content-Length", "200000");

        // Unsatisfiable, HEAD, and errors.
        given().header("Authorization", "Bearer " + token).header("Range", "bytes=200000-")
                .when().get(CONTENT, dataId)
                .then().statusCode(416).header("Content-Range", "bytes */200000").body("returnCode", equalTo(416));
        given().header("Authorization", "Bearer " + token)
                .when().head(CONTENT, dataId)
                .then().statusCode(200).header("Content-Length", "200000").header("ETag", etag);
        given().when().get(CONTENT, dataId).then().statusCode(403);
        given().header("Authorization", "Bearer " + token).when().get(CONTENT, "4c5bc2c6-0fa5-4c3a-9d1e-1a2b3c4d5e6f").then().statusCode(404);
    }

    /**
     * The media support of the PWA (PLAN.md, L5): a download token reads
     * one content without the Authorization header, with ranges, and a
     * thumbnail is stored and found in the Metadata.
     */
    @Test
    void testDownloadTokenAndThumbnailOverHttp() {
        final String circleId = prepareMemberAndCircle("media-member", "Media Circle");
        final String session = login("media-member");
        final String dataId = given().header("Authorization", "Bearer " + session).contentType(ContentType.JSON)
                .body("""
                        {"circleId": "%s", "dataName": "film.webm", "mediaType": "video/webm"}
                        """.formatted(circleId))
                .when().post("/data")
                .then().statusCode(200).extract().path("dataId");
        final byte[] content = bytes(100_000);
        given().header("Authorization", "Bearer " + session).contentType("application/octet-stream").body(content)
                .when().put(CONTENT, dataId).then().statusCode(200);

        final Response created = given().header("Authorization", "Bearer " + session).when().post("/data/{dataId}/token", dataId);
        created.then().statusCode(200).body("returnCode", equalTo(200)).body("expires", notNullValue());
        final String token = created.path("token");

        // The token in the query, as an <video src> would send it.
        final Response partial = given().queryParam("token", token).header("Range", "bytes=1000-1999").when().get(CONTENT, dataId);
        partial.then().statusCode(206).header("Content-Type", "video/webm").header("Content-Range", "bytes 1000-1999/100000");
        assertArrayEquals(Arrays.copyOfRange(content, 1000, 2000), partial.asByteArray());
        given().queryParam("token", token).when().head(CONTENT, dataId).then().statusCode(200).header("Content-Length", "100000");
        given().queryParam("token", "invalid").when().get(CONTENT, dataId).then().statusCode(403).body("returnCode", equalTo(403));
        given().when().post("/data/{dataId}/token", dataId).then().statusCode(403);

        // A thumbnail.
        final byte[] image = { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3 };
        final String thumbnailId = given().header("Authorization", "Bearer " + session).contentType("image/png").body(image)
                .when().put("/data/{dataId}/thumbnail", dataId)
                .then().statusCode(200).body("returnCode", equalTo(200)).extract().path("dataId");
        given().header("Authorization", "Bearer " + session).when().get("/data/{dataId}", dataId)
                .then().statusCode(200).body("metadata[0].thumbnailId", equalTo(thumbnailId));
        final String thumbnailToken = given().header("Authorization", "Bearer " + session).when().post("/data/{dataId}/token", thumbnailId)
                .then().statusCode(200).extract().path("token");
        final Response thumbnail = given().queryParam("token", thumbnailToken).when().get(CONTENT, thumbnailId);
        thumbnail.then().statusCode(200).header("Content-Type", "image/png");
        assertArrayEquals(image, thumbnail.asByteArray());
        given().header("Authorization", "Bearer " + session).contentType("image/svg+xml").body("<svg/>")
                .when().put("/data/{dataId}/thumbnail", dataId)
                .then().statusCode(415);
    }

    /**
     * The sanity health group (S4) is served on the management port.
     */
    @Test
    void testSanityHealthGroup() throws IOException, InterruptedException {
        final int port = org.eclipse.microprofile.config.ConfigProvider.getConfig().getOptionalValue("quarkus.management.test-port", Integer.class).orElse(9001);
        final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/q/health/group/sanity")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("\"EDS sanity\""), response.body());
        assertTrue(response.body().contains("\"failedObjects\""), response.body());
    }

    /**
     * A copy and a move of content above the inline limit (10 MiB), which
     * read the source on a second connection of the pool while the copy is
     * written (PLAN.md, S4), with the JTA transactions of Quarkus.
     */
    @Test
    void testLargeCopyAndMoveOverHttp() {
        final String memberId = given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "admin", "credential": "%s", "newAccountName": "copy-member", "newCredential": "%s"}
                        """.formatted(ADMIN_CREDENTIAL, MEMBER_CREDENTIAL))
                .when().post("/members/createMember")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("memberId");
        final String firstCircle = createCircle(memberId, "Copy Circle");
        final String secondCircle = createCircle(memberId, "Copy Target");
        final String token = login("copy-member");
        final String dataId = given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "copy-member", "credential": "%s", "circleId": "%s", "dataName": "large"}
                        """.formatted(MEMBER_CREDENTIAL, firstCircle))
                .when().post("/data/addData")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("dataId");
        final byte[] content = bytes(11 * 1024 * 1024);
        given().header("Authorization", "Bearer " + token)
                .contentType("application/octet-stream").body(content)
                .when().put(CONTENT, dataId)
                .then().statusCode(200);

        final String copyId = given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "copy-member", "credential": "%s", "dataId": "%s", "targetCircleId": "%s"}
                        """.formatted(MEMBER_CREDENTIAL, dataId, secondCircle))
                .when().post("/data/copyData")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("dataId");
        assertArrayEquals(content, given().header("Authorization", "Bearer " + token).when().get(CONTENT, copyId).then().statusCode(200).extract().asByteArray());

        final String movedId = given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "copy-member", "credential": "%s", "dataId": "%s", "targetCircleId": "%s", "dataName": "moved"}
                        """.formatted(MEMBER_CREDENTIAL, copyId, firstCircle))
                .when().post("/data/moveData")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("dataId");
        assertArrayEquals(content, given().header("Authorization", "Bearer " + token).when().get(CONTENT, movedId).then().statusCode(200).extract().asByteArray());
        given().header("Authorization", "Bearer " + token).when().get(CONTENT, copyId).then().statusCode(404);
    }

    private static String createCircle(final String memberId, final String name) {
        return given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "admin", "credential": "%s", "circleName": "%s", "memberId": "%s"}
                        """.formatted(ADMIN_CREDENTIAL, name, memberId))
                .when().post("/circles/createCircle")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("circleId");
    }

    private static String login(final String member) {
        return given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "%s", "credential": "%s"}
                        """.formatted(member, MEMBER_CREDENTIAL))
                .when().post("/members/login")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("session");
    }

    private static byte[] fetch(final String dataId) {
        final String data = given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "stream-member", "credential": "%s", "dataId": "%s"}
                        """.formatted(MEMBER_CREDENTIAL, dataId))
                .when().post("/data/fetchData")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("data");
        return Base64.getDecoder().decode(data);
    }

    // Seeded on purpose: the test data must be the same in every run.
    private static byte[] bytes(final int size) {
        final byte[] bytes = new byte[size];
        new SplittableRandom(size).nextBytes(bytes);
        return bytes;
    }

    private static String encode(final String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
