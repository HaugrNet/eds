/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.core;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.services.FetchCircleService;
import io.javadog.cws.core.services.FetchMemberService;
import io.javadog.cws.core.services.FetchTrusteeService;
import io.javadog.cws.core.services.MasterKeyService;
import io.javadog.cws.core.services.ProcessCircleService;
import io.javadog.cws.core.services.ProcessMemberService;
import io.javadog.cws.core.services.ProcessTrusteeService;
import io.javadog.cws.core.services.SanityService;
import io.javadog.cws.core.services.SettingService;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.logging.Logger;

/**
 * <p>Java EE Bean for the Management functionality and final Error handling
 * layer. This is also the layer where transactions are controlled.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Stateless
public class ManagementBean {

    private static final Logger LOG = Logger.getLogger(ManagementBean.class.getName());

    @PersistenceContext private EntityManager entityManager;
    private final Settings settings = Settings.getInstance();

    @Transactional(Transactional.TxType.SUPPORTS)
    public VersionResponse version() {
        final VersionResponse response = new VersionResponse();
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
