/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2025, haugr.net
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ejb.Timer;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.setup.fakes.FakeEntityManager;
import net.haugr.eds.core.setup.fakes.FakeTimer;
import net.haugr.eds.core.setup.fakes.FakeTimerService;
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
        final var timerService = new FakeTimerService();
        final var sanitizerBean = prepareSanitizerBean(settings);
        final var bean = new StartupBean(entityManager, sanitizerBean, timerService, settings);

        bean.startup();

        assertFalse(Boolean.parseBoolean(settings.get(StandardSetting.IS_READY.getKey())));
    }

    @Test
    void testStartupWithValidDB() {
        final var timerService = new FakeTimerService();
        final var sanitizerBean = prepareSanitizerBean();

        final StartupBean bean = new StartupBean(entityManager, sanitizerBean, timerService, settings);

        bean.startup();

        assertTrue(Boolean.parseBoolean(settings.get(StandardSetting.IS_READY.getKey())));
    }

    @Test
    void testStartupWithSanityCheck() {
        final var sanitizerBean = prepareSanitizerBean();
        final var timerService = new FakeTimerService();
        final var bean = new StartupBean(entityManager, sanitizerBean, timerService, settings);
        settings.set(StandardSetting.SANITY_STARTUP.getKey(), "true");

        bean.startup();

        assertTrue(Boolean.parseBoolean(settings.get(StandardSetting.IS_READY.getKey())));
    }

    @Test
    void testRunSanitizing() {
        final var sanitizerBean = prepareSanitizerBean();
        final var bean = new StartupBean(entityManager, sanitizerBean, null, settings);

        assertDoesNotThrow(bean::runSanitizing);
    }

    @Test
    void testRunSanitizingWithTimer() {
        final var sanitizerBean = prepareSanitizerBean();
        final var bean = new StartupBean(entityManager, sanitizerBean, null, settings);
        final Timer timer = new FakeTimer();

        assertDoesNotThrow(() -> bean.runSanitizingWithTimeout(timer));
    }
}
