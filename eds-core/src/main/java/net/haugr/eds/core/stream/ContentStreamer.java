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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import net.haugr.eds.core.exceptions.CryptoException;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.jce.DataStreams;
import net.haugr.eds.core.model.EncryptedDataStore;

/**
 * <p>Streams a prepared download (PLAN.md, S3) from the database to the
 * client: batched reads, chunk by chunk decryption, and trimming to the
 * range. A chunk is never written before its tag has been verified.</p>
 *
 * <p>On PostgreSQL the transaction is set to {@code REPEATABLE READ READ
 * ONLY}, so all batches come from one snapshot, even if an upload commits
 * meanwhile. The header is read again in that snapshot: if the content has
 * changed since the download was prepared, the download is aborted, rather
 * than mixing two contents. (On H2 there is no snapshot; a change mid-stream
 * makes a chunk fail its tag or the stored bytes end early, and the download
 * is aborted. Before that counts as damage, the content is compared with
 * what was prepared once more: content which changed is not damaged.)</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class ContentStreamer {

    private static final int COPY_BUFFER = 64 * 1024;

    /** The client went away while writing; not a problem of the content. */
    public static final class ClientAbortException extends IOException {

        private static final long serialVersionUID = 1L;

        ClientAbortException(final IOException cause) {
            super(cause.getMessage(), cause);
        }
    }

    /**
     * The download failed for a reason other than the client: the stored
     * content failed its check (damaged), or it changed or couldn't be read
     * from the database (not damaged).
     */
    public static final class ContentException extends IOException {

        private static final long serialVersionUID = 1L;
        private final boolean damaged;

        ContentException(final String message, final Throwable cause, final boolean damaged) {
            super(message, cause);
            this.damaged = damaged;
        }

        /** True if the stored content is damaged, not just changed. */
        public boolean isDamaged() {
            return damaged;
        }
    }

    private ContentStreamer() {
        // Utility class.
    }

    /**
     * Streams the download to the output, in the connection's transaction.
     *
     * @param connection JDBC Connection, in a transaction of its own
     * @param download   The prepared download
     * @param output     The response body
     * @param batchSize  Bytes per database query
     * @throws IOException ClientAbortException if writing or flushing failed, else ContentException
     */
    public static void stream(final Connection connection, final ContentDownload download, final OutputStream output, final int batchSize) throws IOException {
        final OutputStream out = new ClientOutput(output);

        try {
            snapshot(connection);
            final ContentDownload.Reading reading = changed(connection, download) ? null : download.reading(storedKey(connection, download.dataId()));
            if (reading == null) {
                throw new ContentException("The content changed while the download started.", null, false);
            }

            final InputStream plain = plain(connection, download, reading, batchSize);

            final byte[] buffer = new byte[COPY_BUFFER];
            int read = plain.read(buffer);
            while (read != -1) {
                out.write(buffer, 0, read);
                read = plain.read(buffer);
            }
            out.flush();
        } catch (ClientAbortException | ContentException e) {
            throw e;
        } catch (IOException e) {
            // The stream of the stored bytes wraps what the database throws.
            if (causedByTheDatabase(e)) {
                throw unreadable(e);
            }
            throw failedCheck(connection, download, e);
        } catch (CryptoException e) {
            throw failedCheck(connection, download, e);
        } catch (SQLException | EDSException e) {
            // The database failed, or e.g. the content was deleted meanwhile;
            // a CryptoException, which is an EDSException too, is caught above.
            throw unreadable(e);
        }
    }

    /**
     * A chunk failed its tag, or the stored bytes are not what the header
     * tells. That is damage, unless the content was replaced while it was
     * streamed, which the snapshot rules out on PostgreSQL only. If that
     * can't be told, as the database fails now, it isn't called damage.
     */
    private static ContentException failedCheck(final Connection connection, final ContentDownload download, final Exception cause) {
        boolean changed;
        try {
            changed = changed(connection, download);
        } catch (SQLException | EDSException e) {
            cause.addSuppressed(e);
            changed = true;
        }
        if (changed) {
            return new ContentException("The content changed while it was streamed: " + cause.getMessage(), cause, false);
        }
        return new ContentException("The stored content failed its check: " + cause.getMessage(), cause, true);
    }

    /** Whether the stored content is no longer the one the download was prepared for. */
    private static boolean changed(final Connection connection, final ContentDownload download) throws SQLException {
        final long id = download.dataId();
        return (EncryptedDataStore.length(connection, id) != download.storedLength())
                || !Arrays.equals(EncryptedDataStore.read(connection, id, 0, DataFormat.HEADER_BYTES), download.header().encoded());
    }

    /**
     * The database gave up (connection lost, transaction timeout, deleted
     * row): that tells nothing about the content, which must not be marked
     * as failed for it.
     */
    private static ContentException unreadable(final Exception cause) {
        return new ContentException("The stored content could not be read: " + cause.getMessage(), cause, false);
    }

    private static boolean causedByTheDatabase(final Throwable thrown) {
        for (Throwable cause = thrown.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException) {
                return true;
            }
        }
        return false;
    }

    private static void snapshot(final Connection connection) throws SQLException {
        if ("PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY");
            }
        }
    }

    /**
     * The plain content of the download, decrypted as it is read. Empty
     * content has no plan, but its single chunk is still verified.
     */
    private static InputStream plain(final Connection connection, final ContentDownload download, final ContentDownload.Reading reading,
                                     final int batchSize) throws IOException {
        final long id = download.dataId();
        final DataStreams.Range plan = download.plan();
        if (plan == null) {
            return DataStreams.decrypt(reading.secret(), reading.context(),
                    EncryptedDataStore.open(connection, id, 0, download.storedLength(), batchSize), download.storedLength());
        }

        final InputStream stored = EncryptedDataStore.open(connection, id, plan.storedOffset(), plan.storedLength(), batchSize);
        return DataStreams.decrypt(reading.secret(), reading.context(), download.header(), download.storedLength(), plan, stored);
    }

    /** The Circle Secret the content is on now, by its key row; -1 if the content is gone. */
    private static long storedKey(final Connection connection, final long dataId) throws SQLException {
        try (java.sql.PreparedStatement statement = connection.prepareStatement("SELECT key_id FROM eds_data WHERE id = ?")) {
            statement.setLong(1, dataId);
            try (java.sql.ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : -1;
            }
        }
    }

    /**
     * The response body, where a failed write or flush means that the client
     * went away, which is told apart from a failure of the content.
     */
    private static final class ClientOutput extends FilterOutputStream {

        private ClientOutput(final OutputStream output) {
            super(output);
        }

        @Override
        public void write(final byte[] bytes, final int offset, final int length) throws IOException {
            try {
                out.write(bytes, offset, length);
            } catch (IOException e) {
                throw new ClientAbortException(e);
            }
        }

        @Override
        public void write(final int value) throws IOException {
            try {
                out.write(value);
            } catch (IOException e) {
                throw new ClientAbortException(e);
            }
        }

        // Small content is only sent with the flush, so a client which
        // went away is first seen here.
        @Override
        public void flush() throws IOException {
            try {
                out.flush();
            } catch (IOException e) {
                throw new ClientAbortException(e);
            }
        }
    }
}
