/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.requests.FetchSignatureRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessDataTypeRequest;
import net.haugr.eds.api.requests.SignRequest;
import net.haugr.eds.api.requests.VerifyRequest;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.FetchDataTypeResponse;
import net.haugr.eds.api.responses.FetchSignatureResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessDataTypeResponse;
import net.haugr.eds.api.responses.SignResponse;
import net.haugr.eds.api.responses.VerifyResponse;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.managers.FetchDataManager;
import net.haugr.eds.core.managers.FetchDataTypeManager;
import net.haugr.eds.core.managers.FetchSignatureManager;
import net.haugr.eds.core.managers.ProcessDataManager;
import net.haugr.eds.core.managers.ProcessDataTypeManager;
import net.haugr.eds.core.managers.SignManager;
import net.haugr.eds.core.managers.VerifyManager;
import net.haugr.eds.core.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Java EE Bean for the Share functionality and final Error handling layer.
 * This is also the layer where transactions are controlled.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Stateless
public class ShareBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareBean.class);

    @PersistenceContext
    private EntityManager entityManager;
    private final Settings settings = Settings.getInstance();

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessDataTypeResponse processDataType(final ProcessDataTypeRequest request) {
        ProcessDataTypeResponse response;

        try {
            final ProcessDataTypeManager manager = new ProcessDataTypeManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new ProcessDataTypeResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        FetchDataTypeResponse response;

        try {
            final FetchDataTypeManager manager = new FetchDataTypeManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new FetchDataTypeResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessDataResponse processData(final ProcessDataRequest request) {
        ProcessDataResponse response;

        try {
            final ProcessDataManager manager = new ProcessDataManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new ProcessDataResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public FetchDataResponse fetchData(final FetchDataRequest request) {
        FetchDataResponse response;

        try {
            final FetchDataManager manager = new FetchDataManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new FetchDataResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public SignResponse sign(final SignRequest request) {
        SignResponse response;

        try {
            final SignManager manager = new SignManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new SignResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public VerifyResponse verify(final VerifyRequest request) {
        VerifyResponse response;

        try {
            final VerifyManager manager = new VerifyManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new VerifyResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchSignatureResponse fetchSignatures(final FetchSignatureRequest request) {
        FetchSignatureResponse response;

        try {
            final FetchSignatureManager manager = new FetchSignatureManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new FetchSignatureResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }
}
