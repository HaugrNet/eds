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

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.haugr.eds.core.StatusBean;

/**
 * <p>Drives the gathering for the status of the stone (STATUS.md) once a
 * second. What is gathered, and when, is decided by the
 * {@link StatusBean}: most seconds there is nothing to do. The scheduling
 * itself is the only part which is specific to Quarkus; another runtime
 * would call {@link StatusBean#tick()} with its own means.</p>
 *
 * <p>The inventory isn't gathered before the {@link DatabaseStartup} has
 * prepared the database: on a new database, it would otherwise query tables
 * which the startup is still creating, and Hibernate would log the failed
 * query as a WARN. The {@link StatusBean} logs the wait once, as INFO; the
 * load is sampled meanwhile.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class StatusSchedule {

    private final StatusBean statusBean;
    private final DatabaseStartup databaseStartup;

    /**
     * CDI Constructor.
     *
     * @param statusBean      The EDS Status Bean
     * @param databaseStartup The startup of the database, which gathering waits for
     */
    @Inject
    public StatusSchedule(final StatusBean statusBean, final DatabaseStartup databaseStartup) {
        this.statusBean = statusBean;
        this.databaseStartup = databaseStartup;
    }

    /**
     * Lets the stone gather what is due; the inventory once the database is
     * ready. A run which takes longer than a second, as the inventory may on a large
     * stone, isn't overtaken by the next.
     */
    @Scheduled(identity = "eds-status", every = "1s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void gather() {
        statusBean.tick(databaseStartup.getState() == DatabaseStartup.State.STARTED);
    }
}
