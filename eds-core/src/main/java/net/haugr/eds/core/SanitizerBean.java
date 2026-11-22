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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.managers.SanityManager;
import net.haugr.eds.core.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Generally, the database is always trustworthy, it is a system designed
 * to be entrusted with data - meaning that the bits and bytes going in
 * should match those coming out. However, if certain parts of the data are
 * not accessed or used in a long time, the disc used to persist the data on
 * may develop problems which can corrupt data over time.</p>
 *
 * <p>The simplest solution is to make sure that data is used frequently and
 * as long as the data extracted matches the one that was persisted -
 * everything is fine. If a problem occurs over time, then a flag is set which
 * will mark the data invalid. This way, the corrupted record can be either
 * removed or replaced with a valid record from a backup.</p>
 *
 * <p>The Sanity checks are run hourly by the runtime (see the
 * SanitizerSchedule in eds-quarkus), and optionally at startup.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@ApplicationScoped
@Transactional
public class SanitizerBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SanitizerBean.class);
    private static final int BLOCK = 100;

    private final SanityManager manager;
    private final Settings settings;
    private final UserTransaction userTransaction;

    public SanitizerBean() {
        this(null, Settings.getInstance());
    }

    @Inject
    public SanitizerBean(final EntityManager entityManager, final UserTransaction userTransaction) {
        this(new SanityManager(Settings.getInstance(), entityManager), Settings.getInstance(), userTransaction);
    }

    public SanitizerBean(final EntityManager entityManager, final Settings settings) {
        this(new SanityManager(settings, entityManager), settings, null);
    }

    public SanitizerBean(final SanityManager manager) {
        this(manager, Settings.getInstance(), null);
    }

    public SanitizerBean(final SanityManager manager, final Settings settings, final UserTransaction userTransaction) {
        this.manager = manager;
        this.settings = settings;
        this.userTransaction = userTransaction;
    }

    /**
     * <p>Runs the sanity check (PLAN.md, S4). Each Data Object is checked
     * in its own transaction with the stream timeout, as reading a large
     * object takes time; a run checks at most
     * {@code eds.sanity.check.max.objects}. The outcome is kept in the
     * {@link SanityReport} for the health check.</p>
     */
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void sanitize() {
        final int timeout = settings.getStreamTransactionTimeout();
        StreamTransaction.run(userTransaction, timeout, () -> {
            manager.clearExpired();
            return null;
        });

        final boolean enabled = manager.isEnabled();
        final Counts counts = enabled ? checkAll(timeout) : new Counts(0, 0);

        final long failedTotal = StreamTransaction.run(userTransaction, timeout, manager::countFailed);
        SanityReport.completed(Utilities.newDate(), enabled, counts.checked(), counts.flawed(), failedTotal);
        LOGGER.info("Completed Sanity check, found {} flaws out of {} checked Data Objects; {} are marked as failed.", counts.flawed(), counts.checked(), failedTotal);
    }

    /**
     * Checks the Data Objects, batch by batch, until none are left or the
     * limit of a run is reached. A batch with nothing new ends the run too.
     *
     * @param timeout Timeout of each transaction, in seconds
     * @return How many were checked, and how many of them had a flaw
     */
    // The set of the Ids seen is read through add(), false for one seen before.
    @SuppressWarnings("fb-contrib:WOC_WRITE_ONLY_COLLECTION_LOCAL")
    private Counts checkAll(final int timeout) {
        final Set<Long> seen = new HashSet<>();
        long checked = 0;
        long flawed = 0;
        List<Long> ids = StreamTransaction.run(userTransaction, timeout, () -> manager.findNextBatch(BLOCK));
        while (!ids.isEmpty() && !manager.reachedLimit(checked)) {
            boolean progress = false;
            for (final Long id : ids) {
                if (seen.add(id) && !manager.reachedLimit(checked)) {
                    progress = true;
                    final SanityStatus status = StreamTransaction.run(userTransaction, timeout, () -> manager.processEntity(id));
                    if (status == SanityStatus.FAILED) {
                        flawed++;
                    }
                    checked++;
                }
            }
            ids = progress ? StreamTransaction.run(userTransaction, timeout, () -> manager.findNextBatch(BLOCK)) : List.of();
        }

        return new Counts(checked, flawed);
    }

    private record Counts(long checked, long flawed) {
    }
}
