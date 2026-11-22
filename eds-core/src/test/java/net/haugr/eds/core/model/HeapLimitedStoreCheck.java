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

import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.Arrays;
import javax.sql.DataSource;

/**
 * <p>Run as a separate JVM with a small heap by
 * {@link EncryptedDataStoreTest}: writes a large blob as a stream and reads
 * it back in batches, comparing digests. Prints "OK" on success.</p>
 *
 * <p>Arguments: database URL (H2 or PostgreSQL) and size in bytes, optionally
 * "nolength" to write without a length (for the pgjdbc verification).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class HeapLimitedStoreCheck {

    private HeapLimitedStoreCheck() {
        // Only the main method.
    }

    public static void main(final String[] args) throws Exception {
        final String url = args[0];
        final long size = Long.parseLong(args[1]);
        final boolean withoutLength = (args.length > 2) && "nolength".equals(args[2]);
        final boolean copy = (args.length > 2) && "copy".equals(args[2]);
        final boolean format = copy || ((args.length > 2) && "format".equals(args[2]));
        final DataSource dataSource = url.startsWith("jdbc:h2:") ? h2(url) : StoreTestDatabases.postgresql(url);
        StoreTestDatabases.freshSchema(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            final long id = StoreTestDatabases.createDataRow(connection);
            final MessageDigest written = MessageDigest.getInstance("SHA-512");
            final long start = System.nanoTime();
            final net.haugr.eds.core.jce.SecretEDSKey secret = net.haugr.eds.core.jce.Crypto.generateSymmetricKey(net.haugr.eds.core.enums.KeyAlgorithm.AES_GCM_256);
            final net.haugr.eds.core.jce.DataFormat.Context context = new net.haugr.eds.core.jce.DataFormat.Context("c", "m", 1);
            long storedLength = size;
            try (InputStream generated = new StoreTestDatabases.GeneratedStream(3, size, written)) {
                if (format) {
                    try (net.haugr.eds.core.jce.DataStreams.EncryptingStream encrypting = net.haugr.eds.core.jce.DataStreams.encrypt(secret, context, generated, size)) {
                        storedLength = encrypting.storedLength();
                        EncryptedDataStore.write(connection, id, encrypting, storedLength, "SHA-512");
                    }
                } else if (withoutLength) {
                    EncryptedDataStore.writeToTheEnd(connection, id, generated, "SHA-512");
                } else {
                    EncryptedDataStore.write(connection, id, generated, size, "SHA-512");
                }
            }
            connection.commit();
            final long writtenMs = (System.nanoTime() - start) / 1_000_000;

            long readId = id;
            net.haugr.eds.core.jce.SecretEDSKey readSecret = secret;
            net.haugr.eds.core.jce.DataFormat.Context readContext = context;
            if (copy) {
                // Re-encrypt the object into a second row, for another
                // Circle (S4); the source is read on a second Connection.
                final long target = StoreTestDatabases.createDataRow(connection);
                readSecret = net.haugr.eds.core.jce.Crypto.generateSymmetricKey(net.haugr.eds.core.enums.KeyAlgorithm.AES_GCM_256);
                readContext = new net.haugr.eds.core.jce.DataFormat.Context("c2", "m2", 2);
                net.haugr.eds.core.stream.ContentCopier.copy(connection, dataSource::getConnection, new net.haugr.eds.core.stream.ContentCopier.Source(id, storedLength, secret, context),
                        new net.haugr.eds.core.stream.ContentCopier.Target(target, readSecret, readContext), 4 * 1024 * 1024, "SHA-512", 10L * 1024 * 1024);
                connection.commit();
                readId = target;
                storedLength = EncryptedDataStore.length(connection, target);
            }
            final MessageDigest read = MessageDigest.getInstance("SHA-512");
            final byte[] buffer = new byte[64 * 1024];
            final net.haugr.eds.core.jce.SecretEDSKey decryptSecret = readSecret;
            final net.haugr.eds.core.jce.DataFormat.Context decryptContext = readContext;
            try (InputStream source = EncryptedDataStore.open(connection, readId, 0, storedLength, 4 * 1024 * 1024);
                 InputStream stream = format ? net.haugr.eds.core.jce.DataStreams.decrypt(decryptSecret, decryptContext, source, storedLength) : source) {
                int count = stream.read(buffer);
                while (count != -1) {
                    read.update(buffer, 0, count);
                    count = stream.read(buffer);
                }
            }
            if (format && !copy) {
                // A 1 MB range in the middle, the way a seek in a player reads.
                final net.haugr.eds.core.jce.DataFormat.Header header = net.haugr.eds.core.jce.DataFormat.Header.read(
                        EncryptedDataStore.read(connection, id, 0, net.haugr.eds.core.jce.DataFormat.HEADER_BYTES));
                final long from = size / 2;
                final net.haugr.eds.core.jce.DataStreams.Range range = net.haugr.eds.core.jce.DataStreams.plan(header, storedLength, from, from + 1024 * 1024 - 1);
                final byte[] part;
                try (InputStream stored = EncryptedDataStore.open(connection, id, range.storedOffset(), range.storedLength(), 4 * 1024 * 1024);
                     InputStream plain = net.haugr.eds.core.jce.DataStreams.decrypt(secret, context, header, storedLength, range, stored)) {
                    part = plain.readAllBytes();
                }
                final byte[] expected = new byte[part.length];
                try (InputStream generated = new StoreTestDatabases.GeneratedStream(3, size, MessageDigest.getInstance("SHA-512"))) {
                    generated.skipNBytes(from);
                    generated.readNBytes(expected, 0, expected.length);
                }
                if (!Arrays.equals(expected, part)) {
                    System.out.println("RANGE MISMATCH");
                    System.exit(1);
                }
            }
            connection.commit();
            final long totalMs = (System.nanoTime() - start) / 1_000_000;

            final boolean equal = Arrays.equals(written.digest(), read.digest());
            System.out.println((equal ? "OK" : "MISMATCH") + (copy ? " copy" : format ? " format" : "") + " size=" + size + " write=" + writtenMs + "ms total=" + totalMs
                    + "ms maxHeap=" + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "MB");
            System.exit(equal ? 0 : 1);
        }
    }

    private static DataSource h2(final String url) {
        final org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
        dataSource.setURL(url);
        dataSource.setUser("sa");
        return dataSource;
    }
}
