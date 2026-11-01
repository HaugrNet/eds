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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.SchemaManager;
import net.haugr.eds.core.model.SchemaManager.Outcome;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.setup.fakes.FakeEntityManager;
import net.haugr.eds.core.setup.fakes.FakeUnreachableDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class StartupBeanTest extends DatabaseSetup {

    @Test
    void testEmptyConstructor() {
        final var bean = new StartupBean();

        assertNotNull(bean);
    }

    @Test
    void testStartupWithEmptyDB() {
        final var entityManager = new FakeEntityManager();
        final var sanitizerBean = prepareSanitizerBean(settings);
        final var bean = new StartupBean(entityManager, sanitizerBean, settings);

        assertEquals(Outcome.FAILED, bean.startup());

        assertFalse(Boolean.parseBoolean(settings.get(StandardSetting.IS_READY.getKey())));
    }

    @Test
    void testStartupWithValidDB() {
        final var sanitizerBean = prepareSanitizerBean();

        final StartupBean bean = new StartupBean(entityManager, sanitizerBean, settings);

        assertEquals(Outcome.READY, bean.startup());

        assertTrue(Boolean.parseBoolean(settings.get(StandardSetting.IS_READY.getKey())));
        assertNull(bean.getLastConnectionError());
    }

    @Test
    void testStartupWithUnreachableDatabase() {
        // The startup must stop before verifying the database, so the
        // EntityManager is never used.
        final var schemaManager = new SchemaManager(new FakeUnreachableDataSource());
        final var bean = new StartupBean(null, prepareSanitizerBean(), schemaManager, settings);

        assertEquals(Outcome.UNREACHABLE, bean.startup());

        assertFalse(Boolean.parseBoolean(settings.get(StandardSetting.IS_READY.getKey())));
        assertEquals(FakeUnreachableDataSource.MESSAGE, bean.getLastConnectionError());
    }

    @Test
    void testStartupWithSchemaManagerAndExistingSchema() {
        // The same in-memory database as the EntityManager of the tests, where
        // the schema exists, so the full startup is run.
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        final var bean = new StartupBean(entityManager, prepareSanitizerBean(), new SchemaManager(dataSource), settings);

        assertEquals(Outcome.READY, bean.startup());

        assertTrue(Boolean.parseBoolean(settings.get(StandardSetting.IS_READY.getKey())));
    }

    @Test
    void testStartupWithSanityCheck() {
        final var sanitizerBean = prepareSanitizerBean();
        final var bean = new StartupBean(entityManager, sanitizerBean, settings);
        settings.set(StandardSetting.SANITY_STARTUP.getKey(), "true");

        bean.startup();

        assertTrue(Boolean.parseBoolean(settings.get(StandardSetting.IS_READY.getKey())));
    }

    @Test
    void testRunSanitizing() {
        final var sanitizerBean = prepareSanitizerBean();
        final var bean = new StartupBean(entityManager, sanitizerBean, settings);

        assertDoesNotThrow(bean::runSanitizing);
    }
}
