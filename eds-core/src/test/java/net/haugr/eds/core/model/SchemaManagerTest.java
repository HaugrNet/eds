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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.function.Supplier;
import javax.sql.DataSource;
import net.haugr.eds.core.model.SchemaManager.Outcome;
import net.haugr.eds.core.setup.fakes.FakeUnreachableDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the {@link SchemaManager}, with a real (in-memory) H2 database.
 * The PostgreSQL specific serialization of concurrent instances (the
 * advisory lock) requires a PostgreSQL server, and is not covered here.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class SchemaManagerTest {

    @Test
    void testCreatesSchemaInEmptyDatabase() throws SQLException {
        final DataSource dataSource = newDatabase();
        final SchemaManager manager = new SchemaManager(dataSource);

        assertEquals(Outcome.READY, manager.prepare());
        assertNull(manager.getLastConnectionError());
        assertEquals(17, count(dataSource, "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'PUBLIC'"));
        assertEquals(1, count(dataSource, "SELECT count(*) FROM eds_versions"));
        assertEquals(4, count(dataSource, "SELECT count(*) FROM eds_datatypes"));
    }

    @Test
    void testExistingSchemaIsLeftUnchanged() throws SQLException {
        final DataSource dataSource = newDatabase();
        assertEquals(Outcome.READY, new SchemaManager(dataSource).prepare());
        execute(dataSource, "INSERT INTO eds_settings (name, setting) VALUES ('test.marker', 'kept')");

        // A second instance, or a restart, finds the schema and keeps the data
        assertEquals(Outcome.READY, new SchemaManager(dataSource).prepare());
        assertEquals(1, count(dataSource, "SELECT count(*) FROM eds_versions"));
        assertEquals(1, count(dataSource, "SELECT count(*) FROM eds_settings WHERE name = 'test.marker'"));
    }

    @Test
    void testUnreachableDatabase() {
        final SchemaManager manager = new SchemaManager(new FakeUnreachableDataSource());

        assertEquals(Outcome.UNREACHABLE, manager.prepare());
        assertEquals(FakeUnreachableDataSource.MESSAGE, manager.getLastConnectionError());
    }

    @Test
    void testFailedCreationIsNotMistakenForAnotherInstance() throws SQLException {
        // A view named like the version table makes the creation fail, while
        // the version table itself still does not exist afterwards.
        final DataSource dataSource = newDatabase();
        execute(dataSource, "CREATE VIEW eds_versions AS SELECT 1 AS id");

        assertEquals(Outcome.FAILED, new SchemaManager(dataSource).prepare());
    }

    /**
     * A file based H2 keeps its data, so it is a production system, with the
     * larger ceiling; an in-memory one is a test system.
     */
    @Test
    void testPersistentDatabaseHasTheProductionCeiling(@TempDir final Path directory) throws SQLException, ReflectiveOperationException {
        final JdbcDataSource persistent = new JdbcDataSource();
        persistent.setURL("jdbc:h2:file:" + directory.resolve("eds").toAbsolutePath());
        persistent.setUser("sa");
        final SchemaManager manager = new SchemaManager(persistent);
        final Settings settings = newSettings();

        assertEquals(Outcome.READY, manager.prepare());
        assertEquals(Boolean.TRUE, manager.isPersistent());
        assertEquals("H2", manager.getDatabaseProduct());
        try (Connection connection = persistent.getConnection()) {
            assertEquals(SchemaManager.PRODUCTION_SYSTEM_BYTES, SchemaManager.ceiling(settings, connection));
        }
        try (Connection connection = newDatabase().getConnection()) {
            assertEquals(SchemaManager.TEST_SYSTEM_BYTES, SchemaManager.ceiling(settings, connection));
        }
    }

    /** A database which doesn't tell its URL is taken to be persistent. */
    @Test
    void testDatabaseWithoutUrlIsPersistent() throws SQLException {
        final DatabaseMetaData metaData = (DatabaseMetaData) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{DatabaseMetaData.class},
                (proxy, method, arguments) -> null);
        try (Connection connection = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> "getMetaData".equals(method.getName()) ? metaData : null)) {
            assertTrue(SchemaManager.persistentDatabase(connection));
        }
    }

    /** Without a context class loader, the schema is read with that of EDS. */
    @Test
    void testCreatesSchemaWithoutContextClassLoader() throws SQLException {
        final DataSource dataSource = newDatabase();

        assertEquals(Outcome.READY, withContextClassLoader(null, () -> new SchemaManager(dataSource).prepare()));
        assertEquals(1, count(dataSource, "SELECT count(*) FROM eds_versions"));
    }

    /** A missing schema script fails the preparation, and creates nothing. */
    @Test
    void testMissingSchemaScript() throws Exception {
        final DataSource dataSource = newDatabase();

        try (URLClassLoader empty = new URLClassLoader(new URL[0], null)) {
            assertEquals(Outcome.FAILED, withContextClassLoader(empty, () -> new SchemaManager(dataSource).prepare()));
        }
        assertEquals(0, count(dataSource, "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'PUBLIC'"));
    }

    private static <T> T withContextClassLoader(final ClassLoader loader, final Supplier<T> work) {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            return work.get();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    /** Settings of the test's own, with the defaults, as the shared ones may be changed by other tests. */
    // Reflection reaches what the test checks; it is not the contract.
    @SuppressWarnings("fb-contrib:RFI_SET_ACCESSIBLE")
    private static Settings newSettings() throws ReflectiveOperationException {
        final Constructor<Settings> constructor = Settings.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    // The in-memory test database has H2's default account.
    @SuppressWarnings("findsecbugs:HARD_CODE_PASSWORD")
    private static DataSource newDatabase() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    // Runs the constant SQL of the tests in this class.
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JDBC")
    private static void execute(final DataSource dataSource, final String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    // Runs the constant SQL of the tests in this class.
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JDBC")
    private static long count(final DataSource dataSource, final String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) {
                return fail("No row from: " + sql);
            }
            return result.getLong(1);
        }
    }
}
