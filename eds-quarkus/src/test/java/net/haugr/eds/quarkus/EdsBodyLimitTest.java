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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.services.BodyCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Every request but the uploads of content has a body limit which follows
 * the inline content, checked before the body is read (ticket 11), or, for a
 * body without a length, while it is read (ticket 25).
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@QuarkusTest
final class EdsBodyLimitTest {

    private static final int READ_TIMEOUT_MILLIS = 10_000;

    @TestHTTPResource("/")
    URL root;

    @Inject
    BodyLimit bodyLimit;

    @Inject
    Vertx vertx;

    @BeforeEach
    void apiRoot() {
        RestAssured.basePath = Constants.REST_API;
    }

    @Test
    void testALargeBodyIsRefusedBeforeItIsRead() throws IOException {
        // Before, the stone waited for, and parsed, up to 1100 MB; four such
        // requests at once took a heap of 512 MB down. The body is never sent
        // here, so only an answer to the headers passes.
        final String answer = headersOnly("POST", Constants.REST_API + "/members/login",
                "Content-Type: application/json\r\nContent-Length: 314572800\r\n");
        assertTrue(answer.startsWith("HTTP/1.1 413"), answer);
        assertTrue(answer.contains("\"returnCode\":" + ReturnCode.CONTENT_TOO_LARGE.getCode()), answer);
    }

    @Test
    void testADotSegmentDoesNotMakeAnUpload() throws IOException {
        // The review: the raw path looked like an upload, while the request
        // was routed by the normalized one, to PUT /data/{dataId} with JSON.
        for (final String id : new String[] { ".", "%2e", "%2E" }) {
            final String answer = headersOnly("PUT", Constants.REST_API + Constants.REST_DATA_BASE + '/' + id + "/content",
                    "Content-Type: application/json\r\nContent-Length: 314572800\r\n");
            assertTrue(answer.startsWith("HTTP/1.1 413"), id + ": " + answer);
        }
    }

    @Test
    void testHttp2BodiesAreLimitedByTheirLength() throws Exception {
        // The review: the limit must hold in HTTP/2 as well. A length over it
        // is refused; a request without a body, which in HTTP/2 comes without
        // a length, passes (a body without a length is counted, ticket 25).
        final HttpClient client = vertx.createHttpClient(new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2).setHttp2ClearTextUpgrade(false));
        try {
            final HttpClientResponse tooLarge = client.request(HttpMethod.POST, root.getPort(), root.getHost(), Constants.REST_API + "/members/login")
                    .compose(request -> {
                        request.putHeader("Content-Type", "application/json");
                        request.putHeader("Content-Length", String.valueOf(bodyLimit.limit() + 1));
                        return request.sendHead().compose(sent -> request.response());
                    }).toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            assertEquals(HttpVersion.HTTP_2, tooLarge.version());
            assertEquals(413, tooLarge.statusCode());

            final HttpClientResponse withLength = client.request(HttpMethod.POST, root.getPort(), root.getHost(), Constants.REST_API + "/members/login")
                    .compose(request -> request.putHeader("Content-Type", "application/json")
                            .send("{\"accountName\":\"nobody\",\"credential\":\"AAAA\"}"))
                    .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            assertEquals(200, withLength.statusCode());

            final HttpClientResponse withoutBody = client.request(HttpMethod.POST, root.getPort(), root.getHost(), Constants.REST_API + "/members/login")
                    .compose(HttpClientRequest::send)
                    .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            assertNotEquals(411, withoutBody.statusCode());
            assertNotEquals(413, withoutBody.statusCode());
        } finally {
            client.close();
        }
    }

    @Test
    void testAnHttp2BodyWithoutALengthIsCountedWhileRead() throws Exception {
        // Ticket 25: in HTTP/2 a body needs no length; it is counted while it
        // is read, and the request refused once the count passes the limit.
        final HttpClient client = vertx.createHttpClient(new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2).setHttp2ClearTextUpgrade(false));
        try {
            final Buffer chunk = Buffer.buffer("A".repeat(64 * 1024));
            final long chunks = bodyLimit.limit() / chunk.length() + 64;
            final String answer = client.request(HttpMethod.POST, root.getPort(), root.getHost(), Constants.REST_API + "/members/login")
                    .compose(request -> {
                        // In HTTP/2, "chunked" is a body without a length.
                        request.setChunked(true).putHeader("Content-Type", "application/json");
                        request.write("{\"accountName\":\"nobody\",\"credential\":\"");
                        writeChunks(request, chunk, chunks);
                        return request.response();
                    })
                    .compose(response -> response.body().map(body -> response.version() + " " + response.statusCode() + " " + body))
                    .toCompletionStage().toCompletableFuture().get(60, TimeUnit.SECONDS);
            assertTrue(answer.startsWith("HTTP_2 413 "), answer);
        } finally {
            client.close();
        }
    }

    private static void writeChunks(final HttpClientRequest request, final Buffer chunk, final long left) {
        if (left == 0) {
            request.end("\"}");
        } else {
            request.write(chunk).onSuccess(written -> writeChunks(request, chunk, left - 1));
        }
    }

    @Test
    void testABodyWithoutALengthIsRefused() throws IOException {
        final String answer = headersOnly("POST", Constants.REST_API + "/trustees/setSecrets",
                "Content-Type: application/json\r\nTransfer-Encoding: chunked\r\n");
        assertTrue(answer.startsWith("HTTP/1.1 411"), answer);
    }

    @Test
    void testABodyOfTheLimitIsServed() throws IOException {
        final long limit = bodyLimit.limit();
        assertEquals(BodyCounter.MIN_LIMIT, limit);

        // The largest inline content, in Base64, fits: the endpoint answers it
        // as it answers the same request when it is small.
        final int small = given().contentType(ContentType.JSON).body(loginOfLength(200))
                .when().post("/members/login")
                .then().statusCode(200)
                .extract().path("returnCode");
        assertNotEquals(ReturnCode.CONTENT_TOO_LARGE.getCode(), small);
        given().contentType(ContentType.JSON).body(loginOfLength(limit))
                .when().post("/members/login")
                .then().statusCode(200)
                .body("returnCode", equalTo(small));
        // One byte more is answered from the headers, and the connection closed.
        final String answer = headersOnly("POST", Constants.REST_API + "/members/login",
                "Content-Type: application/json\r\nContent-Length: " + (limit + 1) + "\r\n");
        assertTrue(answer.startsWith("HTTP/1.1 413"), answer);
    }

    @Test
    void testAnUploadOfContentKeepsItsOwnLimit() {
        final byte[] content = new byte[(int) BodyCounter.MIN_LIMIT + 1];
        final int status = given().contentType(ContentType.BINARY)
                .header("Authorization", "Bearer " + UUID.randomUUID())
                .body(content)
                .when().put("/data/" + UUID.randomUUID() + "/content")
                .then().extract().statusCode();
        assertNotEquals(413, status);
        assertNotEquals(411, status);
    }

    @Test
    void testTheLimitFollowsTheInlineContent() {
        final Settings settings = Settings.getInstance();
        final String inline = settings.get(StandardSetting.DATA_MAX_INLINE_SIZE.getKey());
        try {
            settings.set(StandardSetting.DATA_MAX_INLINE_SIZE.getKey(), String.valueOf(30L * 1024 * 1024));
            assertEquals(40L * 1024 * 1024 + BodyCounter.ROOM, bodyLimit.limit());
        } finally {
            settings.set(StandardSetting.DATA_MAX_INLINE_SIZE.getKey(), inline);
        }
    }

    @Test
    void testOnlyTheUploadsAreExempt() {
        final String data = Constants.REST_API + Constants.REST_DATA_BASE + "/" + UUID.randomUUID();
        assertTrue(BodyLimit.isUpload(HttpMethod.PUT, data + "/content"));
        assertTrue(BodyLimit.isUpload(HttpMethod.PUT, data + "/thumbnail"));
        assertFalse(BodyLimit.isUpload(HttpMethod.POST, data + "/content"));
        assertFalse(BodyLimit.isUpload(HttpMethod.PUT, data + "/content/more"));
        assertFalse(BodyLimit.isUpload(HttpMethod.PUT, Constants.REST_API + "/members/content"));
        assertFalse(BodyLimit.isUpload(HttpMethod.PUT, null));

        assertFalse(BodyLimit.tooLarge(" 16 ", 16));
        assertTrue(BodyLimit.tooLarge("17", 16));
        assertTrue(BodyLimit.tooLarge("-1", 16));
        assertTrue(BodyLimit.tooLarge("9999999999999999999999", 16));
    }

    /**
     * A login whose body has exactly the given length.
     */
    private static String loginOfLength(final long length) {
        final String start = "{\"accountName\":\"nobody\",\"credential\":\"";
        final String end = "\"}";
        // Base64 comes in groups of 4; blanks after the JSON make up the rest.
        final int base64 = (int) ((length - start.length() - end.length()) / 4 * 4);
        final String json = start + "A".repeat(base64) + end;
        return json + " ".repeat((int) (length - json.length()));
    }

    /**
     * Sends only the headers of a request, and reads the answer's status line
     * and body; fails if no answer comes within the read timeout.
     */
    // The test stone answers plain HTTP on the loopback; a client which can
    // send headers without the body it announces needs a socket of its own.
    @SuppressWarnings("findsecbugs:UNENCRYPTED_SOCKET")
    private String headersOnly(final String method, final String path, final String headers) throws IOException {
        try (Socket socket = new Socket(root.getHost(), root.getPort());
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(READ_TIMEOUT_MILLIS);
            out.write((method + ' ' + path + " HTTP/1.1\r\nHost: " + root.getHost() + "\r\n" + headers + "\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            final StringBuilder answer = new StringBuilder();
            int length = 0;
            for (String line = in.readLine(); (line != null) && !line.isEmpty(); line = in.readLine()) {
                answer.append(line).append('\n');
                if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                    length = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                }
            }
            final char[] body = new char[length];
            int read = 0;
            while (read < length) {
                final int count = in.read(body, read, length - read);
                assertTrue(count > 0, "The answer ended early: " + answer);
                read += count;
            }
            return answer.append('\n').append(body).toString();
        }
    }
}
