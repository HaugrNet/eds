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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.haugr.eds.core.StartupBean;
import net.haugr.eds.core.StatusBean;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.SchemaManager.Outcome;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.quarkus.DatabaseSelection.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Tests that the {@link StatusSchedule} lets the {@link StatusBean} gather
 * the inventory only once the database is prepared: on a new database the
 * inventory queried tables which the startup was still creating, and
 * Hibernate logged a WARN.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ResourceLock("eds-settings")
final class StatusScheduleTest {

    private final Settings settings = Settings.getInstance();

    @AfterEach
    void restoreReadyFlag() {
        settings.set(StandardSetting.IS_READY, "true");
    }

    @Test
    void testTheInventoryWaitsBeforeTheStartup() {
        final CountingStatusBean statusBean = new CountingStatusBean();
        final DatabaseStartup startup = new DatabaseStartup(new FixedStartupBean(settings, null), null, settings, Database.POSTGRESQL);
        final StatusSchedule schedule = new StatusSchedule(statusBean, startup);

        schedule.gather();
        schedule.gather();

        assertEquals(List.of(false, false), statusBean.ticks);
    }

    @Test
    void testTheInventoryWaitsWhileTheDatabaseIsPrepared() throws Exception {
        final CountDownLatch preparing = new CountDownLatch(1);
        final CountDownLatch prepared = new CountDownLatch(1);
        final CountingStatusBean statusBean = new CountingStatusBean();
        final DatabaseStartup startup = new DatabaseStartup(new FixedStartupBean(settings, Outcome.READY, preparing, prepared),
                null, settings, Database.POSTGRESQL);
        final StatusSchedule schedule = new StatusSchedule(statusBean, startup);

        final CompletableFuture<Void> attempt = CompletableFuture.runAsync(startup::attempt);
        try {
            assertTrue(preparing.await(10, TimeUnit.SECONDS));
            // The attempt holds the startup; gathering doesn't wait for it, and leaves the inventory.
            assertTimeoutPreemptively(Duration.ofSeconds(5), schedule::gather);
            assertEquals(List.of(false), statusBean.ticks);
        } finally {
            prepared.countDown();
        }
        attempt.get(10, TimeUnit.SECONDS);

        schedule.gather();
        assertEquals(List.of(false, true), statusBean.ticks);
    }

    @Test
    void testTheInventoryWaitsWhileWaitingForTheDatabase() {
        final CountingStatusBean statusBean = new CountingStatusBean();
        final DatabaseStartup startup = new DatabaseStartup(new FixedStartupBean(settings, Outcome.UNREACHABLE), null, settings, Database.POSTGRESQL);
        final StatusSchedule schedule = new StatusSchedule(statusBean, startup);

        startup.attempt();
        schedule.gather();

        assertEquals(List.of(false), statusBean.ticks);
    }

    private static final class CountingStatusBean extends StatusBean {

        private final List<Boolean> ticks = new CopyOnWriteArrayList<>();

        private CountingStatusBean() {
            super(null, Settings.getInstance());
        }

        @Override
        public void tick(final boolean databaseReady) {
            ticks.add(databaseReady);
        }
    }

    /**
     * Returns one outcome; optionally says when it has begun, and waits to
     * be let go, as a startup which creates the schema takes a while.
     */
    private static final class FixedStartupBean extends StartupBean {

        private final Settings settings;
        private final Outcome outcome;
        private final CountDownLatch preparing;
        private final CountDownLatch prepared;

        private FixedStartupBean(final Settings settings, final Outcome outcome) {
            this(settings, outcome, null, null);
        }

        private FixedStartupBean(final Settings settings, final Outcome outcome, final CountDownLatch preparing, final CountDownLatch prepared) {
            super(null, null, settings);
            this.settings = settings;
            this.outcome = outcome;
            this.preparing = preparing;
            this.prepared = prepared;
        }

        @Override
        public Outcome startup() {
            if (preparing != null) {
                preparing.countDown();
                try {
                    if (!prepared.await(10, TimeUnit.SECONDS)) {
                        return Outcome.FAILED;
                    }
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                    return Outcome.FAILED;
                }
            }
            settings.set(StandardSetting.IS_READY, String.valueOf(outcome == Outcome.READY));
            return outcome;
        }

        @Override
        public String getLastConnectionError() {
            return "Connection refused";
        }
    }
}
