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

import net.haugr.eds.core.features.EntryQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.api.requests.EntryRequest;
import net.haugr.eds.api.responses.CircleFeatureResponse;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.EntryResponse;
import net.haugr.eds.api.responses.FeatureResponse;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.managers.FeatureManager;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.features.FeatureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>CDI Bean for the features (FEATURES.md) and final Error handling layer.
 * This is also the layer where transactions are controlled: every business
 * method runs in a transaction (REQUIRED). What a call changed is fired as
 * {@link FeatureEvent}s, which reach their observers once the transaction has
 * succeeded.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
@Transactional
public class FeatureBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureBean.class);

    private final EntityManager entityManager;
    private final Settings settings;
    private final TransactionSynchronizationRegistry transactions;
    // What changed, for the socket; null outside CDI, unless a test listens.
    private Consumer<FeatureEvent> changes = null;

    public FeatureBean() {
        this(null, null, (TransactionSynchronizationRegistry) null);
    }

    @Inject
    public FeatureBean(final EntityManager entityManager, final TransactionSynchronizationRegistry transactions, final Event<FeatureEvent> changes) {
        this(entityManager, Settings.getInstance(), transactions);
        this.changes = changes::fire;
    }

    /**
     * For tests without CDI, where the listener is told at once.
     *
     * @param entityManager Entity Manager
     * @param settings      EDS Settings
     * @param listener      Is told what changed
     */
    public FeatureBean(final EntityManager entityManager, final Settings settings, final Consumer<FeatureEvent> listener) {
        this(entityManager, settings, (TransactionSynchronizationRegistry) null);
        this.changes = listener;
    }

    public FeatureBean(final EntityManager entityManager, final Settings settings) {
        this(entityManager, settings, (TransactionSynchronizationRegistry) null);
    }

    public FeatureBean(final EntityManager entityManager, final Settings settings, final TransactionSynchronizationRegistry transactions) {
        this.entityManager = entityManager;
        this.settings = settings;
        this.transactions = transactions;
    }

    // =========================================================================
    // For the socket, which answers failures itself
    // =========================================================================

    public FeatureManager.Identity identify(final byte[] token) {
        return new FeatureManager(settings, entityManager).identify(token);
    }

    public Set<String> readableCircles(final byte[] token) {
        return new FeatureManager(settings, entityManager).readableCircles(token);
    }

    public long signal(final byte[] token, final String circleId, final String feature) {
        return new FeatureManager(settings, entityManager).signal(token, circleId, feature);
    }

    // =========================================================================
    // The features of the stone
    // =========================================================================

    public FeatureResponse features(final byte[] token) {
        return run(manager -> manager.features(token), FeatureResponse::new);
    }

    public FeatureResponse putFeature(final byte[] token, final String name, final String declaration) {
        return run(manager -> manager.putFeature(token, name, declaration), FeatureResponse::new);
    }

    public FeatureResponse deleteFeature(final byte[] token, final String name) {
        return run(manager -> manager.deleteFeature(token, name), FeatureResponse::new);
    }

    // =========================================================================
    // The features of a Circle
    // =========================================================================

    public CircleFeatureResponse circleFeatures(final byte[] token, final String circleId) {
        return run(manager -> manager.circleFeatures(token, circleId), CircleFeatureResponse::new);
    }

    public CircleFeatureResponse setCircleFeature(final byte[] token, final String circleId, final String name, final FeatureState state) {
        return run(manager -> manager.setCircleFeature(token, circleId, name, state), CircleFeatureResponse::new);
    }

    public CircleFeatureResponse removeCircleFeature(final byte[] token, final String circleId, final String name) {
        return run(manager -> manager.removeCircleFeature(token, circleId, name), CircleFeatureResponse::new);
    }

    // =========================================================================
    // Entries
    // =========================================================================

    public EntryResponse addEntry(final byte[] token, final String circleId, final String feature, final EntryRequest body) {
        return run(manager -> manager.addEntry(token, circleId, feature, body), EntryResponse::new);
    }

    public EntryResponse changeEntry(final byte[] token, final String circleId, final String feature, final String entryId, final EntryRequest body) {
        return run(manager -> manager.changeEntry(token, circleId, feature, entryId, body), EntryResponse::new);
    }

    public EntryResponse removeEntry(final byte[] token, final String circleId, final String feature, final String entryId) {
        return run(manager -> manager.removeEntry(token, circleId, feature, entryId), EntryResponse::new);
    }

    public EntryResponse closeEntry(final byte[] token, final String circleId, final String feature, final String entryId, final Boolean closed) {
        return run(manager -> manager.closeEntry(token, circleId, feature, entryId, closed), EntryResponse::new);
    }

    public EntryResponse entry(final byte[] token, final String circleId, final String feature, final String entryId) {
        return run(manager -> manager.entry(token, circleId, feature, entryId), EntryResponse::new);
    }

    public EntryResponse entries(final byte[] token, final String circleId, final String feature, final EntryQuery query) {
        return run(manager -> manager.entries(token, circleId, feature, query), EntryResponse::new);
    }

    public EntryResponse count(final byte[] token, final String circleId, final String feature, final String kind, final Long after) {
        return run(manager -> manager.count(token, circleId, feature, kind, after), EntryResponse::new);
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    /**
     * Runs one call of the manager. A failure becomes the response, and the
     * transaction is rolled back, so nothing of a refused request stays.
     */
    private <R extends EDSResponse> R run(final Function<FeatureManager, R> call, final BiFunction<ReturnCode, String, R> failure) {
        final FeatureManager manager = new FeatureManager(settings, entityManager);
        try {
            final R response = call.apply(manager);
            if (changes != null) {
                for (final FeatureEvent event : manager.events()) {
                    changes.accept(event);
                }
            }
            return response;
        } catch (EDSException e) {
            Transactions.markRollbackOnly(transactions);
            LOGGER.debug("The request failed.", e);
            return failure.apply(e.getReturnCode(), e.getMessage());
        }
    }
}
