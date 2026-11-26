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
package net.haugr.eds.core.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.entities.DataPart;
import org.junit.jupiter.api.Test;

/**
 * <p>Tests for the streamed access to the encrypted data (PLAN.md, S1),
 * on H2 and, if configured (see {@link StoreTestDatabases}), on PostgreSQL.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class EncryptedDataStoreTest {

    private static final int MB = 1024 * 1024;

    @Test
    void testASmallContentNeedsNoBufferOfAWholePart() throws Exception {
        // Ticket 36: each write allocated a buffer of a whole part (4 MiB),
        // whatever the size of the content.
        final com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        assumeTrue(threads.isThreadAllocatedMemorySupported());
        final DataSource dataSource = StoreTestDatabases.h2("store-buffer");
        StoreTestDatabases.freshSchema(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            final long id = StoreTestDatabases.createDataRow(connection);
            final byte[] small = new byte[100];
            // The first write loads the classes and prepares the statements.
            EncryptedDataStore.write(connection, id, new ByteArrayInputStream(small), small.length, "SHA-512");

            final long known = allocatedBy(threads, () ->
                    EncryptedDataStore.write(connection, id, new ByteArrayInputStream(small), small.length, "SHA-512"));
            final long unknown = allocatedBy(threads, () ->
                    EncryptedDataStore.writeToTheEnd(connection, id, new ByteArrayInputStream(small), "SHA-512"));
            assertTrue(known < MB, "A write of 100 bytes with their length allocated " + known + " bytes");
            assertTrue(unknown < MB, "A write of 100 bytes without their length allocated " + unknown + " bytes");

            // Without a length, the buffer grows past its first size as the
            // stream gives more.
            final byte[] large = new byte[DataPart.DEFAULT_SIZE + 3 * DataPart.MIN_SIZE + 7];
            for (int i = 0; i < large.length; i++) {
                large[i] = (byte) (i * 31 + i / 251);
            }
            assertEquals(large.length, EncryptedDataStore.writeToTheEnd(connection, id, new ByteArrayInputStream(large), "SHA-512"));
            assertArrayEquals(large, EncryptedDataStore.read(connection, id, 0, large.length));
            connection.rollback();
        }
    }

    private interface Write {
        void run() throws SQLException;
    }

    private static long allocatedBy(final com.sun.management.ThreadMXBean threads, final Write write) throws SQLException {
        final long thread = Thread.currentThread().threadId();
        final long before = threads.getThreadAllocatedBytes(thread);
        write.run();
        return threads.getThreadAllocatedBytes(thread) - before;
    }

    @Test
    void testH2() throws Exception {
        final DataSource dataSource = StoreTestDatabases.h2("store");
        runStoreChecks(dataSource);
        checkContract(dataSource);
    }

    /**
     * The edges of the store, on H2: connections without savepoints, or
     * which don't release them; a row without parts; a stream which ends at
     * the end of a part; and a stored part shorter than its recorded length.
     */
    @Test
    void testH2Edges() throws Exception {
        final DataSource dataSource = StoreTestDatabases.h2("store-edges");
        StoreTestDatabases.freshSchema(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            final long id = StoreTestDatabases.createDataRow(connection);
            final byte[] value = "The first content".getBytes(StandardCharsets.UTF_8);
            final byte[] other = "The other content".getBytes(StandardCharsets.UTF_8);

            // Without savepoints a write works, and a failed one is undone by
            // the rollback of the caller.
            try (Connection noSavepoints = failing(connection, "setSavepoint")) {
                EncryptedDataStore.write(noSavepoints, id, new ByteArrayInputStream(value), value.length, "SHA-512");
                connection.commit();
                final ByteArrayInputStream shorter = new ByteArrayInputStream(new byte[5]);
                assertThrows(SQLException.class, () -> EncryptedDataStore.write(noSavepoints, id, shorter, 10, "SHA-512"));
                connection.rollback();
                assertArrayEquals(value, EncryptedDataStore.read(connection, id, 0, value.length));
            }

            // A savepoint which isn't released ends with the transaction.
            try (Connection noRelease = failing(connection, "releaseSavepoint")) {
                EncryptedDataStore.write(noRelease, id, new ByteArrayInputStream(other), other.length, "SHA-512");
                connection.commit();
                assertArrayEquals(other, EncryptedDataStore.read(connection, id, 0, other.length));
            }

            // A row without parts has an empty head.
            final long empty = StoreTestDatabases.createDataRow(connection);
            final long metadataId = metadataId(connection, empty);
            assertEquals(new EncryptedDataStore.StoredHead(0, new byte[0]), EncryptedDataStore.heads(connection, List.of(metadataId), 4).get(metadataId));

            // A stream which ends with the first part, before the declared length.
            final ByteArrayInputStream onePart = new ByteArrayInputStream(new byte[DataPart.HEAD_SIZE]);
            final SQLException ended = assertThrows(SQLException.class, () -> EncryptedDataStore.write(connection, empty, onePart, DataPart.HEAD_SIZE + 10L, "SHA-512"));
            assertEquals("The stream ended after " + DataPart.HEAD_SIZE + " bytes, expected " + (DataPart.HEAD_SIZE + 10) + '.', ended.getMessage());
            connection.rollback();

            // A part with fewer bytes than its recorded length ends the data there.
            final long shortPart = StoreTestDatabases.createDataRow(connection);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO eds_data_parts (data_id, part_offset, part_length, part) VALUES (?, 0, 10, ?)")) {
                statement.setLong(1, shortPart);
                statement.setBytes(2, new byte[] { 1, 2, 3, 4 });
                statement.executeUpdate();
            }
            assertArrayEquals(new byte[] { 1, 2, 3, 4 }, EncryptedDataStore.read(connection, shortPart, 0, 10));
            try (InputStream stream = EncryptedDataStore.open(connection, shortPart, 0, 10, 64)) {
                final IOException cut = assertThrows(IOException.class, stream::readAllBytes);
                assertEquals("The stored data ended at 4, expected 10 bytes.", cut.getMessage());
            }
            connection.rollback();
        }
    }

    @Test
    void testPostgreSQL() throws Exception {
        final String url = StoreTestDatabases.postgresqlUrl("eds_store");
        assumeTrue(url != null, "No PostgreSQL test database configured");
        final DataSource dataSource = StoreTestDatabases.postgresql(url);
        runStoreChecks(dataSource);
        checkContract(dataSource);

        checkSnapshot(dataSource);

    }

    /**
     * A 512 MB blob is written and read back in batches by a JVM with a
     * 128 MB heap (PLAN.md, S1). Only on PostgreSQL; H2 holds the
     * value in memory.
     */
    @Test
    void testPostgreSQLWithLimitedHeap() throws Exception {
        final String url = StoreTestDatabases.postgresqlUrl("eds_store_heap");
        assumeTrue(url != null, "No PostgreSQL test database configured");

        final List<String> command = new ArrayList<>(List.of(
                System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
                "-Xmx128m", "-cp", System.getProperty("java.class.path"),
                HeapLimitedStoreCheck.class.getName(), url, String.valueOf(512L * MB)));
        runChild(command);

        // The same with the §4 format: encrypt while writing, and decrypt
        // the whole object and a range while reading (the download path).
        command.add("format");
        runChild(command);

        // And a copy to another Circle, re-encrypted chunk by chunk (S4).
        command.set(command.size() - 1, "copy");
        runChild(command);
    }

    // The command is this JVM's java with the test class path, nothing else.
    @SuppressWarnings("findsecbugs:COMMAND_INJECTION")
    private static void runChild(final List<String> command) throws Exception {
        final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(10, TimeUnit.MINUTES));
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("OK"), output);
    }

    /**
     * A download reads in batches, so an upload which commits in between
     * could mix two contents. With REPEATABLE READ (as the download sets,
     * PLAN.md, S3), all batches come from the snapshot; without it, the
     * later batches have the new content.
     */
    private static void checkSnapshot(final DataSource dataSource) throws Exception {
        final long size = 8L * MB;
        try (Connection writer = dataSource.getConnection(); Connection reader = dataSource.getConnection()) {
            writer.setAutoCommit(false);
            reader.setAutoCommit(false);
            final long id = StoreTestDatabases.createDataRow(writer);
            final byte[] first = generated(11, size, MessageDigest.getInstance("SHA-512"));
            final byte[] second = generated(12, size, MessageDigest.getInstance("SHA-512"));
            EncryptedDataStore.write(writer, id, new ByteArrayInputStream(first), size, "SHA-512");
            writer.commit();

            // With the snapshot: the rest still comes from the first content.
            try (var statement = reader.createStatement()) {
                statement.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY");
            }
            final byte[] snapshotHead = EncryptedDataStore.read(reader, id, 0, MB);
            EncryptedDataStore.write(writer, id, new ByteArrayInputStream(second), size, "SHA-512");
            writer.commit();
            final byte[] snapshotRest = readAll(reader, id, MB, size - MB, MB);
            reader.commit();
            assertArrayEquals(Arrays.copyOf(first, MB), snapshotHead);
            assertArrayEquals(Arrays.copyOfRange(first, MB, (int) size), snapshotRest);

            // Back to the first content, for the case without a snapshot.
            EncryptedDataStore.write(writer, id, new ByteArrayInputStream(first), size, "SHA-512");
            writer.commit();

            // Without a snapshot, an upload in between is visible: the rest
            // comes from the new content.
            final byte[] head = EncryptedDataStore.read(reader, id, 0, MB);
            EncryptedDataStore.write(writer, id, new ByteArrayInputStream(second), size, "SHA-512");
            writer.commit();
            final byte[] rest = readAll(reader, id, MB, size - MB, MB);
            reader.commit();
            assertArrayEquals(Arrays.copyOf(first, MB), head);
            assertArrayEquals(Arrays.copyOfRange(second, MB, (int) size), rest);
        }
    }

    // Seeded on purpose: the test data must be the same in every run.
    @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
    private static void runStoreChecks(final DataSource dataSource) throws Exception {
        StoreTestDatabases.freshSchema(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            final long id = StoreTestDatabases.createDataRow(connection);

            // Write 20 MB as a stream, with the checksum of the bytes written.
            final long size = 20L * MB + 123;
            final MessageDigest expected = MessageDigest.getInstance("SHA-512");
            final String checksum;
            try (InputStream generated = new StoreTestDatabases.GeneratedStream(7, size, expected)) {
                checksum = EncryptedDataStore.write(connection, id, generated, size, "SHA-512");
            }
            assertEquals(Base64.getEncoder().encodeToString(expected.digest()), checksum);
            assertEquals(size, EncryptedDataStore.length(connection, id));
            connection.commit();

            // It is stored in parts: a short first one, then 4 MB each, which
            // follow each other without a gap, and none is larger.
            try (var statement = connection.prepareStatement("SELECT part_offset, part_length, octet_length(part) FROM eds_data_parts WHERE data_id = ? ORDER BY part_offset")) {
                statement.setLong(1, id);
                try (var result = statement.executeQuery()) {
                    long next = 0;
                    int parts = 0;
                    while (result.next()) {
                        assertEquals(next, result.getLong(1));
                        assertEquals(result.getInt(2), result.getInt(3));
                        assertEquals((parts == 0) ? DataPart.HEAD_SIZE : Math.min(DataPart.DEFAULT_SIZE, size - next), result.getInt(2));
                        next += result.getInt(2);
                        parts++;
                    }
                    assertEquals(size, next);
                    assertEquals(6, parts);
                }
            }

            // The same content in parts of another size, and to the end of a
            // stream of unknown length: it reads back the same.
            final long other = StoreTestDatabases.createDataRow(connection);
            final MessageDigest otherWritten = MessageDigest.getInstance("SHA-512");
            try (InputStream generated = new StoreTestDatabases.GeneratedStream(7, size, otherWritten)) {
                assertEquals(size, EncryptedDataStore.writeToTheEnd(connection, other, generated, "SHA-512", MB));
            }
            connection.commit();
            assertEquals(size, EncryptedDataStore.length(connection, other));
            assertEquals(size, EncryptedDataStore.length(connection, id));
            final MessageDigest otherRead = MessageDigest.getInstance("SHA-512");
            try (InputStream stream = EncryptedDataStore.open(connection, other, 0, size, 4 * MB)) {
                otherRead.update(stream.readAllBytes());
            }
            assertArrayEquals(otherWritten.digest(), otherRead.digest());

            // A row without content has no parts, and the length 0.
            final long empty = StoreTestDatabases.createDataRow(connection);
            assertEquals(0, EncryptedDataStore.length(connection, empty));
            assertEquals(0, EncryptedDataStore.read(connection, empty, 0, 10).length);
            EncryptedDataStore.write(connection, empty, new ByteArrayInputStream(new byte[0]), 0, "SHA-512");
            assertEquals(0, EncryptedDataStore.length(connection, empty));
            connection.commit();

            // Read it back in batches, and compare.
            final MessageDigest original = MessageDigest.getInstance("SHA-512");
            final byte[] all = generated(7, size, original);
            final MessageDigest actual = MessageDigest.getInstance("SHA-512");
            try (InputStream stream = EncryptedDataStore.open(connection, id, 0, size, 4 * MB)) {
                actual.update(stream.readAllBytes());
            }
            assertArrayEquals(original.digest(), actual.digest());

            // Ranges, including the first and last byte and across batches.
            for (final long[] range : new long[][] { { 0, 1 }, { size - 1, 1 }, { 4L * MB - 3, 10 }, { 12345, 3 * MB }, { 0, 0 } }) {
                final byte[] read = readAll(connection, id, range[0], range[1], MB);
                assertArrayEquals(Arrays.copyOfRange(all, (int) range[0], (int) (range[0] + range[1])), read, "Range " + range[0] + '+' + range[1]);
            }
            assertArrayEquals(Arrays.copyOfRange(all, 100, 110), EncryptedDataStore.read(connection, id, 100, 10));

            // Reading past the end of the value fails, instead of returning
            // fewer bytes than asked for.
            try (InputStream past = EncryptedDataStore.open(connection, id, size - 5, 10, MB)) {
                assertThrows(IOException.class, past::readAllBytes);
            }

            // A stream with fewer bytes than declared doesn't commit a
            // shorter value.
            final byte[] small = new byte[100];
            new Random(1).nextBytes(small);
            final ByteArrayInputStream shorter = new ByteArrayInputStream(small);
            assertThrows(Exception.class, () -> EncryptedDataStore.write(connection, id, shorter, 200, "SHA-512"));
            // Also without a rollback: the failed write was undone by itself.
            assertEquals(size, EncryptedDataStore.length(connection, id));
            connection.rollback();
            assertEquals(size, EncryptedDataStore.length(connection, id));

            // An unknown row.
            assertEquals(-1, EncryptedDataStore.length(connection, id + 1000));
            final ByteArrayInputStream content = new ByteArrayInputStream(small);
            assertThrows(EDSException.class, () -> EncryptedDataStore.write(connection, id + 1000, content, 100, "SHA-512"));
            connection.rollback();
        }
    }

    /**
     * What the store promises besides reading and writing: on a connection
     * which commits by itself, a write is a transaction of its own, also when
     * it fails; invalid arguments and unknown rows are refused; and the heads
     * of the same content are equal.
     */
    private static void checkContract(final DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.getAutoCommit());
            final long id = StoreTestDatabases.createDataRow(connection);
            final byte[] value = "The content of the row".getBytes(StandardCharsets.UTF_8);
            assertEquals(value.length, EncryptedDataStore.writeToTheEnd(connection, id, new ByteArrayInputStream(value), "SHA-512"));
            assertTrue(connection.getAutoCommit());
            try (Connection other = dataSource.getConnection()) {
                assertArrayEquals(value, EncryptedDataStore.read(other, id, 0, value.length));
            }

            // A failed write is undone, and the connection still commits by itself.
            final ByteArrayInputStream shorter = new ByteArrayInputStream(new byte[5]);
            assertThrows(Exception.class, () -> EncryptedDataStore.write(connection, id, shorter, 10, "SHA-512"));
            assertTrue(connection.getAutoCommit());
            assertArrayEquals(value, EncryptedDataStore.read(connection, id, 0, value.length));

            final ByteArrayInputStream any = new ByteArrayInputStream(value);
            assertThrows(IllegalArgumentException.class, () -> EncryptedDataStore.write(connection, id, any, -1, "SHA-512"));
            assertThrows(IllegalArgumentException.class, () -> EncryptedDataStore.read(connection, id, -1, 1));
            assertThrows(IllegalArgumentException.class, () -> EncryptedDataStore.read(connection, id, 0, -1));
            assertThrows(IllegalArgumentException.class, () -> EncryptedDataStore.open(connection, id, -1, 1, 64));
            assertThrows(IllegalArgumentException.class, () -> EncryptedDataStore.open(connection, id, 0, -1, 64));
            assertThrows(IllegalArgumentException.class, () -> EncryptedDataStore.open(connection, id, 0, 1, 0));

            // The stored bytes read one at a time as well.
            try (InputStream stored = EncryptedDataStore.open(connection, id, 0, value.length, 64)) {
                assertEquals(value[0], stored.read());
                assertEquals(value[1], stored.read());
            }
            final EDSException unknown = assertThrows(EDSException.class, () -> EncryptedDataStore.read(connection, id + 1000, 0, 1));
            assertEquals(ReturnCode.IDENTIFICATION_WARNING, unknown.getReturnCode());

            final long metadataId = metadataId(connection, id);
            final EncryptedDataStore.StoredHead head = EncryptedDataStore.heads(connection, List.of(metadataId), 4).get(metadataId);
            assertEquals(head, EncryptedDataStore.heads(connection, List.of(metadataId), 4).get(metadataId));
            assertEquals(head.hashCode(), EncryptedDataStore.heads(connection, List.of(metadataId), 4).get(metadataId).hashCode());
            assertNotEquals(head, EncryptedDataStore.heads(connection, List.of(metadataId), 3).get(metadataId));
            assertNotEquals(head, new EncryptedDataStore.StoredHead(head.storedLength() + 1, head.head()));
            assertNotEquals(head, (Object) head.toString());
            assertArrayEquals(Arrays.copyOf(value, 4), head.head());
            assertEquals(value.length, head.storedLength());
            assertEquals("StoredHead[storedLength=" + value.length + ", head=4 bytes]", head.toString());
        }
    }

    /**
     * The connection, where the named method fails as on a database which
     * doesn't support it. Closing it leaves the connection open.
     */
    private static Connection failing(final Connection connection, final String unsupported) {
        return (Connection) Proxy.newProxyInstance(EncryptedDataStoreTest.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    if (unsupported.equals(method.getName())) {
                        throw new SQLFeatureNotSupportedException(unsupported + " is not supported.");
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    try {
                        return method.invoke(connection, arguments);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private static long metadataId(final Connection connection, final long dataId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT metadata_id FROM eds_data WHERE id = ?")) {
            statement.setLong(1, dataId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return fail("The data row " + dataId + " has no metadata.");
                }
                return result.getLong(1);
            }
        }
    }

    /** All bytes of a generated stream, which is closed after. */
    private static byte[] generated(final long seed, final long size, final MessageDigest digest) throws IOException {
        try (InputStream stream = new StoreTestDatabases.GeneratedStream(seed, size, digest)) {
            return stream.readAllBytes();
        }
    }

    /** A range of the stored bytes, read through a stream which is closed after. */
    private static byte[] readAll(final Connection connection, final long id, final long offset, final long length, final int batchSize) throws IOException {
        try (InputStream stream = EncryptedDataStore.open(connection, id, offset, length, batchSize)) {
            return stream.readAllBytes();
        }
    }
}
