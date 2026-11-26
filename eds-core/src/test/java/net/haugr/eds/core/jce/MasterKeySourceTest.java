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
package net.haugr.eds.core.jce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.LocalListener;
import net.haugr.eds.core.exceptions.EDSException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The source of the MasterKey may be given before anyone is authenticated
 * (ticket 6), so it is read within limits: only {@code file:} (a regular
 * file) and {@code https:}, a secret of a bounded size, within timeouts, and
 * a failure never repeats the URL (ticket 5).
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class MasterKeySourceTest {

    @TempDir
    Path directory;

    @Test
    void testASecretOfTheLimitIsRead() throws IOException {
        final byte[] secret = new byte[MasterKey.MAX_SECRET_BYTES];
        final String url = Files.write(directory.resolve("secret"), secret).toUri().toString();

        assertArrayEquals(secret, MasterKey.readMasterKeySecretFromUrl(url));
    }

    @Test
    void testASourceAboveTheLimitIsRefused() throws IOException {
        final String url = Files.write(directory.resolve("large"), new byte[MasterKey.MAX_SECRET_BYTES + 1]).toUri().toString();

        final EDSException e = assertThrows(EDSException.class, () -> MasterKey.readMasterKeySecretFromUrl(url));
        assertEquals(ReturnCode.NETWORK_ERROR, e.getReturnCode());
        assertEquals("The MasterKey source could not be read (it holds more than 16384 bytes).", e.getMessage());
    }

    @Test
    void testADeviceIsNotRead() {
        // Before, file:///dev/zero was read until the memory ran out.
        final EDSException e = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> assertThrows(EDSException.class, () -> MasterKey.readMasterKeySecretFromUrl("file:///dev/zero")));
        assertEquals("The MasterKey source could not be read (not a regular file).", e.getMessage());
    }

    @Test
    void testADirectoryIsNotRead() {
        final String url = directory.toUri().toString();
        final EDSException e = assertThrows(EDSException.class, () -> MasterKey.readMasterKeySecretFromUrl(url));
        assertEquals("The MasterKey source could not be read (not a regular file).", e.getMessage());
    }

    @Test
    void testTheMessageDoesNotRepeatTheUrl() {
        final String url = directory.resolve("token-a1b2c3").toUri() + "?key=s3cr3t";
        final EDSException e = assertThrows(EDSException.class, () -> MasterKey.readMasterKeySecretFromUrl(url));
        assertEquals(ReturnCode.NETWORK_ERROR, e.getReturnCode());
        assertFalse(e.getMessage().contains("token-a1b2c3"), e.getMessage());
        assertFalse(e.getMessage().contains("s3cr3t"), e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void testAnInvalidUrlIsRefused() {
        final EDSException e = assertThrows(EDSException.class, () -> MasterKey.readMasterKeySecretFromUrl("file://host name/secret"));
        assertEquals("The MasterKey source could not be read (URISyntaxException).", e.getMessage());
    }

    @Test
    void testOtherSchemesAreRefusedWithoutAConnection() throws IOException {
        try (LocalListener listener = LocalListener.answering()) {
            for (final String url : List.of("http://127.0.0.1:" + listener.port() + "/secret",
                    "ftp://127.0.0.1:" + listener.port() + "/secret",
                    "jar:file:///tmp/a.jar!/secret",
                    "file://127.0.0.1:" + listener.port() + "/secret",
                    "file://localhost/etc/hostname",
                    "/etc/hostname")) {
                final EDSException e = assertThrows(EDSException.class, () -> MasterKey.readMasterKeySecretFromUrl(url), url);
                assertEquals("The MasterKey source must be a file: URL without a host, or an https: URL.", e.getMessage(), url);
            }
            assertEquals(0, listener.accepted(), "Nothing may be fetched by another scheme.");
        }
    }

    @Test
    void testASilentServerTimesOut() throws IOException {
        // Before, there was no timeout: a server which never answers held
        // the request, and its database connection, for ever.
        try (LocalListener listener = LocalListener.silent()) {
            final String url = "https://127.0.0.1:" + listener.port() + "/secret";
            final long started = System.nanoTime();
            final EDSException e = assertTimeoutPreemptively(Duration.ofSeconds(20),
                    () -> assertThrows(EDSException.class, () -> MasterKey.readMasterKeySecretFromUrl(url)));
            final long millis = Duration.ofNanos(System.nanoTime() - started).toMillis();

            assertEquals("The MasterKey source could not be read (SocketTimeoutException).", e.getMessage());
            assertTrue((millis >= MasterKey.TIMEOUT_MILLIS - 100) && (millis < MasterKey.DEADLINE_MILLIS), "Timed out after " + millis + " ms");
            assertEquals(1, listener.accepted());
        }
    }

    @Test
    void testASourceWhichDripsStopsAtTheDeadline() throws IOException {
        // One byte a second never lets a read time out; before, such a
        // server held the request for as long as it liked, the TLS
        // handshake and the headers included.
        try (LocalListener listener = LocalListener.dripping()) {
            final String url = "https://127.0.0.1:" + listener.port() + "/secret";
            final long started = System.nanoTime();
            final EDSException e = assertTimeoutPreemptively(Duration.ofSeconds(30),
                    () -> assertThrows(EDSException.class, () -> MasterKey.readMasterKeySecretFromUrl(url)));
            final long millis = Duration.ofNanos(System.nanoTime() - started).toMillis();

            assertEquals("The MasterKey source could not be read (it took longer than 10000 ms).", e.getMessage());
            assertTrue((millis >= MasterKey.DEADLINE_MILLIS - 100) && (millis < MasterKey.DEADLINE_MILLIS + 5_000), "Stopped after " + millis + " ms");
            // The reading thread is interrupted, and gives its slot back.
            awaitUntil(() -> MasterKey.availableFetches() == MasterKey.MAX_FETCHES);
        }
    }

    @Test
    void testOnlyAFewSourcesAreReadAtATime() throws Exception {
        // Slow sources can't hold all the database connections: beyond the
        // limit, a request is refused at once.
        // A reading which a test before stopped ends in its own thread.
        awaitUntil(() -> MasterKey.availableFetches() == MasterKey.MAX_FETCHES);
        try (LocalListener listener = LocalListener.silent()) {
            final String url = "https://127.0.0.1:" + listener.port() + "/secret";
            final List<Thread> readers = new ArrayList<>();
            for (int i = 0; i < MasterKey.MAX_FETCHES; i++) {
                readers.add(Thread.ofVirtual().start(() -> assertThrows(EDSException.class, () -> MasterKey.readMasterKeySecretFromUrl(url))));
            }
            awaitUntil(() -> listener.accepted() == MasterKey.MAX_FETCHES);
            assertEquals(MasterKey.MAX_FETCHES, listener.accepted());

            final long started = System.nanoTime();
            final EDSException e = assertThrows(EDSException.class, () -> MasterKey.readMasterKeySecretFromUrl(url));
            assertEquals(MasterKey.SOURCES_BUSY, e.getMessage());
            assertTrue(Duration.ofNanos(System.nanoTime() - started).toMillis() < 1_000);
            assertEquals(MasterKey.MAX_FETCHES, listener.accepted());

            for (final Thread reader : readers) {
                reader.join(Duration.ofSeconds(20));
            }
        }
    }

    @Test
    void testAnEmptySourceGivesNoBytes() throws IOException {
        assertEquals(0, MasterKey.readLimited(new ByteArrayInputStream(new byte[0])).length);
    }

    /**
     * Polls the condition, and fails if it doesn't hold within 10 seconds.
     */
    private static void awaitUntil(final BooleanSupplier condition) {
        final long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean()) {
            assertTrue(System.nanoTime() < until, "The condition didn't hold within 10 seconds.");
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
    }
}
