/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-war)
 * =============================================================================
 */
package io.javadog.cws.war;

import io.javadog.cws.api.System;
import io.javadog.cws.api.common.ReturnCode;
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
import io.javadog.cws.common.Settings;
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
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@CWSBean
@Stateless
public class SystemBean implements System {

    private static final Logger log = LoggerFactory.getLogger(SystemBean.class);

    //@PersistenceContext(unitName = "cwsDatabase")
    private EntityManager entityManager = null;

    private final Settings settings = new Settings();

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.NEVER)
    public VersionResponse version() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final String cwsConfig = "cws.config";
        VersionResponse response;

        try (InputStream stream = loader.getResourceAsStream(cwsConfig)) {
            final Properties properties = new Properties();
            properties.load(stream);

            response = new VersionResponse();
            response.setVersion(properties.getProperty("cws.version"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            response = new VersionResponse(ReturnCode.ERROR, e.getMessage());
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public SettingResponse settings(final SettingRequest request) {
        SettingResponse response;

        try {
            final Serviceable<SettingResponse, SettingRequest> service = new SettingService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new SettingResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.NEVER)
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        FetchMemberResponse response;

        try {
            final Serviceable<FetchMemberResponse, FetchMemberRequest> service = new FetchMemberService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchMemberResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        ProcessMemberResponse response;

        try {
            final Serviceable<ProcessMemberResponse, ProcessMemberRequest> service = new ProcessMemberService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new ProcessMemberResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.NEVER)
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        FetchCircleResponse response;

        try {
            final Serviceable<FetchCircleResponse, FetchCircleRequest> service = new FetchCircleService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchCircleResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        ProcessCircleResponse response;

        try {
            final Serviceable<ProcessCircleResponse, ProcessCircleRequest> service = new ProcessCircleService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new ProcessCircleResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }
}
