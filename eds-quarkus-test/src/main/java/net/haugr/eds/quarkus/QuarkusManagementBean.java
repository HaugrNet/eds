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
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.requests.InventoryRequest;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.responses.AuthenticateResponse;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.api.responses.FetchTrusteeResponse;
import net.haugr.eds.api.responses.InventoryResponse;
import net.haugr.eds.api.responses.MasterKeyResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.api.responses.SanityResponse;
import net.haugr.eds.api.responses.SettingResponse;
import net.haugr.eds.api.responses.VersionResponse;
import net.haugr.eds.core.ManagementBean;

/**
 * <p>Quarkus-specific CDI bean that exposes the EDS core ShareBean for injection.
 * This avoids relying on the Stateless EJB, which Quarkus does not support.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class QuarkusManagementBean extends ManagementBean {

    /**
     * CDI Constructor.
     *
     * @param entityManager EDS EntityManager instance
     */
    @Inject
    public QuarkusManagementBean(final EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public VersionResponse version() {
        return super.version();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public SettingResponse settings(final SettingRequest request) {
        return super.settings(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public MasterKeyResponse masterKey(final MasterKeyRequest request) {
        return super.masterKey(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public SanityResponse sanity(final SanityRequest request) {
        return super.sanity(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public InventoryResponse inventory(final InventoryRequest request) {
        return super.inventory(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public AuthenticateResponse authenticated(final Authentication request) {
        return super.authenticated(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        return super.fetchMembers(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        return super.processMember(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        return super.fetchCircles(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        return super.processCircle(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        return super.fetchTrustees(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        return super.processTrustee(request);
    }
}
