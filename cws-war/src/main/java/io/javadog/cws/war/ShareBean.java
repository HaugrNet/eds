/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-war)
 * =============================================================================
 */
package io.javadog.cws.war;

import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.core.services.FetchDataService;
import io.javadog.cws.core.services.FetchDataTypeService;
import io.javadog.cws.core.services.FetchSignatureService;
import io.javadog.cws.core.services.ProcessDataService;
import io.javadog.cws.core.services.ProcessDataTypeService;
import io.javadog.cws.core.services.SignService;
import io.javadog.cws.core.services.VerifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Stateless
public class ShareBean {

    private static final Logger log = LoggerFactory.getLogger(ShareBean.class);

    //@PersistenceContext(unitName = "cwsDS")
    private EntityManager entityManager = null;

    private final Settings settings = new Settings();

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessDataTypeResponse processDataType(final ProcessDataTypeRequest request) {
        ProcessDataTypeResponse response;

        try {
            final Serviceable<ProcessDataTypeResponse, ProcessDataTypeRequest> service = new ProcessDataTypeService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new ProcessDataTypeResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.NEVER)
    public FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        FetchDataTypeResponse response;

        try {
            final Serviceable<FetchDataTypeResponse, FetchDataTypeRequest> service = new FetchDataTypeService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchDataTypeResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessDataResponse processData(final ProcessDataRequest request) {
        ProcessDataResponse response;

        try {
            final Serviceable<ProcessDataResponse, ProcessDataRequest> service = new ProcessDataService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new ProcessDataResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchDataResponse fetchData(final FetchDataRequest request) {
        FetchDataResponse response;

        try {
            final Serviceable<FetchDataResponse, FetchDataRequest> service = new FetchDataService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchDataResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public SignResponse sign(final SignRequest request) {
        SignResponse response;

        try {
            final Serviceable<SignResponse, SignRequest> service = new SignService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new SignResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public VerifyResponse verify(final VerifyRequest request) {
        VerifyResponse response;

        try {
            final Serviceable<VerifyResponse, VerifyRequest> service = new VerifyService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new VerifyResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchSignatureResponse fetchSignatures(final FetchSignatureRequest request) {
        FetchSignatureResponse response;

        try {
            final Serviceable<FetchSignatureResponse, FetchSignatureRequest> service = new FetchSignatureService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchSignatureResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }
}
