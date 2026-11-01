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
package net.haugr.eds.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Map;
import java.util.Optional;
import net.haugr.eds.quarkus.DatabaseSelection.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/**
 * Tests for the {@link DatabaseSelection}. The tests use the system property,
 * as environment variables cannot be changed from within the JVM; they
 * assume that the environment variable is not set.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
final class DatabaseSelectionTest {

    private static final String URL = "jdbc:postgresql://dbserver:5432/eds";

    @BeforeEach
    void requireCleanEnvironment() {
        assumeTrue(System.getenv(DatabaseSelection.URL_VARIABLE) == null,
                "The environment variable " + DatabaseSelection.URL_VARIABLE + " is set");
        System.clearProperty(DatabaseSelection.URL_PROPERTY);
    }

    @AfterEach
    void clearProperty() {
        System.clearProperty(DatabaseSelection.URL_PROPERTY);
    }

    @Test
    void testNoUrlSelectsH2() {
        assertEquals(Optional.empty(), DatabaseSelection.configuredUrl());
        assertEquals(Database.H2, DatabaseSelection.selected());

        final Map<String, String> properties = new DatabaseSelection().getProperties();
        assertEquals("true", properties.get("quarkus.datasource.h2.active"));
        assertEquals("true", properties.get("quarkus.hibernate-orm.h2.active"));
        assertEquals("false", properties.get("quarkus.datasource.pg.active"));
        assertEquals("false", properties.get("quarkus.hibernate-orm.pg.active"));
        assertNull(properties.get("quarkus.datasource.pg.jdbc.url"));
    }

    @Test
    void testBlankUrlSelectsH2() {
        System.setProperty(DatabaseSelection.URL_PROPERTY, "   ");

        assertEquals(Database.H2, DatabaseSelection.selected());
    }

    @Test
    void testUrlSelectsPostgreSQL() {
        System.setProperty(DatabaseSelection.URL_PROPERTY, "  " + URL + "  ");

        assertEquals(Optional.of(URL), DatabaseSelection.configuredUrl());
        assertEquals(Database.POSTGRESQL, DatabaseSelection.selected());

        final Map<String, String> properties = new DatabaseSelection().getProperties();
        assertEquals("true", properties.get("quarkus.datasource.pg.active"));
        assertEquals("true", properties.get("quarkus.hibernate-orm.pg.active"));
        assertEquals("false", properties.get("quarkus.datasource.h2.active"));
        assertEquals("false", properties.get("quarkus.hibernate-orm.h2.active"));
        assertEquals(URL, properties.get("quarkus.datasource.pg.jdbc.url"));
    }

    @Test
    void testConfigSourceContract() {
        System.setProperty(DatabaseSelection.URL_PROPERTY, URL);
        final DatabaseSelection source = new DatabaseSelection();

        assertEquals(source.getProperties().keySet(), source.getPropertyNames());
        assertEquals(URL, source.getValue("quarkus.datasource.pg.jdbc.url"));
        assertNull(source.getValue("quarkus.http.port"));
        assertFalse(source.getName().isBlank());

        // Above application.properties (250), below environment variables (300)
        assertTrue(source.getOrdinal() > 250 && source.getOrdinal() < 300);
    }

    @Test
    void testUnitNames() {
        assertEquals("pg", Database.POSTGRESQL.getUnit());
        assertEquals("h2", Database.H2.getUnit());
    }
}
