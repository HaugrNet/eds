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
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import net.haugr.eds.core.socket.AccessChange;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.responses.AuthenticateResponse;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.api.responses.FetchTrusteeResponse;
import net.haugr.eds.api.responses.MasterKeyResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.api.responses.SanityResponse;
import net.haugr.eds.api.responses.SettingResponse;
import net.haugr.eds.api.responses.VersionResponse;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.managers.AbstractManager;
import net.haugr.eds.core.managers.AuthenticatedManager;
import net.haugr.eds.core.managers.FetchCircleManager;
import net.haugr.eds.core.managers.FetchMemberManager;
import net.haugr.eds.core.managers.FetchTrusteeManager;
import net.haugr.eds.core.managers.MasterKeyManager;
import net.haugr.eds.core.managers.ProcessCircleManager;
import net.haugr.eds.core.managers.ProcessMemberManager;
import net.haugr.eds.core.managers.ProcessTrusteeManager;
import net.haugr.eds.core.managers.SanityManager;
import net.haugr.eds.core.model.SchemaManager;
import net.haugr.eds.core.managers.SettingManager;
import net.haugr.eds.core.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>CDI Bean for the Management functionality and final Error handling
 * layer. This is also the layer where transactions are controlled: every
 * business method runs in a transaction (REQUIRED). Even reading requests
 * may write, as the first who comes to a stone without accounts becomes its
 * System Administrator.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@ApplicationScoped
@Transactional
public class ManagementBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementBean.class);

    private final EntityManager entityManager;
    private final Settings settings;
    private final SchemaManager schemaManager;
    private final TransactionSynchronizationRegistry transactions;
    private final Event<AccessChange> accessChanges;
    private final AuthLimitBean authLimit;

    public ManagementBean() {
        this(null, null, Settings.getInstance(), null, null);
    }

    @Inject
    public ManagementBean(final EntityManager entityManager, final SchemaManager schemaManager, final TransactionSynchronizationRegistry transactions,
                          final Event<AccessChange> accessChanges, final AuthLimitBean authLimit) {
        this(entityManager, schemaManager, Settings.getInstance(), transactions, accessChanges, authLimit);
    }

    public ManagementBean(final EntityManager entityManager, final Settings settings) {
        this(entityManager, null, settings, null, null);
    }

    public ManagementBean(final EntityManager entityManager, final Settings settings, final TransactionSynchronizationRegistry transactions) {
        this(entityManager, null, settings, transactions, null);
    }

    public ManagementBean(final EntityManager entityManager, final Settings settings, final TransactionSynchronizationRegistry transactions, final Event<AccessChange> accessChanges) {
        this(entityManager, null, settings, transactions, accessChanges);
    }

    public ManagementBean(final EntityManager entityManager, final SchemaManager schemaManager, final Settings settings,
                          final TransactionSynchronizationRegistry transactions, final Event<AccessChange> accessChanges) {
        this(entityManager, schemaManager, settings, transactions, accessChanges, new AuthLimitBean(entityManager, settings));
    }

    public ManagementBean(final EntityManager entityManager, final SchemaManager schemaManager, final Settings settings,
                          final TransactionSynchronizationRegistry transactions, final Event<AccessChange> accessChanges, final AuthLimitBean authLimit) {
        this.entityManager = entityManager;
        this.schemaManager = schemaManager;
        this.settings = settings;
        this.transactions = transactions;
        this.accessChanges = accessChanges;
        this.authLimit = authLimit;
    }

    /**
     * Lets the socket check its connections again, once the change is
     * committed (PLAN.md, L4): a change of Members, Circles, Trustees or
     * the MasterKey may end a Session or the access to a Circle. A change
     * of one Circle names it, so only the connections which joined it are
     * checked (ticket 18).
     *
     * @param response The response of the change
     * @param change   What changed, or null when nothing can end an access
     */
    private void accessChange(final EDSResponse response, final AccessChange change) {
        if ((accessChanges != null) && (change != null) && response.isOk()) {
            accessChanges.fire(change);
        }
    }

    /**
     * <p>Runs a Management request: invokes the business logic, and converts an
     * EDS Exception into the typed error response. As the Exception never
     * reaches the transaction interceptor, the transaction is marked
     * rollback-only here, so partial writes are not committed.</p>
     *
     * <p>The manager counts the attempts at a passphrase (#68) with the
     * {@link AuthLimitBean}, in transactions of their own, so the rollback of
     * the request doesn't undo them.</p>
     *
     * @param request The request
     * @param manager Makes the manager with the business logic to run
     * @param onError Builds the typed error response from code and message
     * @param <Q>     Request type
     * @param <R>     Response type
     * @return The response, successful or converted from an Exception
     */
    private <Q extends Authentication, R extends EDSResponse> R execute(final Q request, final Supplier<? extends AbstractManager<?, R, Q>> manager,
                                                                        final BiFunction<ReturnCode, String, R> onError) {
        R response;

        try {
            final AbstractManager<?, R, Q> logic = manager.get();
            logic.limitAttempts(authLimit);
            response = logic.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting System.
            LOGGER.debug("The request failed.", e);
            response = onError.apply(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    public VersionResponse version() {
        final VersionResponse response = new VersionResponse();
        response.setVersion(Constants.EDS_VERSION);
        if (schemaManager != null) {
            response.setDatabase(schemaManager.getDatabaseProduct());
            response.setPersistent(schemaManager.isPersistent());
            response.setStorageCeiling(storageCeiling());
        }

        return response;
    }

    /** The effective storage ceiling, or null if the database has not been reached since EDS started. */
    private Long storageCeiling() {
        if (schemaManager.isPersistent() == null) {
            return null;
        }
        try {
            return entityManager.unwrap(org.hibernate.Session.class)
                    .doReturningWork(connection -> SchemaManager.ceiling(settings, connection));
        } catch (RuntimeException e) {
            LOGGER.debug("Cannot determine the storage ceiling: {}", e.getMessage());
            return null;
        }
    }

    public SettingResponse settings(final SettingRequest request) {
        return execute(request, () -> new SettingManager(settings, entityManager), (code, message) -> {
            final SettingResponse response = new SettingResponse(code, message);
            // For the case that we have a settings warning, the current
            // settings should be returned, so it can be shown what they
            // currently are.
            if (code == ReturnCode.SETTING_WARNING) {
                response.setSettings(SettingManager.convert(settings));
            }
            return response;
        });
    }

    /**
     * Loads the MasterKey from its configured source, at startup. It runs in
     * a transaction, as the first setup stores the MasterKey version and key
     * check value. Without a valid source, EDS stays locked.
     *
     * @return True if EDS was unlocked
     */
    public boolean loadMasterKey() {
        boolean unlocked = false;

        try {
            unlocked = new MasterKeyManager(settings, entityManager).loadAtStartup();
        } catch (EDSException e) {
            Transactions.markRollbackOnly(transactions);
            LOGGER.error("Loading the MasterKey failed: {}", e.getMessage(), e);
        }

        return unlocked;
    }

    public MasterKeyResponse masterKey(final MasterKeyRequest request) {
        final MasterKeyManager manager = new MasterKeyManager(settings, entityManager);
        final MasterKeyResponse response = execute(request, () -> manager, MasterKeyResponse::new);
        if (response.isOk()) {
            // A new MasterKey only once it is committed (ticket 13).
            manager.installAfterCommit(transactions);
        }
        accessChange(response, AccessChange.EVERYONE);
        return response;
    }

    public SanityResponse sanity(final SanityRequest request) {
        return execute(request, () -> new SanityManager(settings, entityManager), SanityResponse::new);
    }

    public AuthenticateResponse authenticated(final Authentication request) {
        return execute(request, () -> new AuthenticatedManager(settings, entityManager), AuthenticateResponse::new);
    }

    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        return execute(request, () -> new FetchMemberManager(settings, entityManager), FetchMemberResponse::new);
    }

    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        final ProcessMemberResponse response = execute(request, () -> new ProcessMemberManager(settings, entityManager), ProcessMemberResponse::new);
        // Creating and inviting an account end no access; a login may end
        // the oldest Session of the account (eds.session.max.per.member).
        final Action action = (request != null) ? request.getAction() : null;
        accessChange(response, ((action == Action.CREATE) || (action == Action.INVITE)) ? null : AccessChange.EVERYONE);
        return response;
    }

    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        return execute(request, () -> new FetchCircleManager(settings, entityManager), FetchCircleResponse::new);
    }

    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        final ProcessCircleResponse response = execute(request, () -> new ProcessCircleManager(settings, entityManager), ProcessCircleResponse::new);
        // A new Circle ends no access; the others name their Circle.
        accessChange(response, ((request == null) || (request.getAction() == Action.CREATE)) ? null : AccessChange.of(request.getCircleId()));
        return response;
    }

    public FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        return execute(request, () -> new FetchTrusteeManager(settings, entityManager), FetchTrusteeResponse::new);
    }

    public ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        final ProcessTrusteeResponse response = execute(request, () -> new ProcessTrusteeManager(settings, entityManager), ProcessTrusteeResponse::new);
        accessChange(response, (request != null) ? AccessChange.of(request.getCircleId()) : null);
        return response;
    }
}
