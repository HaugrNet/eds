/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.core;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.requests.Authentication;
import net.haugr.cws.api.requests.FetchCircleRequest;
import net.haugr.cws.api.requests.FetchMemberRequest;
import net.haugr.cws.api.requests.FetchTrusteeRequest;
import net.haugr.cws.api.requests.InventoryRequest;
import net.haugr.cws.api.requests.MasterKeyRequest;
import net.haugr.cws.api.requests.ProcessCircleRequest;
import net.haugr.cws.api.requests.ProcessMemberRequest;
import net.haugr.cws.api.requests.ProcessTrusteeRequest;
import net.haugr.cws.api.requests.SanityRequest;
import net.haugr.cws.api.requests.SettingRequest;
import net.haugr.cws.api.responses.AuthenticateResponse;
import net.haugr.cws.api.responses.FetchCircleResponse;
import net.haugr.cws.api.responses.FetchMemberResponse;
import net.haugr.cws.api.responses.FetchTrusteeResponse;
import net.haugr.cws.api.responses.InventoryResponse;
import net.haugr.cws.api.responses.MasterKeyResponse;
import net.haugr.cws.api.responses.ProcessCircleResponse;
import net.haugr.cws.api.responses.ProcessMemberResponse;
import net.haugr.cws.api.responses.ProcessTrusteeResponse;
import net.haugr.cws.api.responses.SanityResponse;
import net.haugr.cws.api.responses.SettingResponse;
import net.haugr.cws.api.responses.VersionResponse;
import net.haugr.cws.core.exceptions.CWSException;
import net.haugr.cws.core.managers.AuthenticatedManager;
import net.haugr.cws.core.managers.FetchCircleManager;
import net.haugr.cws.core.managers.FetchMemberManager;
import net.haugr.cws.core.managers.FetchTrusteeManager;
import net.haugr.cws.core.managers.InventoryManager;
import net.haugr.cws.core.managers.MasterKeyManager;
import net.haugr.cws.core.managers.ProcessCircleManager;
import net.haugr.cws.core.managers.ProcessMemberManager;
import net.haugr.cws.core.managers.ProcessTrusteeManager;
import net.haugr.cws.core.managers.SanityManager;
import net.haugr.cws.core.managers.SettingManager;
import net.haugr.cws.core.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Java EE Bean for the Management functionality and final Error handling
 * layer. This is also the layer where transactions are controlled.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Stateless
public class ManagementBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementBean.class);

    @PersistenceContext
    private EntityManager entityManager;
    private final Settings settings = Settings.getInstance();

    @Transactional(Transactional.TxType.SUPPORTS)
    public VersionResponse version() {
        final VersionResponse response = new VersionResponse();
        response.setVersion(Constants.CWS_VERSION);

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public SettingResponse settings(final SettingRequest request) {
        SettingResponse response;

        try {
            final SettingManager manager = new SettingManager(settings, entityManager);
            response = manager.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new SettingResponse(e.getReturnCode(), e.getMessage());

            // For the case that we have a settings warning, the current
            // settings should be returned, so it can be shown what they
            // currently are - this way, an administrator will have to worry a
            // little less about the putting the system into an unusable state.
            if (e.getReturnCode() == ReturnCode.SETTING_WARNING) {
                response.setSettings(SettingManager.convert(settings));
            }
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public MasterKeyResponse masterKey(final MasterKeyRequest request) {
        MasterKeyResponse response;

        try {
            final MasterKeyManager manager = new MasterKeyManager(settings, entityManager);
            response = manager.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new MasterKeyResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public SanityResponse sanity(final SanityRequest request) {
        SanityResponse response;

        try {
            final SanityManager manager = new SanityManager(settings, entityManager);
            response = manager.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new SanityResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public InventoryResponse inventory(final InventoryRequest request) {
        InventoryResponse response;

        try {
            final InventoryManager manager = new InventoryManager(settings, entityManager);
            response = manager.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new InventoryResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public AuthenticateResponse authenticated(final Authentication request) {
        AuthenticateResponse response;

        try {
            final AuthenticatedManager manager = new AuthenticatedManager(settings, entityManager);
            response = manager.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new AuthenticateResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        FetchMemberResponse response;

        try {
            final FetchMemberManager manager = new FetchMemberManager(settings, entityManager);
            response = manager.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new FetchMemberResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        ProcessMemberResponse response;

        try {
            final ProcessMemberManager manager = new ProcessMemberManager(settings, entityManager);
            response = manager.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new ProcessMemberResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        FetchCircleResponse response;

        try {
            final FetchCircleManager manager = new FetchCircleManager(settings, entityManager);
            response = manager.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new FetchCircleResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        ProcessCircleResponse response;

        try {
            final ProcessCircleManager manager = new ProcessCircleManager(settings, entityManager);
            response = manager.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new ProcessCircleResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        FetchTrusteeResponse response;

        try {
            final FetchTrusteeManager manager = new FetchTrusteeManager(settings, entityManager);
            response = manager.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new FetchTrusteeResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        ProcessTrusteeResponse response;

        try {
            final ProcessTrusteeManager manager = new ProcessTrusteeManager(settings, entityManager);
            response = manager.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new ProcessTrusteeResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }
}
