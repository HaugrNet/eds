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
package net.haugr.eds.load;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * <p>The HTTP side of one user of the load test: its own connections (an
 * HttpClient of its own, as a separate client would have), JSON in and out,
 * raw bytes for content, and every request measured in {@link Metrics}.</p>
 *
 * <p>The resource API answers with the HTTP status of the return code; the
 * older JSON API with 200 and the return code in the body. Both are read as
 * the return code, so a request is good when it is 200 (or 206).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class Http implements AutoCloseable {

    static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final String base;
    private final Metrics metrics;
    private final HttpClient client;
    private volatile String session = null;

    /** The answer to a request. */
    record Reply(int code, JsonNode json, byte[] body, HttpResponse<byte[]> response, String error) {

        boolean ok() {
            return (code == 200) || (code == 206);
        }

        String text(final String field) {
            final JsonNode node = (json != null) ? json.get(field) : null;
            return ((node != null) && !node.isNull()) ? node.asText() : null;
        }

        String header(final String name) {
            return (response != null) ? response.headers().firstValue(name).orElse(null) : null;
        }

        String describe() {
            if (error != null) {
                return error;
            }
            final String message = text("returnMessage");
            return code + ((message != null) ? " " + message : "");
        }
    }

    Http(final String base, final Metrics metrics) {
        this.base = base;
        this.metrics = metrics;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .version(HttpClient.Version.HTTP_1_1)
                .proxy(HttpClient.Builder.NO_PROXY)
                .build();
    }

    void session(final String token) {
        this.session = token;
    }

    String session() {
        return session;
    }

    static ObjectNode object() {
        return JSON.createObjectNode();
    }

    static String query(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** The body of the older JSON API, authenticated with the Session in the body instead of the header. */
    ObjectNode sessionBody() {
        return object().put("credential", session).put("credentialType", "SESSION");
    }

    // =========================================================================
    // Requests
    // =========================================================================

    Reply get(final String operation, final String path) {
        return json(operation, "GET", path, null, Set.of());
    }

    Reply delete(final String operation, final String path) {
        return json(operation, "DELETE", path, null, Set.of());
    }

    Reply post(final String operation, final String path, final JsonNode body) {
        return json(operation, "POST", path, body, Set.of());
    }

    Reply put(final String operation, final String path, final JsonNode body) {
        return json(operation, "PUT", path, body, Set.of());
    }

    /**
     * A JSON request. A return code in {@code expected} counts as an
     * expected outcome; everything else that isn't 200 as a failure.
     */
    Reply json(final String operation, final String method, final String path, final JsonNode body, final Set<Integer> expected) {
        final byte[] bytes = (body != null) ? body.toString().getBytes(StandardCharsets.UTF_8) : null;
        final HttpRequest.Builder builder = request(path)
                .header("Accept", "application/json")
                .method(method, (bytes != null) ? HttpRequest.BodyPublishers.ofByteArray(bytes) : HttpRequest.BodyPublishers.noBody());
        if (bytes != null) {
            builder.header("Content-Type", "application/json");
        }
        return send(operation, builder, (bytes != null) ? bytes.length : 0, expected, true);
    }

    /** Content: a body of bytes up, or a download; the JSON body of an answer is read for an error. */
    Reply bytes(final String operation, final String method, final String path, final byte[] body, final String contentType,
                final Map<String, String> headers, final Set<Integer> expected, final boolean authorized) {
        final HttpRequest.Builder builder = request(path)
                .method(method, (body != null) ? HttpRequest.BodyPublishers.ofByteArray(body) : HttpRequest.BodyPublishers.noBody());
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        headers.forEach(builder::header);
        return send(operation, builder, (body != null) ? body.length : 0, expected, authorized);
    }

    private HttpRequest.Builder request(final String path) {
        return HttpRequest.newBuilder(URI.create(base + path)).timeout(TIMEOUT);
    }

    private Reply send(final String operation, final HttpRequest.Builder builder, final long sent, final Set<Integer> expected, final boolean authorized) {
        final String token = session;
        if (authorized && (token != null)) {
            builder.header("Authorization", "Bearer " + token);
        }
        final long start = System.nanoTime();
        try {
            final HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            final long time = System.nanoTime() - start;
            final byte[] body = response.body();
            JsonNode json = null;
            final String type = response.headers().firstValue("Content-Type").orElse("");
            if (type.startsWith("application/json") && (body.length > 0)) {
                json = JSON.readTree(body);
            }
            int code = response.statusCode();
            if ((code == 200) && (json != null) && json.has("returnCode")) {
                // The older JSON API answers 200 with the outcome in the body.
                code = json.get("returnCode").asInt();
            }
            final Reply reply = new Reply(code, json, body, response, null);
            final Metrics.Outcome outcome = reply.ok() ? Metrics.Outcome.OK
                    : expected.contains(code) ? Metrics.Outcome.EXPECTED : Metrics.Outcome.FAILED;
            metrics.tally(Metrics.Kind.REST, operation, outcome, time, sent, body.length, reply.ok() ? null : reply.describe());
            return reply;
        } catch (IOException e) {
            final long time = System.nanoTime() - start;
            final String error = "no answer: " + e;
            metrics.tally(Metrics.Kind.REST, operation, Metrics.Outcome.FAILED, time, sent, 0, error);
            return new Reply(0, null, new byte[0], null, error);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return new Reply(0, null, new byte[0], null, "interrupted");
        }
    }

    /** Records an outcome found by looking at an answer, such as content which isn't what was uploaded. */
    void verdict(final String operation, final boolean good, final String detail) {
        metrics.tally(operation, good ? Metrics.Outcome.OK : Metrics.Outcome.FAILED, 0, 0, 0, detail);
    }

    HttpClient client() {
        return client;
    }

    String base() {
        return base;
    }

    @Override
    public void close() {
        client.close();
    }
}
