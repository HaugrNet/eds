/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-war)
 * =============================================================================
 */
package io.javadog.cws.war;

import static io.javadog.cws.war.CommonBean.destroy;

import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.core.services.FetchCircleService;
import io.javadog.cws.core.services.FetchMemberService;
import io.javadog.cws.core.services.ProcessCircleService;
import io.javadog.cws.core.services.ProcessMemberService;
import io.javadog.cws.core.services.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Stateless
public class SystemBean {

    private static final Logger log = LoggerFactory.getLogger(SystemBean.class);

    @PersistenceContext(unitName = "cwsDS")
    private EntityManager entityManager;

    @Inject private SettingBean settingBean;

    @Transactional(Transactional.TxType.NEVER)
    public VersionResponse version() {
        final VersionResponse response = new VersionResponse();
        response.setVersion("1.0");

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public SettingResponse settings(final SettingRequest request) {
        Serviceable<SettingResponse, SettingRequest> service = null;
        SettingResponse response;

        try {
            service = new SettingService(settingBean.getSettings(), entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new SettingResponse(e.getReturnCode(), e.getMessage());
        } finally {
            destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.NEVER)
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        Serviceable<FetchMemberResponse, FetchMemberRequest> service = null;
        FetchMemberResponse response;

        try {
            service = new FetchMemberService(settingBean.getSettings(), entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchMemberResponse(e.getReturnCode(), e.getMessage());
        } finally {
            destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        Serviceable<ProcessMemberResponse, ProcessMemberRequest> service = null;
        ProcessMemberResponse response;

        try {
            service = new ProcessMemberService(settingBean.getSettings(), entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new ProcessMemberResponse(e.getReturnCode(), e.getMessage());
        } finally {
            destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.NEVER)
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        Serviceable<FetchCircleResponse, FetchCircleRequest> service = null;
        FetchCircleResponse response;

        try {
            service = new FetchCircleService(settingBean.getSettings(), entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchCircleResponse(e.getReturnCode(), e.getMessage());
        } finally {
            destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        Serviceable<ProcessCircleResponse, ProcessCircleRequest> service = null;
        ProcessCircleResponse response;

        try {
            service = new ProcessCircleService(settingBean.getSettings(), entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new ProcessCircleResponse(e.getReturnCode(), e.getMessage());
        } finally {
            destroy(service);
        }

        return response;
    }
}
