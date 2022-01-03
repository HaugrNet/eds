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

import net.haugr.cws.api.requests.FetchDataRequest;
import net.haugr.cws.api.requests.FetchDataTypeRequest;
import net.haugr.cws.api.requests.FetchSignatureRequest;
import net.haugr.cws.api.requests.ProcessDataRequest;
import net.haugr.cws.api.requests.ProcessDataTypeRequest;
import net.haugr.cws.api.requests.SignRequest;
import net.haugr.cws.api.requests.VerifyRequest;
import net.haugr.cws.api.responses.FetchDataResponse;
import net.haugr.cws.api.responses.FetchDataTypeResponse;
import net.haugr.cws.api.responses.FetchSignatureResponse;
import net.haugr.cws.api.responses.ProcessDataResponse;
import net.haugr.cws.api.responses.ProcessDataTypeResponse;
import net.haugr.cws.api.responses.SignResponse;
import net.haugr.cws.api.responses.VerifyResponse;
import net.haugr.cws.core.exceptions.CWSException;
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.services.FetchDataService;
import net.haugr.cws.core.services.FetchDataTypeService;
import net.haugr.cws.core.services.FetchSignatureService;
import net.haugr.cws.core.services.ProcessDataService;
import net.haugr.cws.core.services.ProcessDataTypeService;
import net.haugr.cws.core.services.SignService;
import net.haugr.cws.core.services.VerifyService;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 * <p>Java EE Bean for the Share functionality and final Error handling layer.
 * This is also the layer where transactions are controlled.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Stateless
public class ShareBean {

    private static final Logger LOG = Logger.getLogger(ShareBean.class.getName());

    @PersistenceContext
    private EntityManager entityManager;
    private final Settings settings = Settings.getInstance();

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessDataTypeResponse processDataType(final ProcessDataTypeRequest request) {
        ProcessDataTypeService service = null;
        ProcessDataTypeResponse response;

        try {
            service = new ProcessDataTypeService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new ProcessDataTypeResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        FetchDataTypeService service = null;
        FetchDataTypeResponse response;

        try {
            service = new FetchDataTypeService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new FetchDataTypeResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessDataResponse processData(final ProcessDataRequest request) {
        ProcessDataService service = null;
        ProcessDataResponse response;

        try {
            service = new ProcessDataService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new ProcessDataResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public FetchDataResponse fetchData(final FetchDataRequest request) {
        FetchDataService service = null;
        FetchDataResponse response;

        try {
            service = new FetchDataService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new FetchDataResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public SignResponse sign(final SignRequest request) {
        SignService service = null;
        SignResponse response;

        try {
            service = new SignService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new SignResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public VerifyResponse verify(final VerifyRequest request) {
        VerifyService service = null;
        VerifyResponse response;

        try {
            service = new VerifyService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new VerifyResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchSignatureResponse fetchSignatures(final FetchSignatureRequest request) {
        FetchSignatureService service = null;
        FetchSignatureResponse response;

        try {
            service = new FetchSignatureService(settings, entityManager);
            response = service.perform(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            LOG.log(Settings.DEBUG, e.getMessage(), e);
            response = new FetchSignatureResponse(e.getReturnCode(), e.getMessage());
        } finally {
            CommonBean.destroy(service);
        }

        return response;
    }
}
