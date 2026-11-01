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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import net.haugr.eds.core.model.SchemaManager.Outcome;
import net.haugr.eds.core.setup.fakes.FakeUnreachableDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

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
        assertEquals(10, count(dataSource, "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'PUBLIC'"));
        assertEquals(1, count(dataSource, "SELECT count(*) FROM eds_versions"));
        assertEquals(2, count(dataSource, "SELECT count(*) FROM eds_datatypes"));
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

    private static DataSource newDatabase() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static void execute(final DataSource dataSource, final String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static long count(final DataSource dataSource, final String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }
}
