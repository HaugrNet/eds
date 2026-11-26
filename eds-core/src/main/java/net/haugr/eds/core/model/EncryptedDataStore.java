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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.entities.DataPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Streamed access to the encrypted data of a Data Object via JDBC
 * (PLAN.md, Streaming). The data is stored as a row of
 * {@code eds_data_parts} for each part of it (see {@link DataPart}): it is
 * written part by part as it arrives, and read part by part, one query for
 * each, so memory is bounded by one part. JPA reads and writes the same rows
 * as a whole (see {@code DataEntity}), for data small enough for it.</p>
 *
 * <p>The methods use the given connection and don't commit; the caller's
 * transaction decides. On a connection which commits by itself, a write is a
 * transaction of its own, so it is all or nothing there too. The database
 * only sees opaque bytes: a part is a range of them, and where it ends says
 * nothing about the content.</p>
 *
 * <p>Only SQL which every database knows is used: no {@code substring} or
 * {@code octet_length} of a binary value, and no value above the size of a
 * part ({@link DataPart#MAX_SIZE} at most). Before the data was stored in
 * parts, it was one value of up to 1000 MiB, which PostgreSQL could read in
 * ranges ({@code STORAGE EXTERNAL}) while H2 held it in memory as a whole;
 * now neither holds more than a part.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class EncryptedDataStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedDataStore.class);

    private static final String WRITE_CHECKSUM = "UPDATE eds_data SET checksum = ? WHERE id = ?";
    private static final String DELETE_PARTS = "DELETE FROM eds_data_parts WHERE data_id = ?";
    private static final String INSERT_PART = "INSERT INTO eds_data_parts (data_id, part_offset, part_length, part) VALUES (?, ?, ?, ?)";
    // A row for every eds_data row, also for one without parts; none if
    // there is no such eds_data row.
    private static final String READ_LENGTH = "SELECT COALESCE(SUM(p.part_length), 0) FROM eds_data d LEFT JOIN eds_data_parts p ON p.data_id = d.id WHERE d.id = ? GROUP BY d.id";
    private static final String READ_HEADS = "SELECT d.metadata_id, (SELECT COALESCE(SUM(a.part_length), 0) FROM eds_data_parts a WHERE a.data_id = d.id), h.part " +
            "FROM eds_data d LEFT JOIN eds_data_parts h ON h.data_id = d.id AND h.part_offset = 0 WHERE d.metadata_id IN (";
    // The part which holds the byte at an offset
    private static final String READ_PART = "SELECT part_offset, part FROM eds_data_parts WHERE data_id = ? AND part_offset <= ? AND part_offset + part_length > ?";
    // Placeholder checksum, replaced in the same transaction once the
    // stream has been read and the digest is known.
    private static final String PENDING = "pending";
    private static final long TO_THE_END = -1;

    private EncryptedDataStore() {
        // Utility class.
    }

    /**
     * Writes the encrypted data of an existing row as a stream of exactly
     * the given length, in parts of the default size, and stores the digest
     * of the bytes written as the checksum (for the sanity job, PLAN.md,
     * Crypto §4). What was stored for the row is replaced.
     *
     * @param connection      JDBC Connection, in the caller's transaction
     * @param dataId          Internal Id of the eds_data row
     * @param encrypted       Stream of the encrypted data
     * @param length          Exact length of the stream
     * @param digestAlgorithm Digest algorithm for the checksum, e.g. SHA-512
     * @return The checksum, Base64 encoded
     * @throws SQLException if the database write fails, or the stream fails or ends early
     */
    public static String write(final Connection connection, final long dataId, final InputStream encrypted, final long length, final String digestAlgorithm) throws SQLException {
        return write(connection, dataId, encrypted, length, digestAlgorithm, DataPart.DEFAULT_SIZE);
    }

    /**
     * Writes the encrypted data of an existing row as a stream of exactly
     * the given length, as {@link #write(Connection, long, InputStream, long, String)}
     * does, in parts of the given size.
     *
     * @param connection      JDBC Connection, in the caller's transaction
     * @param dataId          Internal Id of the eds_data row
     * @param encrypted       Stream of the encrypted data
     * @param length          Exact length of the stream
     * @param digestAlgorithm Digest algorithm for the checksum, e.g. SHA-512
     * @param partSize        Bytes of a part, see {@link DataPart#size(int)}
     * @return The checksum, Base64 encoded
     * @throws SQLException if the database write fails, or the stream fails or ends early
     */
    public static String write(final Connection connection, final long dataId, final InputStream encrypted, final long length, final String digestAlgorithm, final int partSize) throws SQLException {
        if (length < 0) {
            throw new IllegalArgumentException("Invalid length.");
        }
        final MessageDigest digest = newDigest(digestAlgorithm);
        final InputStream stream = Objects.requireNonNull(encrypted);
        final String[] checksum = new String[1];
        atomic(connection, () -> {
            store(connection, dataId, stream, digest, length, partSize);
            checksum[0] = Base64.getEncoder().encodeToString(digest.digest());
            writeChecksum(connection, dataId, checksum[0]);
        });

        return checksum[0];
    }

    /**
     * Writes the encrypted data of an existing row as a stream which is
     * read to its end, for content of unknown length (a pipe), in parts of
     * the default size, and stores the digest as
     * {@link #write(Connection, long, InputStream, long, String)} does.
     *
     * @param connection      JDBC Connection, in the caller's transaction
     * @param dataId          Internal Id of the eds_data row
     * @param encrypted       Stream of the encrypted data
     * @param digestAlgorithm Digest algorithm for the checksum, e.g. SHA-512
     * @return The number of bytes written
     * @throws SQLException if the database write fails, or the stream fails
     */
    public static long writeToTheEnd(final Connection connection, final long dataId, final InputStream encrypted, final String digestAlgorithm) throws SQLException {
        return writeToTheEnd(connection, dataId, encrypted, digestAlgorithm, DataPart.DEFAULT_SIZE);
    }

    /**
     * Writes the encrypted data of an existing row as a stream which is
     * read to its end, as {@link #writeToTheEnd(Connection, long, InputStream, String)}
     * does, in parts of the given size. Each part is written as it has
     * arrived, so nothing is spooled, whatever the database.
     *
     * @param connection      JDBC Connection, in the caller's transaction
     * @param dataId          Internal Id of the eds_data row
     * @param encrypted       Stream of the encrypted data
     * @param digestAlgorithm Digest algorithm for the checksum, e.g. SHA-512
     * @param partSize        Bytes of a part, see {@link DataPart#size(int)}
     * @return The number of bytes written
     * @throws SQLException if the database write fails, or the stream fails
     */
    public static long writeToTheEnd(final Connection connection, final long dataId, final InputStream encrypted, final String digestAlgorithm, final int partSize) throws SQLException {
        final MessageDigest digest = newDigest(digestAlgorithm);
        final InputStream stream = Objects.requireNonNull(encrypted);
        final long[] written = new long[1];
        atomic(connection, () -> {
            written[0] = store(connection, dataId, stream, digest, TO_THE_END, partSize);
            writeChecksum(connection, dataId, Base64.getEncoder().encodeToString(digest.digest()));
        });

        return written[0];
    }

    /**
     * Returns the stored length of the encrypted data, without reading it.
     *
     * @param connection JDBC Connection
     * @param dataId     Internal Id of the eds_data row
     * @return Length in bytes, or -1 if there is no such row
     * @throws SQLException if the query fails
     */
    public static long length(final Connection connection, final long dataId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(READ_LENGTH)) {
            statement.setLong(1, dataId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : -1;
            }
        }
    }

    /**
     * The stored length and the first bytes of the content of a Data Object.
     *
     * @param storedLength Length of the stored (encrypted) value
     * @param head         The first bytes of the stored value
     */
    public record StoredHead(long storedLength, byte[] head) {

        @Override
        public boolean equals(final Object other) {
            return (other instanceof StoredHead(final long otherLength, final byte[] otherHead))
                    && (storedLength == otherLength) && Arrays.equals(head, otherHead);
        }

        @Override
        public int hashCode() {
            return (31 * Long.hashCode(storedLength)) + Arrays.hashCode(head);
        }

        @Override
        public String toString() {
            return "StoredHead[storedLength=" + storedLength + ", head=" + head.length + " bytes]";
        }
    }

    /**
     * Reads the stored length and the first bytes of the content of the
     * given Data Objects in one query, without reading the content, e.g. to
     * give the size of each entry of a folder listing. Only the first part
     * of each is read, which is short ({@link DataPart#HEAD_SIZE}), so no
     * more than that many bytes are given.
     *
     * @param connection  JDBC Connection
     * @param metadataIds Internal Ids of the eds_metadata rows
     * @param headLength  Number of bytes to read from the start of each value
     * @return The stored heads by metadata Id; Data Objects without content are missing
     * @throws SQLException if the query fails
     */
    // The statement only gets a '?' per Id; every value is bound with setLong.
    @SuppressWarnings({"findsecbugs:SQL_INJECTION_JDBC", "java:S2077"})
    public static Map<Long, StoredHead> heads(final Connection connection, final Collection<Long> metadataIds, final int headLength) throws SQLException {
        final Map<Long, StoredHead> heads = new HashMap<>();
        if (metadataIds.isEmpty()) {
            return heads;
        }

        final String sql = READ_HEADS + String.join(", ", Collections.nCopies(metadataIds.size(), "?")) + ')';
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (final Long id : metadataIds) {
                statement.setLong(index++, id);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final byte[] first = result.getBytes(3);
                    final byte[] head = (first == null) ? new byte[0] : Arrays.copyOf(first, Math.clamp(headLength, 0, first.length));
                    heads.put(result.getLong(1), new StoredHead(result.getLong(2), head));
                }
            }
        }

        return heads;
    }

    /**
     * Reads a range of the encrypted data into memory.
     *
     * @param connection JDBC Connection
     * @param dataId     Internal Id of the eds_data row
     * @param offset     Offset of the first byte, from 0
     * @param length     Number of bytes
     * @return The bytes, fewer if the value ends before
     * @throws SQLException if the query fails
     */
    public static byte[] read(final Connection connection, final long dataId, final long offset, final int length) throws SQLException {
        if ((offset < 0) || (length < 0)) {
            throw new IllegalArgumentException("Invalid offset or length.");
        }
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.min(length, DataPart.MAX_SIZE));
        long next = offset;
        final long end = offset + length;
        while (next < end) {
            final Part part = readPart(connection, dataId, next);
            if (part == null) {
                break;
            }
            final int from = (int) (next - part.offset);
            final int count = (int) Math.min((long) part.bytes.length - from, end - next);
            bytes.write(part.bytes, from, count);
            next += count;
        }
        if ((next == offset) && (length(connection, dataId) < 0)) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "No data found for the internal Id " + dataId + '.');
        }

        return bytes.toByteArray();
    }

    /**
     * Opens a stream over a range of the encrypted data, which reads it
     * part by part, one query for each. The stream is only valid while the
     * connection is, and should be read in a transaction with a stable
     * snapshot (REPEATABLE READ), see PLAN.md, Streaming.
     *
     * @param connection JDBC Connection
     * @param dataId     Internal Id of the eds_data row
     * @param offset     Offset of the first byte, from 0
     * @param length     Number of bytes to read
     * @param batchSize  Bytes per query before the data was stored in parts;
     *                   a query now reads a part, of the size it was written with
     * @return Stream of the bytes
     */
    public static InputStream open(final Connection connection, final long dataId, final long offset, final long length, final int batchSize) {
        if ((offset < 0) || (length < 0) || (batchSize <= 0)) {
            throw new IllegalArgumentException("Invalid offset, length or batch size.");
        }

        return new PartStream(connection, dataId, offset, length);
    }

    // =========================================================================
    // Internal methods
    // =========================================================================

    private interface Work {
        void run() throws SQLException;
    }

    /**
     * A write is several statements, and a failed write must leave what was
     * stored before, as it did when it was one statement. In the caller's
     * transaction it is undone to a savepoint, so the transaction can go on;
     * where there are no savepoints, the caller's rollback undoes it. On a
     * connection which commits by itself, the write is a transaction here.
     */
    private static void atomic(final Connection connection, final Work work) throws SQLException {
        if (!connection.getAutoCommit()) {
            inCallersTransaction(connection, work);
            return;
        }

        connection.setAutoCommit(false);
        try {
            work.run();
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static void inCallersTransaction(final Connection connection, final Work work) throws SQLException {
        final Savepoint savepoint = savepoint(connection);
        try {
            work.run();
        } catch (SQLException | RuntimeException e) {
            undo(connection, savepoint, e);
            throw e;
        }
        release(connection, savepoint);
    }

    private static Savepoint savepoint(final Connection connection) {
        try {
            return connection.setSavepoint();
        } catch (SQLException e) {
            // Not on this connection: the transaction of the caller decides.
            LOGGER.trace("No savepoint on this connection.", e);
            return null;
        }
    }

    private static void undo(final Connection connection, final Savepoint savepoint, final Exception cause) {
        if (savepoint != null) {
            try {
                connection.rollback(savepoint);
            } catch (SQLException undo) {
                cause.addSuppressed(undo);
            }
        }
    }

    private static void release(final Connection connection, final Savepoint savepoint) {
        if (savepoint != null) {
            try {
                connection.releaseSavepoint(savepoint);
            } catch (SQLException e) {
                // Not every database releases a savepoint; it ends with the transaction.
                LOGGER.trace("The savepoint is not released before the transaction ends.", e);
            }
        }
    }

    /**
     * Replaces the parts of a row by those of the stream. With a length, the
     * stream must give exactly that many bytes, and no more are read; without
     * one ({@link #TO_THE_END}), it is read to its end. Each part is added
     * to the digest as it is written.
     *
     * @return The number of bytes written
     */
    private static long store(final Connection connection, final long dataId, final InputStream stream, final MessageDigest digest,
                              final long length, final int wantedSize) throws SQLException {
        // First, as it tells whether there is such a row at all.
        writeChecksum(connection, dataId, PENDING);
        try (PreparedStatement statement = connection.prepareStatement(DELETE_PARTS)) {
            statement.setLong(1, dataId);
            statement.executeUpdate();
        }

        final int partSize = DataPart.size(wantedSize);
        // No more room than the content needs (ticket 36): with a length, as
        // much as the largest of its parts; without, that of a smallest part,
        // which grows as the stream gives more.
        final PartBuffer buffer = new PartBuffer((length == TO_THE_END)
                ? Math.min(DataPart.MIN_SIZE, partSize)
                : (int) Math.min(partSize, length));
        long offset = 0;
        try (PreparedStatement statement = connection.prepareStatement(INSERT_PART)) {
            // A parameter stays set for each execution, until it is set again.
            statement.setLong(1, dataId);
            while ((length == TO_THE_END) || (offset < length)) {
                int wanted = DataPart.sizeAt(offset, partSize);
                if (length != TO_THE_END) {
                    wanted = (int) Math.min(wanted, length - offset);
                }
                final int read = buffer.fill(stream, wanted);
                if (read > 0) {
                    digest.update(buffer.bytes(), 0, read);
                    statement.setLong(2, offset);
                    statement.setInt(3, read);
                    statement.setBinaryStream(4, new ByteArrayInputStream(buffer.bytes(), 0, read), read);
                    statement.executeUpdate();
                    offset += read;
                }
                if (read < wanted) {
                    break;
                }
            }
        }
        if ((length != TO_THE_END) && (offset != length)) {
            throw new SQLException("The stream ended after " + offset + " bytes, expected " + length + '.');
        }

        return offset;
    }

    /**
     * The bytes of one part, read from the stream. It starts as large as the
     * caller expects a part to need, and grows, up to the size of a part,
     * only if the stream gives more (ticket 36): a buffer of a whole part
     * (4 MiB) for each write was most of what the stone allocated.
     */
    private static final class PartBuffer {

        private byte[] bytes;

        private PartBuffer(final int size) {
            bytes = new byte[size];
        }

        private byte[] bytes() {
            return bytes;
        }

        /**
         * Reads from the stream until the wanted number of bytes is there, or
         * it ends. A failure of the stream, whatever the stream was, is given
         * on as a failure of the write, as the database driver did when it
         * read the stream itself.
         */
        private int fill(final InputStream stream, final int wanted) throws SQLException {
            int filled = 0;
            try {
                while (filled < wanted) {
                    if (filled == bytes.length) {
                        bytes = Arrays.copyOf(bytes, Math.clamp(2L * bytes.length, Math.min(DataPart.HEAD_SIZE, wanted), wanted));
                    }
                    final int read = stream.read(bytes, filled, Math.min(wanted, bytes.length) - filled);
                    if (read < 0) {
                        break;
                    }
                    filled += read;
                }
            } catch (IOException | RuntimeException e) {
                throw new SQLException("The stream of the data failed: " + e.getMessage(), e);
            }

            return filled;
        }
    }

    private static void writeChecksum(final Connection connection, final long dataId, final String checksum) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(WRITE_CHECKSUM)) {
            statement.setString(1, checksum);
            statement.setLong(2, dataId);
            requireOneRow(statement.executeUpdate(), dataId);
        }
    }

    /** A stored part: where it starts in the value, and its bytes. */
    private static final class Part {

        private final long offset;
        private final byte[] bytes;

        private Part(final long offset, final byte[] bytes) {
            this.offset = offset;
            this.bytes = bytes;
        }
    }

    /**
     * Reads the part which holds the byte at the given offset.
     *
     * @return The part, or null if the data ends before the offset
     */
    private static Part readPart(final Connection connection, final long dataId, final long offset) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(READ_PART)) {
            statement.setLong(1, dataId);
            statement.setLong(2, offset);
            statement.setLong(3, offset);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                final long start = result.getLong(1);
                final byte[] bytes = result.getBytes(2);
                return ((bytes == null) || (bytes.length <= offset - start)) ? null : new Part(start, bytes);
            }
        }
    }

    private static MessageDigest newDigest(final String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new EDSException(ReturnCode.CRYPTO_ERROR, e.getMessage(), e);
        }
    }

    private static void requireOneRow(final int updated, final long dataId) {
        if (updated != 1) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "No data found for the internal Id " + dataId + '.');
        }
    }

    private static final class PartStream extends InputStream {

        private final Connection connection;
        private final long dataId;
        private final long end;
        private long next;
        private byte[] part = new byte[0];
        private int position = 0;
        private int limit = 0;

        private PartStream(final Connection connection, final long dataId, final long offset, final long length) {
            this.connection = connection;
            this.dataId = dataId;
            this.next = offset;
            this.end = offset + length;
        }

        @Override
        public int read() throws IOException {
            if (!fill()) {
                return -1;
            }

            return part[position++] & 0xFF;
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            if (length == 0) {
                return 0;
            }
            if (!fill()) {
                return -1;
            }
            final int count = Math.min(length, limit - position);
            System.arraycopy(part, position, bytes, offset, count);
            position += count;

            return count;
        }

        private boolean fill() throws IOException {
            if (position < limit) {
                return true;
            }
            if (next >= end) {
                return false;
            }

            final Part read;
            try {
                read = readPart(connection, dataId, next);
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
            if (read == null) {
                // The stored value is shorter than expected.
                throw new IOException("The stored data ended at " + next + ", expected " + end + " bytes.");
            }
            part = read.bytes;
            position = (int) (next - read.offset);
            limit = (int) Math.min(part.length, end - read.offset);
            next = read.offset + limit;

            return true;
        }
    }
}
