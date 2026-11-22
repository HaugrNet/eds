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
import net.haugr.eds.core.exceptions.EDSException;
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

    public ManagementBean() {
        this(null, null, Settings.getInstance(), null, null);
    }

    @Inject
    public ManagementBean(final EntityManager entityManager, final SchemaManager schemaManager, final TransactionSynchronizationRegistry transactions, final Event<AccessChange> accessChanges) {
        this(entityManager, schemaManager, Settings.getInstance(), transactions, accessChanges);
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
        this.entityManager = entityManager;
        this.schemaManager = schemaManager;
        this.settings = settings;
        this.transactions = transactions;
        this.accessChanges = accessChanges;
    }

    /**
     * Lets the socket check its connections again, once the change is
     * committed (PLAN.md, L4): a change of Members, Circles, Trustees or
     * the MasterKey may end a Session or the access to a Circle.
     *
     * @param response The response of the change
     */
    private void accessChange(final EDSResponse response) {
        if ((accessChanges != null) && response.isOk()) {
            accessChanges.fire(AccessChange.INSTANCE);
        }
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
        SettingResponse response;

        try {
            final SettingManager manager = new SettingManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new SettingResponse(e.getReturnCode(), e.getMessage());

            // For the case that we have a settings warning, the current
            // settings should be returned, so it can be shown what they
            // currently are.
            if (e.getReturnCode() == ReturnCode.SETTING_WARNING) {
                response.setSettings(SettingManager.convert(settings));
            }
        }

        return response;
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
        MasterKeyResponse response;

        try {
            final MasterKeyManager manager = new MasterKeyManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new MasterKeyResponse(e.getReturnCode(), e.getMessage());
        }

        accessChange(response);
        return response;
    }

    public SanityResponse sanity(final SanityRequest request) {
        SanityResponse response;

        try {
            final SanityManager manager = new SanityManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new SanityResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    public AuthenticateResponse authenticated(final Authentication request) {
        AuthenticateResponse response;

        try {
            final AuthenticatedManager manager = new AuthenticatedManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new AuthenticateResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        FetchMemberResponse response;

        try {
            final FetchMemberManager manager = new FetchMemberManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new FetchMemberResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        ProcessMemberResponse response;

        try {
            final ProcessMemberManager manager = new ProcessMemberManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new ProcessMemberResponse(e.getReturnCode(), e.getMessage());
        }

        accessChange(response);
        return response;
    }

    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        FetchCircleResponse response;

        try {
            final FetchCircleManager manager = new FetchCircleManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new FetchCircleResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        ProcessCircleResponse response;

        try {
            final ProcessCircleManager manager = new ProcessCircleManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new ProcessCircleResponse(e.getReturnCode(), e.getMessage());
        }

        accessChange(response);
        return response;
    }

    public FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        FetchTrusteeResponse response;

        try {
            final FetchTrusteeManager manager = new FetchTrusteeManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new FetchTrusteeResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    public ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        ProcessTrusteeResponse response;

        try {
            final ProcessTrusteeManager manager = new ProcessTrusteeManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new ProcessTrusteeResponse(e.getReturnCode(), e.getMessage());
        }

        accessChange(response);
        return response;
    }
}
