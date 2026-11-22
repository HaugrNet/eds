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
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.responses.StatusKeyResponse;
import net.haugr.eds.api.responses.StatusResponse;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.MasterKey;
import net.haugr.eds.core.managers.StatusManager;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.socket.SocketHub;
import net.haugr.eds.core.status.StatusCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>CDI Bean for the status of the stone (STATUS.md) and final Error
 * handling layer. The gathering is driven from outside, by {@link #tick()}
 * once a second; a request for the status only reads what was gathered.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
@Transactional
public class StatusBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusBean.class);

    /** The database is asked how long a trivial query takes at most this often. */
    private static final long PROBE_NANOS = 10_000_000_000L;
    /** A status key which was found valid is trusted this long without asking the database again. */
    private static final long KEY_NANOS = 60_000_000_000L;

    private final EntityManager entityManager;
    private final Settings settings;
    private final TransactionSynchronizationRegistry transactions;
    // Hashes of status keys which were valid, with the time until which they
    // are taken as valid: a watcher which asks every second doesn't cost a
    // query each time.
    private final Map<String, Long> validKeys = new ConcurrentHashMap<>();
    private final AtomicReference<StatusManager.Database> database = new AtomicReference<>();
    private volatile long lastProbe = 0;

    public StatusBean() {
        this(null, null, (TransactionSynchronizationRegistry) null);
    }

    @Inject
    public StatusBean(final EntityManager entityManager, final TransactionSynchronizationRegistry transactions,
                      final Instance<SocketHub> socketHub) {
        this(entityManager, Settings.getInstance(), transactions);
        StatusCollector.getInstance().connections(() -> socketHub.get().connections());
    }

    public StatusBean(final EntityManager entityManager, final Settings settings) {
        this(entityManager, settings, null);
    }

    public StatusBean(final EntityManager entityManager, final Settings settings, final TransactionSynchronizationRegistry transactions) {
        this.entityManager = entityManager;
        this.settings = settings;
        this.transactions = transactions;
    }

    // =========================================================================
    // Gathering
    // =========================================================================

    /**
     * Gathers what is due: a sample of the load at the stone's load
     * interval, and one of the inventory at its inventory interval. Called
     * once a second; most calls find nothing to do.
     */
    public void tick() {
        tick(System.nanoTime());
    }

    /**
     * As {@link #tick()}, at the given time, for the tests.
     *
     * @param nanos The time, as of {@link System#nanoTime()}
     */
    public void tick(final long nanos) {
        final StatusCollector collector = StatusCollector.getInstance();

        if (collector.loadDue(nanos, settings.getStatusLoadInterval())) {
            if ((lastProbe == 0) || ((nanos - lastProbe) >= PROBE_NANOS)) {
                lastProbe = nanos;
                try {
                    collector.database(new StatusManager(settings, entityManager).probe());
                } catch (RuntimeException e) {
                    // The database isn't reached: no number is better than an old one.
                    collector.database(Double.NaN);
                    LOGGER.debug("The database couldn't be timed: {}", e.getMessage());
                }
            }
            collector.sampleLoad(nanos);
        }

        if (collector.inventoryDue(nanos, settings.getStatusInventoryInterval() * 60)) {
            try {
                final StatusManager manager = new StatusManager(settings, entityManager);
                collector.sampleInventory(nanos, manager.inventory());
                database.set(manager.database());
            } catch (RuntimeException e) {
                collector.inventoryFailed(nanos);
                LOGGER.debug("The inventory couldn't be gathered: {}", e.getMessage());
            }
        }
    }

    // =========================================================================
    // The status
    // =========================================================================

    /**
     * The status of the stone, for the System Administrator and for the
     * holder of a status key.
     *
     * @param token   A status key, or the Session token of the System Administrator
     * @param since   Only samples of the load after this sequence; null for all kept
     * @param history True to add the load of the last day and all kept samples of the inventory
     * @return The status
     */
    public StatusResponse status(final byte[] token, final Long since, final Boolean history) {
        try {
            authorize(token);

            final StatusCollector collector = StatusCollector.getInstance();
            final boolean all = Boolean.TRUE.equals(history);
            final StatusResponse response = new StatusResponse();
            response.setName(settings.getStoneName());
            response.setVersion(Constants.EDS_VERSION);
            response.setAwake(MasterKey.getInstance().isUnlocked());
            response.setTime(Utilities.newDate());
            response.setStarted(collector.started());
            final StatusManager.Database known = database.get();
            if (known != null) {
                response.setDatabase(known.product());
                response.setPersistent(known.persistent());
                response.setDatabaseMark(known.mark());
            }
            response.setLoad(collector.load(since));
            if (all) {
                response.setDay(collector.day());
            }
            response.setInventory(collector.inventory(all));

            return response;
        } catch (EDSException e) {
            Transactions.markRollbackOnly(transactions);
            LOGGER.debug("The request failed.", e);
            return new StatusResponse(e.getReturnCode(), e.getMessage());
        }
    }

    /** A status key is enough; without one it takes the Session of the System Administrator. */
    private void authorize(final byte[] token) {
        if ((token == null) || (token.length == 0)) {
            throw new EDSException(ReturnCode.AUTHENTICATION_WARNING, "A status key or the Session of the System Administrator is needed.");
        }
        final long now = System.nanoTime();
        final String checksum = StatusManager.checksum(token);
        final Long valid = validKeys.get(checksum);
        if ((valid != null) && ((valid - now) > 0)) {
            return;
        }
        final StatusManager manager = new StatusManager(settings, entityManager);
        if ((token.length == StatusManager.KEY_LENGTH) && manager.isStatusKey(checksum)) {
            validKeys.put(checksum, now + KEY_NANOS);
            return;
        }
        validKeys.remove(checksum);
        manager.administrator(token);
    }

    // =========================================================================
    // The status keys
    // =========================================================================

    public StatusKeyResponse keys(final byte[] token) {
        try {
            return new StatusManager(settings, entityManager).keys(token);
        } catch (EDSException e) {
            return failed(e);
        }
    }

    public StatusKeyResponse createKey(final byte[] token, final String label) {
        try {
            return new StatusManager(settings, entityManager).createKey(token, label);
        } catch (EDSException e) {
            return failed(e);
        }
    }

    public StatusKeyResponse deleteKey(final byte[] token, final String label) {
        try {
            final StatusManager manager = new StatusManager(settings, entityManager);
            final StatusKeyResponse response = manager.deleteKey(token, label);
            // A withdrawn key is refused at once.
            if (manager.withdrawn() != null) {
                validKeys.remove(manager.withdrawn());
            }
            return response;
        } catch (EDSException e) {
            return failed(e);
        }
    }

    private StatusKeyResponse failed(final EDSException e) {
        Transactions.markRollbackOnly(transactions);
        LOGGER.debug("The request failed.", e);
        return new StatusKeyResponse(e.getReturnCode(), e.getMessage());
    }
}
