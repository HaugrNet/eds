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
package net.haugr.eds.core.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.jce.DataStreams;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.ConnectionSource;
import net.haugr.eds.core.model.EncryptedDataStore;
import net.haugr.eds.core.model.StoreTestDatabases;
import org.junit.jupiter.api.Test;

/**
 * <p>Copy of content to another Circle (PLAN.md, S4): large content is
 * read on a Connection of its own while the target is written, small content
 * is copied in memory. On H2 and, if configured, on PostgreSQL.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ContentCopierTest {

    private static final int BATCH = 10_000;
    private static final long THRESHOLD = 50_000;

    @Test
    void testH2() throws Exception {
        runChecks(StoreTestDatabases.h2("copier"));
    }

    @Test
    void testPostgreSQL() throws Exception {
        final String url = StoreTestDatabases.postgresqlUrl("eds_copier");
        assumeTrue(url != null, "No PostgreSQL test database configured");
        runChecks(StoreTestDatabases.postgresql(url));
    }

    /**
     * How a streamed copy fails, on H2: the target can't be written, the
     * request is interrupted, or the reader is.
     */
    @Test
    void testH2Failures() throws Exception {
        final DataSource dataSource = StoreTestDatabases.h2("copier-failures");
        StoreTestDatabases.freshSchema(dataSource);
        checkFailingTarget(dataSource);
        checkInterruptedRequest(dataSource);
        checkInterruptedReader(dataSource);
    }

    private static void runChecks(final DataSource dataSource) throws Exception {
        StoreTestDatabases.freshSchema(dataSource);
        // Streamed, with many batches and chunks; in memory; empty content.
        checkCopy(dataSource, 1_000_000);
        checkCopy(dataSource, 40_000);
        checkCopy(dataSource, 0);
        // Streamed, and in memory.
        checkTamperedSource(dataSource, 300_000);
        checkTamperedSource(dataSource, 40_000);
        checkUnreadableSource(dataSource);
    }

    // Seeded on purpose: the test data must be the same in every run.
    @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
    private static void checkCopy(final DataSource dataSource, final int size) throws Exception {
        final byte[] plain = new byte[size];
        new Random(size).nextBytes(plain);
        final Fixture fixture = Fixture.create(dataSource, plain);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            final long targetId = StoreTestDatabases.createDataRow(connection);
            final SecretEDSKey targetSecret = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
            final DataFormat.Context targetContext = new DataFormat.Context("c2", "m2", 2);
            ContentCopier.copy(connection, dataSource::getConnection, fixture.source(),
                    new ContentCopier.Target(targetId, targetSecret, targetContext), BATCH, "SHA-512", THRESHOLD);
            connection.commit();

            final long stored = EncryptedDataStore.length(connection, targetId);
            final byte[] copied = DataStreams.decrypt(targetSecret, targetContext, EncryptedDataStore.open(connection, targetId, 0, stored, BATCH), stored).readAllBytes();
            assertArrayEquals(plain, copied, "size " + size);
        }
    }

    /**
     * A damaged chunk of the source fails the copy, also while a large one
     * is streamed, and nothing of the target is written.
     */
    // Seeded on purpose: the test data must be the same in every run.
    @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
    private static void checkTamperedSource(final DataSource dataSource, final int size) throws Exception {
        final byte[] plain = new byte[size];
        new Random(7).nextBytes(plain);
        final Fixture fixture = Fixture.create(dataSource, plain);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE eds_data_parts SET part = ? WHERE data_id = ? AND part_offset = ?")) {
            // The byte is in the second part, which follows the short first one.
            final int head = net.haugr.eds.core.model.entities.DataPart.HEAD_SIZE;
            final byte[] stored = EncryptedDataStore.open(connection, fixture.id(), 0, fixture.source().storedLength(), BATCH).readAllBytes();
            stored[(size * 2) / 3] ^= 1;
            statement.setBytes(1, java.util.Arrays.copyOfRange(stored, head, stored.length));
            statement.setLong(2, fixture.id());
            statement.setLong(3, head);
            org.junit.jupiter.api.Assertions.assertEquals(1, statement.executeUpdate());
        }

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            final long targetId = StoreTestDatabases.createDataRow(connection);
            connection.commit();
            final ContentCopier.Target target = new ContentCopier.Target(targetId, Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256), new DataFormat.Context("c2", "m3", 2));
            final ContentCopier.Source source = fixture.source();
            final EDSException e = assertThrows(EDSException.class, () -> ContentCopier.copy(connection, dataSource::getConnection, source, target, BATCH, "SHA-512", THRESHOLD));
            assertEquals(ReturnCode.INTEGRITY_ERROR, e.getReturnCode());
            connection.rollback();
            assertEquals(0, EncryptedDataStore.length(connection, targetId));
        }
    }

    /**
     * When the reader of a large source gets no connection, e.g. as the pool
     * is used up, the copy fails as a database error, and writes nothing.
     */
    // Seeded on purpose: the test data must be the same in every run.
    @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
    private static void checkUnreadableSource(final DataSource dataSource) throws Exception {
        final byte[] plain = new byte[300_000];
        new Random(11).nextBytes(plain);
        final Fixture fixture = Fixture.create(dataSource, plain);
        final ConnectionSource exhausted = () -> {
            throw new SQLException("No connection is left in the pool.", "08001");
        };

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            final long targetId = StoreTestDatabases.createDataRow(connection);
            connection.commit();
            final ContentCopier.Target target = new ContentCopier.Target(targetId, Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256), new DataFormat.Context("c2", "m4", 2));
            final ContentCopier.Source source = fixture.source();
            final EDSException e = assertThrows(EDSException.class, () -> ContentCopier.copy(connection, exhausted, source, target, BATCH, "SHA-512", THRESHOLD));
            assertEquals(ReturnCode.DATABASE_ERROR, e.getReturnCode(), e.getMessage());
            connection.rollback();
            assertEquals(0, EncryptedDataStore.length(connection, targetId));
        }
    }

    /**
     * A failure of the target's database, while the reader of a large
     * source still has chunks to hand over, is given on as it is, and the
     * reader stops.
     */
    // Seeded on purpose: the test data must be the same in every run.
    @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
    private static void checkFailingTarget(final DataSource dataSource) throws Exception {
        final byte[] plain = new byte[1_000_000];
        new Random(13).nextBytes(plain);
        final Fixture fixture = Fixture.create(dataSource, plain);
        final AtomicReference<Thread> reader = new AtomicReference<>();
        final ConnectionSource readers = () -> {
            reader.set(Thread.currentThread());
            return dataSource.getConnection();
        };

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            final long targetId = StoreTestDatabases.createDataRow(connection);
            connection.commit();
            final ContentCopier.Target target = new ContentCopier.Target(targetId, Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256), new DataFormat.Context("c2", "m5", 2));
            final ContentCopier.Source source = fixture.source();
            try (Connection failing = failingStatements(connection)) {
                final SQLException e = assertThrows(SQLException.class, () -> ContentCopier.copy(failing, readers, source, target, BATCH, "SHA-512", THRESHOLD));
                assertEquals("The target can't be written.", e.getMessage());
            }
            assertTrue(reader.get().join(Duration.ofSeconds(10)), "The reader stops.");
            connection.rollback();
            assertEquals(0, EncryptedDataStore.length(connection, targetId));
        }
    }

    /**
     * A request which is interrupted while it waits for the source fails
     * the copy, keeps the interrupt, and passes it on to the reader.
     */
    // Seeded on purpose: the test data must be the same in every run.
    @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
    private static void checkInterruptedRequest(final DataSource dataSource) throws Exception {
        final byte[] plain = new byte[300_000];
        new Random(17).nextBytes(plain);
        final Fixture fixture = Fixture.create(dataSource, plain);
        final AtomicReference<Thread> reader = new AtomicReference<>();
        final Thread request = Thread.currentThread();
        // The request is interrupted once the reader has started, so the
        // reader is known when the copy fails, whichever runs first.
        final ConnectionSource readers = () -> {
            reader.set(Thread.currentThread());
            request.interrupt();
            return dataSource.getConnection();
        };

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            final long targetId = StoreTestDatabases.createDataRow(connection);
            connection.commit();
            final ContentCopier.Target target = new ContentCopier.Target(targetId, Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256), new DataFormat.Context("c2", "m6", 2));
            final ContentCopier.Source source = fixture.source();
            final EDSException e = assertThrows(EDSException.class, () -> ContentCopier.copy(connection, readers, source, target, BATCH, "SHA-512", THRESHOLD));
            assertTrue(Thread.interrupted(), "The interrupt is kept.");
            assertTrue(e.getMessage().contains("Interrupted while reading the source."), e.getMessage());
            assertTrue(reader.get().join(Duration.ofSeconds(10)), "The reader stops.");
            connection.rollback();
            assertEquals(0, EncryptedDataStore.length(connection, targetId));
        }
    }

    /**
     * A reader which is interrupted (as when the server stops) ends without
     * handing over the rest; the copy notices that it stopped, instead of
     * waiting for it forever. The reader is interrupted while it reads, or
     * when it has read everything, after a while, before it tells the end.
     */
    private static void checkInterruptedReader(final DataSource dataSource) throws Exception {
        final AtomicInteger prepared = new AtomicInteger();
        checkInterruptedReader(dataSource, 300_000, method -> {
            if ("prepareStatement".equals(method) && (prepared.incrementAndGet() == 2)) {
                Thread.currentThread().interrupt();
            }
        });
        // The copy waits for the reader at most a second at a time, and
        // checks in between that it is still there.
        final CountDownLatch never = new CountDownLatch(1);
        checkInterruptedReader(dataSource, 100_000, method -> {
            if ("rollback".equals(method)) {
                assertFalse(never.await(1500, TimeUnit.MILLISECONDS));
                Thread.currentThread().interrupt();
            }
        });
    }

    // Seeded on purpose: the test data must be the same in every run.
    @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
    private static void checkInterruptedReader(final DataSource dataSource, final int size, final AfterCall interrupting) throws Exception {
        final byte[] plain = new byte[size];
        new Random(19).nextBytes(plain);
        final Fixture fixture = Fixture.create(dataSource, plain);
        final ConnectionSource readers = () -> watched(dataSource.getConnection(), interrupting);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            final long targetId = StoreTestDatabases.createDataRow(connection);
            connection.commit();
            final ContentCopier.Target target = new ContentCopier.Target(targetId, Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256), new DataFormat.Context("c2", "m7", 2));
            final ContentCopier.Source source = fixture.source();
            final EDSException e = assertThrows(EDSException.class, () -> ContentCopier.copy(connection, readers, source, target, BATCH, "SHA-512", THRESHOLD));
            assertTrue(e.getMessage().contains("The source reader stopped unexpectedly."), e.getMessage());
            connection.rollback();
            assertEquals(0, EncryptedDataStore.length(connection, targetId));
        }
    }

    /** Called on the reader's thread after each call of its Connection. */
    @FunctionalInterface
    private interface AfterCall {
        void called(String method) throws InterruptedException;
    }

    /** The connection, where every statement fails to be prepared. Closing it leaves the connection open. */
    private static Connection failingStatements(final Connection connection) {
        return (Connection) Proxy.newProxyInstance(ContentCopierTest.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    if (method.getName().endsWith("Statement")) {
                        throw new SQLException("The target can't be written.");
                    }
                    return "close".equals(method.getName()) ? null : invoke(connection, method, arguments);
                });
    }

    /**
     * The connection, which tells each call to it after it was made; closing
     * the returned one closes the given one.
     */
    private static Connection watched(final Connection connection, final AfterCall interrupting) {
        return (Connection) Proxy.newProxyInstance(ContentCopierTest.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    final Object result = invoke(connection, method, arguments);
                    interrupting.called(method.getName());
                    return result;
                });
    }

    private static Object invoke(final Connection connection, final Method method, final Object[] arguments) throws Throwable {
        try {
            return method.invoke(connection, arguments);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * A committed source object, encrypted in the §4 format.
     */
    private record Fixture(long id, ContentCopier.Source source) {

        static Fixture create(final DataSource dataSource, final byte[] plain) throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                final long id = StoreTestDatabases.createDataRow(connection);
                final SecretEDSKey secret = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
                final DataFormat.Context context = new DataFormat.Context("c1", "m" + id, 1);
                final DataStreams.EncryptingStream encrypting = DataStreams.encrypt(secret, context, new ByteArrayInputStream(plain), plain.length);
                EncryptedDataStore.write(connection, id, encrypting, encrypting.storedLength(), "SHA-512");
                connection.commit();
                return new Fixture(id, new ContentCopier.Source(id, encrypting.storedLength(), secret, context));
            }
        }
    }
}
