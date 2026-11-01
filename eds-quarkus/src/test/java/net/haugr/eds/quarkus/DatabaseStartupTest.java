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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import net.haugr.eds.core.StartupBean;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.SchemaManager.Outcome;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.quarkus.DatabaseSelection.Database;
import net.haugr.eds.quarkus.DatabaseStartup.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Tests for the {@link DatabaseStartup} retry logic, with a stand-in for the
 * EDS StartupBean, which returns the given startup outcomes.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ResourceLock("eds-settings")
final class DatabaseStartupTest {

    private final Settings settings = Settings.getInstance();

    @AfterEach
    void restoreReadyFlag() {
        settings.set(StandardSetting.IS_READY, "true");
    }

    @Test
    void testReachableDatabaseStartsAtOnce() {
        final FakeStartupBean startupBean = new FakeStartupBean(settings, Outcome.READY);
        final DatabaseStartup startup = create(startupBean);

        startup.attempt();

        assertEquals(State.STARTED, startup.getState());
        assertEquals(1, startup.getAttempts());
        assertEquals(1, startupBean.calls);
        assertTrue(settings.isReady());
    }

    @Test
    void testUnreachableDatabaseIsRetriedUntilReachable() {
        final FakeStartupBean startupBean = new FakeStartupBean(settings, Outcome.UNREACHABLE, Outcome.UNREACHABLE, Outcome.READY);
        final DatabaseStartup startup = create(startupBean);

        startup.attempt();
        assertEquals(State.WAITING, startup.getState());
        assertFalse(settings.isReady(), "EDS must not report ready while waiting");

        startup.retry();
        assertEquals(State.WAITING, startup.getState());
        assertFalse(settings.isReady());

        startup.retry();
        assertEquals(State.STARTED, startup.getState());
        assertEquals(3, startup.getAttempts());
        assertEquals(3, startupBean.calls);
        assertTrue(settings.isReady());

        // Once started, retries do nothing
        startup.retry();
        startup.attempt();
        assertEquals(3, startup.getAttempts());
        assertEquals(3, startupBean.calls);
    }

    @Test
    void testFailedStartupIsNotRetried() {
        // E.g. a schema which cannot be created, or a different schema
        // version; waiting does not solve that.
        final FakeStartupBean startupBean = new FakeStartupBean(settings, Outcome.FAILED, Outcome.READY);
        final DatabaseStartup startup = create(startupBean);

        startup.attempt();
        startup.retry();

        assertEquals(State.FAILED, startup.getState());
        assertEquals(1, startup.getAttempts());
        assertEquals(1, startupBean.calls);
        assertFalse(settings.isReady());
    }

    @Test
    void testRetryBeforeFirstAttemptDoesNothing() {
        final FakeStartupBean startupBean = new FakeStartupBean(settings, Outcome.READY);
        final DatabaseStartup startup = create(startupBean);

        startup.retry();

        assertEquals(State.PENDING, startup.getState());
        assertEquals(0, startup.getAttempts());
        assertEquals(0, startupBean.calls);
    }

    private DatabaseStartup create(final StartupBean startupBean) {
        return new DatabaseStartup(startupBean, settings, Database.POSTGRESQL);
    }

    /**
     * Returns the given outcomes in order, repeating the last one, and sets
     * the ready flag like the real StartupBean does.
     */
    private static final class FakeStartupBean extends StartupBean {

        private final Settings settings;
        private final Deque<Outcome> outcomes;
        private int calls = 0;

        private FakeStartupBean(final Settings settings, final Outcome... outcomes) {
            super(null, null, settings);
            this.settings = settings;
            this.outcomes = new ArrayDeque<>(Arrays.asList(outcomes));
        }

        @Override
        public Outcome startup() {
            calls++;
            final Outcome outcome = (outcomes.size() > 1) ? outcomes.poll() : outcomes.peek();
            settings.set(StandardSetting.IS_READY, String.valueOf(outcome == Outcome.READY));
            return outcome;
        }

        @Override
        public String getLastConnectionError() {
            return "Connection refused";
        }
    }
}
