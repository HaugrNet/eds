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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * A local TCP listener for tests, which counts the connections it accepts.
 * Each connection is handled in a virtual thread of its own, until the
 * listener is closed: it is closed at once, kept open without a word (a
 * server which never answers), or given one byte a second (a server which
 * answers too slowly for anything, but never slowly enough for a read
 * timeout: a TLS record header, then the record one byte a second).
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
// findsecbugs:UNENCRYPTED_SERVER_SOCKET - a test listener on the loopback
// address, which never carries anything but the bytes the test sends.
@SuppressWarnings("findsecbugs:UNENCRYPTED_SERVER_SOCKET")
public final class LocalListener implements AutoCloseable {

    private enum Mode { CLOSING, SILENT, DRIPPING }

    private final ServerSocket server;
    private final Mode mode;
    private final AtomicInteger accepted = new AtomicInteger();
    private final Thread acceptor;

    private LocalListener(final Mode mode) throws IOException {
        this.mode = mode;
        server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
        acceptor = Thread.ofVirtual().start(this::acceptAll);
    }

    /**
     * @return A listener which closes each connection at once
     * @throws IOException if it can't listen
     */
    public static LocalListener answering() throws IOException {
        return new LocalListener(Mode.CLOSING);
    }

    /**
     * @return A listener which keeps each connection open without a word
     * @throws IOException if it can't listen
     */
    public static LocalListener silent() throws IOException {
        return new LocalListener(Mode.SILENT);
    }

    /**
     * @return A listener which sends one byte a second on each connection
     * @throws IOException if it can't listen
     */
    public static LocalListener dripping() throws IOException {
        return new LocalListener(Mode.DRIPPING);
    }

    private void acceptAll() {
        while (!server.isClosed()) {
            try {
                // PMD.CloseResource - the socket is closed by the thread
                // which handles it (handle, try-with-resources).
                @SuppressWarnings("PMD.CloseResource")
                final Socket socket = server.accept();
                accepted.incrementAndGet();
                Thread.ofVirtual().start(() -> handle(socket));
            } catch (IOException _) {
                // The listener is closed, which ends the loop.
                return;
            }
        }
    }

    private void handle(final Socket connection) {
        try (Socket socket = connection; OutputStream out = socket.getOutputStream()) {
            if (mode == Mode.DRIPPING) {
                // The header of a TLS record (handshake, TLS 1.2, 16 KiB),
                // whose body then comes one byte a second.
                out.write(new byte[] { 0x16, 0x03, 0x03, 0x40, 0x00 });
                out.flush();
            }
            while ((mode != Mode.CLOSING) && !server.isClosed()) {
                if (mode == Mode.DRIPPING) {
                    out.write(0x16);
                    out.flush();
                }
                // A server's pace, not a wait for a result of the test.
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            }
        } catch (IOException _) {
            // The other side went away.
        }
    }

    /**
     * @return The port listened on, of the loopback address
     */
    public int port() {
        return server.getLocalPort();
    }

    /**
     * @return The number of connections accepted so far
     */
    public int accepted() {
        return accepted.get();
    }

    /**
     * Stops listening; the connections end within a second.
     *
     * @throws IOException if the socket can't be closed
     */
    @Override
    public void close() throws IOException {
        server.close();
        try {
            acceptor.join(Duration.ofSeconds(5));
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
