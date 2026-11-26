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
package net.haugr.eds.testing.load;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * <p>The socket of one user ({@code /api/v1/socket}): frames as JSON, a
 * request waits for the answer with its {@code ref}, and what EDS announces
 * on its own (ADDED, CHANGED, PRESENT, SIGNAL, …) goes to a listener.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class Socket implements AutoCloseable {

    private static final Duration ANSWER = Duration.ofSeconds(60);
    /** Frames which answer a request of this connection. */
    private static final Set<String> ANSWERS = Set.of("READY", "JOINED", "LEFT", "DONE", "ENTRIES", "COUNTED", "ERROR");

    private final Metrics metrics;
    private final Map<String, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final AtomicLong refs = new AtomicLong();
    private final Consumer<JsonNode> listener;
    private final Object sending = new Object();
    private final CompletableFuture<String> closed = new CompletableFuture<>();
    /** The user ended its Session itself, so the stone may end the connection first. */
    private final AtomicBoolean sessionEnding = new AtomicBoolean(false);
    private WebSocket socket = null;

    Socket(final Metrics metrics, final Consumer<JsonNode> listener) {
        this.metrics = metrics;
        this.listener = listener;
    }

    /** Opens the connection and authenticates it; false if either failed (recorded). */
    boolean open(final Http http) {
        final String url = http.base().replaceFirst("^http", "ws") + "/socket";
        final long start = System.nanoTime();
        try {
            socket = http.client().newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(url), new Listener())
                    .get(30, TimeUnit.SECONDS);
            metrics.tally(Metrics.Kind.SOCKET, "socket.connect", Metrics.Outcome.OK, System.nanoTime() - start, 0, 0, null);
        } catch (ExecutionException | TimeoutException e) {
            metrics.tally(Metrics.Kind.SOCKET, "socket.connect", Metrics.Outcome.FAILED, System.nanoTime() - start, 0, 0, String.valueOf(e));
            return false;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
        final JsonNode ready = request("socket.AUTH", Http.object().put("type", "AUTH").put("token", http.session()), Set.of());
        return (ready != null) && "READY".equals(ready.path("type").asText());
    }

    boolean isOpen() {
        return (socket != null) && !closed.isDone();
    }

    /** Why the stone closed the connection, or null while it is open. */
    String closedBecause() {
        return closed.getNow(null);
    }

    /**
     * Sends a frame and waits for its answer. An ERROR with a return code in
     * {@code expected} counts as expected; null if no answer came.
     */
    JsonNode request(final String operation, final ObjectNode frame, final Set<Integer> expected) {
        final String ref = Long.toString(refs.incrementAndGet());
        frame.put("ref", ref);
        final CompletableFuture<JsonNode> answer = new CompletableFuture<>();
        pending.put(ref, answer);
        final String text = frame.toString();
        final long start = System.nanoTime();
        try {
            send(text);
            final JsonNode reply = answer.get(ANSWER.toSeconds(), TimeUnit.SECONDS);
            final long time = System.nanoTime() - start;
            if ("ERROR".equals(reply.path("type").asText())) {
                final int code = reply.path("returnCode").asInt();
                final String detail = code + " " + reply.path("returnMessage").asText();
                metrics.tally(Metrics.Kind.SOCKET, operation, expected.contains(code) ? Metrics.Outcome.EXPECTED : Metrics.Outcome.FAILED, time, text.length(), 0, detail);
            } else {
                metrics.tally(Metrics.Kind.SOCKET, operation, Metrics.Outcome.OK, time, text.length(), 0, null);
            }
            return reply;
        } catch (ExecutionException | TimeoutException | IOException e) {
            metrics.tally(Metrics.Kind.SOCKET, operation, Metrics.Outcome.FAILED, System.nanoTime() - start, text.length(), 0,
                    "no answer: " + e + ((closedBecause() != null) ? ", closed: " + closedBecause() : ""));
            return null;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            pending.remove(ref);
        }
    }

    /** Sends a frame which EDS doesn't answer, a SIGNAL; an ERROR for it would come with its ref. */
    void fire(final String operation, final ObjectNode frame) {
        frame.put("ref", Long.toString(refs.incrementAndGet()));
        final String text = frame.toString();
        final long start = System.nanoTime();
        try {
            send(text);
            metrics.tally(operation, Metrics.Outcome.OK, System.nanoTime() - start, text.length(), 0, null);
        } catch (IOException e) {
            metrics.tally(operation, Metrics.Outcome.FAILED, System.nanoTime() - start, text.length(), 0, "not sent: " + e);
        }
    }

    private void send(final String text) throws IOException {
        if (!isOpen()) {
            throw new IOException("the socket is closed" + ((closedBecause() != null) ? ": " + closedBecause() : ""));
        }
        // A WebSocket takes one message at a time.
        synchronized (sending) {
            try {
                socket.sendText(text, true).get(30, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                throw new IOException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
        }
    }

    /**
     * The user is about to end its Session, e.g. with a new passphrase:
     * until the socket is closed, the stone may tell it first, with an ERROR
     * of no frame, which is then expected.
     */
    public void sessionEnding() {
        sessionEnding.set(true);
    }

    /** The request waiting for this frame, or null if it answers none of this connection. */
    private CompletableFuture<JsonNode> waitingFor(final String type, final String ref) {
        return ((ref != null) && ANSWERS.contains(type)) ? pending.get(ref) : null;
    }

    /** The connection is gone: no request waiting will get its answer. */
    private void failPending(final Throwable why) {
        pending.values().forEach(answer -> answer.completeExceptionally(why));
    }

    @Override
    public void close() {
        if (isOpen()) {
            try {
                synchronized (sending) {
                    socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(10, TimeUnit.SECONDS);
                }
            } catch (ExecutionException | TimeoutException _) {
                socket.abort();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                socket.abort();
            }
        }
        closed.complete("closed by the test");
    }

    private final class Listener implements WebSocket.Listener {

        private final StringBuilder text = new StringBuilder();

        @Override
        public CompletionStage<?> onText(final WebSocket webSocket, final CharSequence data, final boolean last) {
            text.append(data);
            if (last) {
                final String frame = text.toString();
                text.setLength(0);
                handle(frame);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(final WebSocket webSocket, final int statusCode, final String reason) {
            closed.complete(statusCode + " " + reason);
            failPending(new IOException("closed: " + statusCode + " " + reason));
            return null;
        }

        @Override
        public void onError(final WebSocket webSocket, final Throwable error) {
            closed.complete("error: " + error);
            failPending(error);
        }

        private void handle(final String frame) {
            try {
                final JsonNode node = Http.JSON.readTree(frame.getBytes(StandardCharsets.UTF_8));
                final String type = node.path("type").asText();
                final String ref = node.path("ref").asText(null);
                final CompletableFuture<JsonNode> answer = waitingFor(type, ref);
                if (answer != null) {
                    answer.complete(node);
                } else if ("ERROR".equals(type)) {
                    // An error of a frame nobody waits for: a signal, or the stone's own.
                    final boolean ended = sessionEnding.get() && (node.path("returnCode").asInt() == 403);
                    metrics.tally("socket.ERROR", ended ? Metrics.Outcome.EXPECTED : Metrics.Outcome.FAILED, 0, 0, frame.length(), frame);
                } else {
                    metrics.tally("socket.heard." + type, Metrics.Outcome.OK, 0, 0, frame.length(), null);
                    listener.accept(node);
                }
            } catch (IOException | RuntimeException e) {
                metrics.tally("socket.frame", Metrics.Outcome.FAILED, 0, 0, frame.length(), "unreadable frame: " + e);
            }
        }
    }
}
