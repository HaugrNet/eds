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

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import net.haugr.eds.core.StartupBean;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.SchemaManager.Outcome;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.quarkus.DatabaseSelection.Database;
import org.jboss.logging.Logger;

/**
 * <p>Starts EDS, by running the EDS StartupBean, which prepares the database
 * (creating the schema if it is missing), verifies it, and starts the
 * services.</p>
 *
 * <p>If the database cannot be reached, for example because PostgreSQL is
 * still starting, EDS stays running but not ready, and the startup is
 * repeated every {@code eds.db.retry-interval} (default 10 seconds), until
 * the database is reachable. The services are only started once, as the
 * StartupBean stops before them while the database is unreachable. The first
 * failure is logged as a warning, the following ones only at debug level, so
 * a long outage does not flood the log.</p>
 *
 * <p>Problems which waiting cannot solve, such as a schema which cannot be
 * created, or a schema version which does not match, are not retried; they
 * are logged, and EDS stays not ready.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class DatabaseStartup {

    private static final Logger LOGGER = Logger.getLogger(DatabaseStartup.class);

    /**
     * The state of the EDS startup.
     */
    public enum State {

        /** Not attempted yet. */
        PENDING,

        /** Waiting for the database to become reachable. */
        WAITING,

        /** The database was prepared and verified, EDS is ready. */
        STARTED,

        /** The database was reachable, but could not be prepared or verified. */
        FAILED
    }

    private final StartupBean startupBean;
    private final Settings settings;
    private final Database database;
    private State state = State.PENDING;
    private int attempts = 0;

    /**
     * CDI Constructor.
     *
     * @param startupBean The EDS StartupBean
     */
    @Inject
    public DatabaseStartup(final StartupBean startupBean) {
        this(startupBean, Settings.getInstance(), DatabaseSelection.selected());
    }

    /**
     * Constructor with all dependencies, for testing.
     *
     * @param startupBean The EDS StartupBean
     * @param settings    The EDS Settings
     * @param database    The selected database
     */
    DatabaseStartup(final StartupBean startupBean, final Settings settings, final Database database) {
        this.startupBean = startupBean;
        this.settings = settings;
        this.database = database;
    }

    /**
     * Makes the first attempt, when Quarkus has started.
     *
     * @param event Quarkus Startup Event
     */
    void onStart(@Observes final StartupEvent event) {
        if (database == Database.POSTGRESQL) {
            LOGGER.infof("Using the PostgreSQL database at %s", withoutParameters(DatabaseSelection.configuredUrl().orElse("")));
        } else {
            LOGGER.warnf("No database configured (%s), using the in-memory H2 database. All data is lost when EDS stops!", DatabaseSelection.URL_VARIABLE);
        }

        attempt();
    }

    /**
     * Retries while waiting for the database. Overlapping executions are
     * skipped, as an attempt may take longer than the interval, if the
     * connection attempt has to time out.
     */
    @Scheduled(identity = "eds-database-retry",
               every = "${eds.db.retry-interval:10s}",
               delayed = "${eds.db.retry-interval:10s}",
               concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void retry() {
        if (getState() == State.WAITING) {
            attempt();
        }
    }

    /**
     * Runs the EDS StartupBean, unless EDS has already started, or failed.
     * Synchronized, as the first attempt and the retries run in different
     * threads.
     */
    synchronized void attempt() {
        if (state == State.STARTED || state == State.FAILED) {
            return;
        }

        attempts++;
        final Outcome outcome = startupBean.startup();
        switch (outcome) {
            case UNREACHABLE -> waitForDatabase();
            case FAILED -> {
                state = State.FAILED;
                settings.set(StandardSetting.IS_READY, "false");
                LOGGER.error("The EDS database could not be prepared or is not valid, EDS is not ready. See the previous errors for details, correct the problem, and restart EDS.");
            }
            case READY -> {
                if (attempts > 1) {
                    LOGGER.infof("The database is reachable, after %d attempts.", attempts);
                }
                state = State.STARTED;
            }
        }
    }

    private void waitForDatabase() {
        // The ready flag defaults to true, it must report not ready while
        // waiting (the StartupBean sets it as well, this is a safeguard).
        settings.set(StandardSetting.IS_READY, "false");
        state = State.WAITING;

        if (attempts == 1) {
            LOGGER.warnf("The database cannot be reached (%s). EDS is not ready, and retries until it is reachable.", startupBean.getLastConnectionError());
        } else {
            LOGGER.debugf("The database is still not reachable, attempt %d: %s", attempts, startupBean.getLastConnectionError());
        }
    }

    /**
     * Returns the current state.
     *
     * @return The state
     */
    public synchronized State getState() {
        return state;
    }

    /**
     * Returns the number of attempts made so far.
     *
     * @return Number of attempts
     */
    public synchronized int getAttempts() {
        return attempts;
    }

    /**
     * JDBC URLs may hold credentials as parameters, so only the part before
     * the parameters is logged.
     */
    private static String withoutParameters(final String url) {
        final int parameters = url.indexOf('?');
        return (parameters < 0) ? url : url.substring(0, parameters);
    }
}
