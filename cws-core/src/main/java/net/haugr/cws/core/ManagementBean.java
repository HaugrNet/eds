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
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.services.AuthenticatedService;
import net.haugr.cws.core.services.FetchCircleService;
import net.haugr.cws.core.services.FetchMemberService;
import net.haugr.cws.core.services.FetchTrusteeService;
import net.haugr.cws.core.services.InventoryService;
import net.haugr.cws.core.services.MasterKeyService;
import net.haugr.cws.core.services.ProcessCircleService;
import net.haugr.cws.core.services.ProcessMemberService;
import net.haugr.cws.core.services.ProcessTrusteeService;
import net.haugr.cws.core.services.SanityService;
import net.haugr.cws.core.services.SettingService;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 * <p>Java EE Bean for the Management functionality and final Error handling
 * layer. This is also the layer where transactions are controlled.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Stateless
public class ManagementBean {

    private static final Logger LOG = Logger.getLogger(ManagementBean.class.getName());

    @PersistenceContext
    private EntityManager entityManager;
    private final Settings settings = Settings.getInstance();

    @Transactional(Transactional.TxType.SUPPORTS)
    public VersionResponse version() {
        final var response = new VersionResponse();
        response.setVersion(Constants.CWS_VERSION);

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public SettingResponse settings(final SettingRequest request) {
        SettingService service = null;
        SettingResponse response;

        try {
            service = new SettingService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new SettingResponse(e.getReturnCode(), e.getMessage());

            // For the case that we have a settings warning, the current
            // settings should be returned, so it can be shown what they
            // currently are - this way, an administrator will have to worry a
            // little less about the putting the system into an unusable state.
            if (e.getReturnCode() == ReturnCode.SETTING_WARNING) {
                response.setSettings(SettingService.convert(settings));
            }
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public MasterKeyResponse masterKey(final MasterKeyRequest request) {
        MasterKeyService service = null;
        MasterKeyResponse response;

        try {
            service = new MasterKeyService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new MasterKeyResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public SanityResponse sanity(final SanityRequest request) {
        SanityService service = null;
        SanityResponse response;

        try {
            service = new SanityService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new SanityResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public InventoryResponse inventory(final InventoryRequest request) {
        InventoryService service = null;
        InventoryResponse response;

        try {
            service = new InventoryService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new InventoryResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public AuthenticateResponse authenticated(final Authentication request) {
        AuthenticatedService service = null;
        AuthenticateResponse response;

        try {
            service = new AuthenticatedService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new AuthenticateResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        FetchMemberService service = null;
        FetchMemberResponse response;

        try {
            service = new FetchMemberService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new FetchMemberResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        ProcessMemberService service = null;
        ProcessMemberResponse response;

        try {
            service = new ProcessMemberService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new ProcessMemberResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        FetchCircleService service = null;
        FetchCircleResponse response;

        try {
            service = new FetchCircleService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new FetchCircleResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        ProcessCircleService service = null;
        ProcessCircleResponse response;

        try {
            service = new ProcessCircleService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new ProcessCircleResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        FetchTrusteeService service = null;
        FetchTrusteeResponse response;

        try {
            service = new FetchTrusteeService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new FetchTrusteeResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        ProcessTrusteeService service = null;
        ProcessTrusteeResponse response;

        try {
            service = new ProcessTrusteeService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new ProcessTrusteeResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }
}
