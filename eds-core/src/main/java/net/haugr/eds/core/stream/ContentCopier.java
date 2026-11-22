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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.exceptions.CryptoException;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.jce.DataStreams;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.ConnectionSource;
import net.haugr.eds.core.model.EncryptedDataStore;

/**
 * <p>Copies the content of a Data Object to another one, re-encrypting it
 * chunk by chunk for the target (PLAN.md, S4): for a copy, or a move to
 * another Circle. The plaintext never exists in full.</p>
 *
 * <p>The target is written as one streamed statement on the request's
 * Connection, and while that statement runs, the Connection can't also run
 * the queries which read the source. So the source is read on a Connection
 * of its own, by a reader thread, in a read-only transaction with a stable
 * snapshot (as a download), and handed over in small chunks through a
 * bounded queue: the data flows from the source, through the decryption and
 * the new encryption, to the target, with only a few chunks in memory.</p>
 *
 * <p>Content up to the given threshold (the JSON inline limit) is copied in
 * memory on the request's Connection instead, so it also sees content written
 * earlier in the same transaction.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class ContentCopier {

    /** Bytes per chunk handed from the reader to the writer. */
    private static final int CHUNK_BYTES = 65536;
    /** Chunks waiting in the queue, at most. */
    private static final int QUEUE_CHUNKS = 4;
    private static final long JOIN_MILLIS = 10_000;

    private ContentCopier() {
        // Utility class.
    }

    /**
     * The source of a copy.
     *
     * @param dataId       Internal Id of the source content
     * @param storedLength Stored length of the source
     * @param circleSecret Circle Secret of the source
     * @param context      The source Data Object
     */
    public record Source(long dataId, long storedLength, SecretEDSKey circleSecret, DataFormat.Context context) {
    }

    /**
     * The target of a copy.
     *
     * @param dataId       Internal Id of the target row, which exists
     * @param circleSecret Circle Secret of the target
     * @param context      The target Data Object
     * @param rotation     True for the same Data Object under a new Circle
     *                     Secret (PLAN.md, Crypto C6): its header, and so its
     *                     ETag, stays
     */
    public record Target(long dataId, SecretEDSKey circleSecret, DataFormat.Context context, boolean rotation) {

        /**
         * The target of a copy to another Data Object.
         *
         * @param dataId       Internal Id of the target row, which exists
         * @param circleSecret Circle Secret of the target
         * @param context      The target Data Object
         */
        public Target(final long dataId, final SecretEDSKey circleSecret, final DataFormat.Context context) {
            this(dataId, circleSecret, context, false);
        }
    }

    /**
     * Copies and re-encrypts the content.
     *
     * @param connection      JDBC Connection, in the caller's transaction
     * @param readers         Opens the Connection for reading a large source
     * @param source          The source
     * @param target          The target
     * @param batchSize       Bytes of a part the copy is stored in, which is what a query reads
     * @param digestAlgorithm Digest for the checksum
     * @param memoryThreshold Largest source copied in memory
     * @return Checksum of the target
     * @throws SQLException if the database fails
     */
    public static String copy(final Connection connection, final ConnectionSource readers, final Source source, final Target target,
                              final int batchSize, final String digestAlgorithm, final long memoryThreshold) throws SQLException {
        if (source.storedLength() <= memoryThreshold) {
            return copyInMemory(connection, source, target, batchSize, digestAlgorithm);
        }
        if (readers == null) {
            throw new EDSException(ReturnCode.ERROR, "Large content can't be copied, as no database connection for reading the source is configured.");
        }

        return copyStreamed(connection, readers, source, target, batchSize, digestAlgorithm);
    }

    private static String copyInMemory(final Connection connection, final Source source, final Target target, final int batchSize, final String digestAlgorithm) throws SQLException {
        // Closing the streams frees nothing, as they only read from the
        // connection, which is the caller's; it is done for good order.
        try (DataStreams.EncryptingStream encrypting = reencrypt(source, target,
                EncryptedDataStore.open(connection, source.dataId(), 0, source.storedLength(), batchSize))) {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream((int) encrypting.storedLength());
            encrypting.transferTo(buffer);
            return EncryptedDataStore.write(connection, target.dataId(), new ByteArrayInputStream(buffer.toByteArray()), encrypting.storedLength(), digestAlgorithm, batchSize);
        } catch (IOException | CryptoException e) {
            throw copyFailed(e);
        }
    }

    private static String copyStreamed(final Connection connection, final ConnectionSource readers, final Source source, final Target target,
                                       final int batchSize, final String digestAlgorithm) throws SQLException {
        try (Reader reader = Reader.start(readers, source, batchSize)) {
            return write(connection, reader, source, target, batchSize, digestAlgorithm);
        }
    }

    private static String write(final Connection connection, final Reader reader, final Source source, final Target target,
                                final int batchSize, final String digestAlgorithm) throws SQLException {
        TrackingStream tracking = null;
        // Closing the streams frees nothing, as the reader is closed by the
        // caller; it is done for good order.
        try (DataStreams.EncryptingStream encrypting = reencrypt(source, target, reader.stream())) {
            tracking = new TrackingStream(encrypting);
            return EncryptedDataStore.write(connection, target.dataId(), tracking, encrypting.storedLength(), digestAlgorithm, batchSize);
        } catch (IOException | CryptoException e) {
            throw copyFailed(e);
        } catch (SQLException e) {
            // A failure of the source is reported by the database driver as a
            // failure of the write; the cause is the one which matters.
            if ((tracking != null) && (tracking.failure != null)) {
                throw copyFailed(tracking.failure);
            }
            throw e;
        }
    }

    private static DataStreams.EncryptingStream reencrypt(final Source source, final Target target, final InputStream stored) throws IOException {
        return target.rotation()
                ? DataStreams.rotate(source.circleSecret(), source.context(), stored, source.storedLength(), target.circleSecret(), target.context())
                : DataStreams.reencrypt(source.circleSecret(), source.context(), stored, source.storedLength(), target.circleSecret(), target.context());
    }

    private static EDSException copyFailed(final Exception e) {
        // A database failure while reading the source, or else the source
        // failed its check.
        final ReturnCode returnCode = (e.getCause() instanceof SQLException) ? ReturnCode.DATABASE_ERROR : ReturnCode.INTEGRITY_ERROR;
        return new EDSException(returnCode, "The content could not be copied: " + e.getMessage(), e);
    }

    /**
     * Reads the stored source on a Connection of its own, and hands it over
     * in chunks. It stops when it is closed, also if the write failed, and
     * then waits a little for its thread to end.
     */
    private static final class Reader implements Runnable, AutoCloseable {

        private static final byte[] END = new byte[0];

        private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(QUEUE_CHUNKS);
        private final ConnectionSource readers;
        private final Source source;
        private final int batchSize;
        private final AtomicReference<Thread> thread = new AtomicReference<>();
        private volatile boolean cancelled = false;
        private final AtomicReference<Exception> failure = new AtomicReference<>();

        private Reader(final ConnectionSource readers, final Source source, final int batchSize) {
            this.readers = readers;
            this.source = source;
            this.batchSize = batchSize;
        }

        private static Reader start(final ConnectionSource readers, final Source source, final int batchSize) {
            final Reader reader = new Reader(readers, source, batchSize);
            final Thread started = Thread.ofVirtual().name("eds-copy-reader-" + source.dataId()).unstarted(reader);
            reader.thread.set(started);
            started.start();
            return reader;
        }

        @Override
        public void run() {
            try (Connection connection = readers.open()) {
                connection.setAutoCommit(false);
                connection.setReadOnly(true);
                connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                try (InputStream stored = EncryptedDataStore.open(connection, source.dataId(), 0, source.storedLength(), batchSize)) {
                    byte[] chunk = stored.readNBytes(CHUNK_BYTES);
                    while ((chunk.length > 0) && !cancelled) {
                        put(chunk);
                        chunk = stored.readNBytes(CHUNK_BYTES);
                    }
                } finally {
                    // Read only, nothing to commit.
                    connection.rollback();
                }
            } catch (IOException | SQLException | RuntimeException e) {
                failure.set(e);
            } catch (InterruptedException e) {
                failure.set(e);
                Thread.currentThread().interrupt();
                return;
            }

            try {
                put(END);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }

        private void put(final byte[] chunk) throws InterruptedException {
            // Waiting for the writer, unless it gave up.
            boolean offered = false;
            while (!cancelled && !offered) {
                offered = queue.offer(chunk, 100, TimeUnit.MILLISECONDS);
            }
        }

        @Override
        public void close() {
            cancelled = true;
            queue.clear();
            final Thread running = thread.get();
            try {
                if (!running.join(Duration.ofMillis(JOIN_MILLIS))) {
                    running.interrupt();
                }
            } catch (InterruptedException _) {
                running.interrupt();
                Thread.currentThread().interrupt();
            }
        }

        private InputStream stream() {
            return new Chunks();
        }

        /** The chunks of the reader, as a stream. */
        private final class Chunks extends InputStream {

            private byte[] current = new byte[0];
            private int position = 0;
            private boolean ended = false;

            @Override
            public int read() throws IOException {
                final byte[] single = new byte[1];
                return (read(single, 0, 1) == 1) ? (single[0] & 0xFF) : -1;
            }

            @Override
            public int read(final byte[] bytes, final int offset, final int length) throws IOException {
                if (length == 0) {
                    return 0;
                }
                while (position == current.length) {
                    if (ended) {
                        return -1;
                    }
                    next();
                }
                final int count = Math.min(length, current.length - position);
                System.arraycopy(current, position, bytes, offset, count);
                position += count;
                return count;
            }

            private void next() throws IOException {
                final byte[] chunk = take();
                if (chunk == END) {
                    ended = true;
                    final Exception cause = failure.get();
                    if (cause != null) {
                        throw new IOException("The source could not be read: " + cause.getMessage(), cause);
                    }
                }
                current = chunk;
                position = 0;
            }

            /** The next chunk, as soon as the reader has it. */
            private byte[] take() throws IOException {
                try {
                    byte[] chunk = queue.poll(1, TimeUnit.SECONDS);
                    while (chunk == null) {
                        if (!thread.get().isAlive() && queue.isEmpty()) {
                            throw new IOException("The source reader stopped unexpectedly.");
                        }
                        chunk = queue.poll(1, TimeUnit.SECONDS);
                    }
                    return chunk;
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException("Interrupted while reading the source.");
                }
            }
        }
    }

    /**
     * Remembers the first failure of the stream (an IOException, or a
     * CryptoException of a damaged chunk), as the database driver reports it
     * as a failure of the statement.
     */
    private static final class TrackingStream extends FilterInputStream {

        private Exception failure = null;

        private TrackingStream(final InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            try {
                return super.read();
            } catch (IOException | RuntimeException e) {
                failure = (failure != null) ? failure : e;
                throw e;
            }
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) throws IOException {
            try {
                return super.read(bytes, offset, length);
            } catch (IOException | RuntimeException e) {
                failure = (failure != null) ? failure : e;
                throw e;
            }
        }
    }
}
