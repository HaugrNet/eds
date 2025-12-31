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
package net.haugr.eds.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
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

/**
 * <p>Quarkus-specific CDI bean that exposes the EDS core ShareBean for injection.
 * This avoids relying on the Stateless EJB, which Quarkus does not support.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class QuarkusShareBean extends ShareBean {

    /**
     * CDI Constructor.
     *
     * @param entityManager EDS EntityManager instance
     */
    @Inject
    public QuarkusShareBean(final EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessDataTypeResponse processDataType(final ProcessDataTypeRequest request) {
        return super.processDataType(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        return super.fetchDataTypes(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessDataResponse processData(final ProcessDataRequest request) {
        return super.processData(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public FetchDataResponse fetchData(final FetchDataRequest request) {
        return super.fetchData(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public SignResponse sign(final SignRequest request) {
        return super.sign(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public VerifyResponse verify(final VerifyRequest request) {
        return super.verify(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public FetchSignatureResponse fetchSignatures(final FetchSignatureRequest request) {
        return super.fetchSignatures(request);
    }
}
