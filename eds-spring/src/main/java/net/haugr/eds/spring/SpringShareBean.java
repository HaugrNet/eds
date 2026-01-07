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
package net.haugr.eds.spring;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import net.haugr.eds.core.ShareBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring-specific service that wraps the EDS core ShareBean.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Service
public class SpringShareBean extends ShareBean implements Logged {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringShareBean.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Default constructor for Spring.
     */
    public SpringShareBean() {
        super(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProcessDataTypeResponse processDataType(final ProcessDataTypeRequest request) {
        return logRequest(LOGGER, "processDataType[" + request.getAction() + "]", () -> new ShareBean(entityManager).processDataType(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        return logRequest(LOGGER, "fetchDataTypes", () -> new ShareBean(entityManager).fetchDataTypes(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProcessDataResponse processData(final ProcessDataRequest request) {
        return logRequest(LOGGER, "processData[" + request.getAction() + "]", () -> new ShareBean(entityManager).processData(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public FetchDataResponse fetchData(final FetchDataRequest request) {
        return logRequest(LOGGER, "fetchData", () -> new ShareBean(entityManager).fetchData(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public SignResponse sign(final SignRequest request) {
        return logRequest(LOGGER, "sign", () -> new ShareBean(entityManager).sign(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public VerifyResponse verify(final VerifyRequest request) {
        return logRequest(LOGGER, "verify", () -> new ShareBean(entityManager).verify(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public FetchSignatureResponse fetchSignatures(final FetchSignatureRequest request) {
        return logRequest(LOGGER, "fetchSignatures", () -> new ShareBean(entityManager).fetchSignatures(request));
    }
}
