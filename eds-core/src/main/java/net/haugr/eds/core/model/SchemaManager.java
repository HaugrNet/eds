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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Prepares the EDS database, before the StartupBean verifies it. If the
 * EDS schema does not exist yet, it is created from the shared schema script
 * (META-INF/eds-schema.sql), in a single transaction. An existing schema is
 * left untouched, as EDS 2.0 does not upgrade databases in place.</p>
 *
 * <p>The database, user and password are a prerequisite, as creating them
 * requires administrative rights, which EDS should not have.</p>
 *
 * <p>Several EDS instances may start at the same time against an empty
 * database. For PostgreSQL, the schema creation is therefore serialized with
 * a transaction scoped advisory lock, and the schema is checked again once the
 * lock is held, so only the first instance creates it. As a safety net for
 * all databases, a failed creation is treated as success, if the schema
 * exists afterwards, i.e. it was created by another instance.</p>
 *
 * <p>The connection is taken directly from the datasource, outside of any
 * container managed transaction, as the schema is committed explicitly.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public class SchemaManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaManager.class);
    private static final String SCHEMA_SCRIPT = "META-INF/eds-schema.sql";
    private static final String VERSION_TABLE = "eds_versions";
    private static final String POSTGRESQL = "PostgreSQL";
    // Arbitrary, but fixed, key for the PostgreSQL advisory lock, which
    // serializes the schema creation between EDS instances ("EDSSCHMA").
    private static final long SCHEMA_LOCK_KEY = 0x4544535343484D41L;

    /**
     * The outcome of preparing the database.
     */
    public enum Outcome {

        /** The database is reachable, and the schema exists. */
        READY,

        /** No connection could be obtained, the attempt can be retried. */
        UNREACHABLE,

        /** Connected, but the schema could not be checked or created. */
        FAILED
    }

    private final DataSource dataSource;
    private volatile String lastConnectionError = null;

    /**
     * Creates a new Schema Manager for the given datasource.
     *
     * @param dataSource The datasource of the EDS database
     */
    public SchemaManager(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Prepares the database. Failing to obtain a connection is reported as
     * {@link Outcome#UNREACHABLE}, without logging, so the caller decides how
     * to report and retry it. All other problems are logged, and reported as
     * {@link Outcome#FAILED}.
     *
     * @return The outcome
     */
    public Outcome prepare() {
        final Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            lastConnectionError = e.getMessage();
            return Outcome.UNREACHABLE;
        }

        try (connection) {
            if (tableExists(connection.getMetaData(), VERSION_TABLE)) {
                LOGGER.info("The EDS schema exists.");
            } else {
                createSchema(connection);
            }
            return Outcome.READY;
        } catch (SQLException | IOException e) {
            LOGGER.error("Cannot prepare the EDS database: {}", e.getMessage(), e);
            return Outcome.FAILED;
        }
    }

    /**
     * Returns the message of the last failed connection attempt.
     *
     * @return Message of the last connection failure, or null
     */
    public String getLastConnectionError() {
        return lastConnectionError;
    }

    private static void createSchema(final Connection connection) throws SQLException, IOException {
        final List<String> statements = SqlScript.split(readSchema());
        final boolean postgresql = POSTGRESQL.equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());

        final boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            if (postgresql) {
                // Blocks while another instance holds the lock, i.e. while it
                // creates the schema. The lock is released by the commit or
                // rollback, or if the connection is lost.
                statement.execute("SELECT pg_advisory_xact_lock(" + SCHEMA_LOCK_KEY + ')');
                if (tableExists(connection.getMetaData(), VERSION_TABLE)) {
                    connection.commit();
                    LOGGER.info("The EDS schema was created by another EDS instance.");
                    return;
                }
            }

            LOGGER.info("Creating the EDS schema ({} statements).", statements.size());
            for (final String sql : statements) {
                statement.execute(sql);
            }
            connection.commit();
            LOGGER.info("The EDS schema was created.");
        } catch (SQLException e) {
            connection.rollback();
            // Safety net: if another instance created the schema at the same
            // time, the creation fails here, but the schema is fine.
            if (tableExists(connection.getMetaData(), VERSION_TABLE)) {
                LOGGER.info("The EDS schema was created by another EDS instance ({}).", e.getMessage());
                return;
            }
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    /**
     * Databases differ in how they store unquoted identifiers (PostgreSQL in
     * lower case, H2 in upper case), so the metadata is asked for both.
     */
    private static boolean tableExists(final DatabaseMetaData metaData, final String table) throws SQLException {
        for (final String name : List.of(table.toLowerCase(Locale.ROOT), table.toUpperCase(Locale.ROOT))) {
            try (ResultSet tables = metaData.getTables(null, null, name, new String[] { "TABLE" })) {
                if (tables.next()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String readSchema() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = SchemaManager.class.getClassLoader();
        }

        try (InputStream stream = loader.getResourceAsStream(SCHEMA_SCRIPT)) {
            if (stream == null) {
                throw new IOException("The EDS schema script " + SCHEMA_SCRIPT + " is missing.");
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
