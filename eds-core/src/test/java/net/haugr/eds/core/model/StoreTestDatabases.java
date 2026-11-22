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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * <p>Test support for {@link EncryptedDataStore}: the test databases, a
 * fresh schema and a row to write to. PostgreSQL is only used when a test
 * database is given, with the system property {@value #POSTGRESQL_PROPERTY}
 * or the environment variable {@value #POSTGRESQL_ENV}, as a JDBC URL with
 * user and password, e.g.
 * {@code jdbc:postgresql://localhost:5432/eds_test?user=eds&password=secret}.
 * The database is emptied by the tests.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class StoreTestDatabases {

    public static final String POSTGRESQL_PROPERTY = "eds.test.postgresql.url";
    public static final String POSTGRESQL_ENV = "EDS_TEST_POSTGRESQL_URL";

    private StoreTestDatabases() {
        // Utility class.
    }

    public static String postgresqlUrl() {
        final String property = System.getProperty(POSTGRESQL_PROPERTY);
        return ((property != null) && !property.isBlank()) ? property : System.getenv(POSTGRESQL_ENV);
    }

    /**
     * The URL of the PostgreSQL test database, with a schema of its own: the
     * test classes run in parallel JVMs, and {@link #freshSchema} drops the
     * schema.
     *
     * @param schema Name of the schema, for this test
     * @return URL, or null if no PostgreSQL test database is configured
     */
    public static String postgresqlUrl(final String schema) {
        final String url = postgresqlUrl();
        return (url == null) ? null : url + (url.contains("?") ? '&' : '?') + "currentSchema=" + schema;
    }

    public static DataSource h2(final String name) {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        return dataSource;
    }

    public static DataSource postgresql(final String url) {
        final PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        return dataSource;
    }

    /**
     * Drops and creates the EDS schema, with the {@link SchemaManager}.
     *
     * @param dataSource The test database
     * @throws SQLException if the database can't be prepared
     */
    // The schema is the test database's own; DDL can't take it as a parameter.
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JDBC")
    public static void freshSchema(final DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            if ("PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
                final String schema = (dataSource instanceof PGSimpleDataSource pg) && (pg.getCurrentSchema() != null) ? pg.getCurrentSchema() : "public";
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
                statement.execute("CREATE SCHEMA " + schema);
            } else {
                statement.execute("DROP ALL OBJECTS");
            }
        }
        if (new SchemaManager(dataSource).prepare() != SchemaManager.Outcome.READY) {
            throw new SQLException("The EDS schema could not be created.");
        }
    }

    /**
     * Creates a Circle, Key, Data Object and an empty data row.
     *
     * @param connection JDBC Connection
     * @return Internal Id of the data row
     * @throws SQLException if the inserts fail
     */
    public static long createDataRow(final Connection connection) throws SQLException {
        final String unique = UUID.randomUUID().toString();
        try (PreparedStatement circle = connection.prepareStatement("INSERT INTO eds_circles (external_id, name) VALUES (?, ?)");
             PreparedStatement key = connection.prepareStatement("INSERT INTO eds_keys (circle_id, algorithm, status) VALUES ((SELECT max(id) FROM eds_circles), 'AES_GCM_256', 'ACTIVE')");
             PreparedStatement metadata = connection.prepareStatement("INSERT INTO eds_metadata (external_id, parent_id, circle_id, datatype_id, name) " +
                     "VALUES (?, 0, (SELECT max(id) FROM eds_circles), 2, 'stream object')")) {
            circle.setString(1, unique);
            circle.setString(2, "circle " + unique);
            circle.executeUpdate();
            key.executeUpdate();
            metadata.setString(1, unique);
            metadata.executeUpdate();
        }
        // Without content: a data row has no parts until something is written.
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO eds_data (metadata_id, key_id, checksum) " +
                "VALUES ((SELECT max(id) FROM eds_metadata), (SELECT max(id) FROM eds_keys), 'empty')")) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT max(id) FROM eds_data");
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("The data row which was just added isn't there.");
            }
            return result.getLong(1);
        }
    }

    /**
     * A generated stream of pseudo-random bytes, which updates the digest
     * with what it delivers. Nothing is held in memory. Each byte depends
     * only on the seed and its position, not on how the stream is read.
     */
    // The digest is the caller's, updated by the one thread reading the stream.
    @SuppressWarnings("java:S9428")
    public static final class GeneratedStream extends InputStream {

        private final long seed;
        private final long size;
        private final MessageDigest digest;
        private long delivered = 0;

        public GeneratedStream(final long seed, final long size, final MessageDigest digest) {
            this.seed = seed;
            this.size = size;
            this.digest = digest;
        }

        @Override
        public int read() {
            final byte[] one = new byte[1];
            return (read(one, 0, 1) == 1) ? (one[0] & 0xFF) : -1;
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) {
            if (delivered >= size) {
                return -1;
            }
            final int count = (int) Math.min(length, size - delivered);
            for (int i = 0; i < count; i++) {
                final long position = delivered + i;
                long mixed = (position + seed) * 0x9E3779B97F4A7C15L;
                mixed ^= mixed >>> 29;
                bytes[offset + i] = (byte) (mixed >>> 32);
            }
            digest.update(bytes, offset, count);
            delivered += count;

            return count;
        }
    }
}
