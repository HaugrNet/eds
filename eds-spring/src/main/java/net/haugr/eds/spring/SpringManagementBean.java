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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring-specific service that wraps the EDS core ManagementBean.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Service
public class SpringManagementBean extends ManagementBean implements Logged {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringManagementBean.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Default constructor for Spring.
     */
    public SpringManagementBean() {
        super(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public VersionResponse version() {
        return logRequest(LOGGER, "version", () -> new ManagementBean(entityManager).version());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public SettingResponse settings(final SettingRequest request) {
        return logRequest(LOGGER, "settings", () -> new ManagementBean(entityManager).settings(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public MasterKeyResponse masterKey(final MasterKeyRequest request) {
        return logRequest(LOGGER, "masterKey", () -> new ManagementBean(entityManager).masterKey(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public SanityResponse sanity(final SanityRequest request) {
        return logRequest(LOGGER, "sanity", () -> new ManagementBean(entityManager).sanity(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public InventoryResponse inventory(final InventoryRequest request) {
        return logRequest(LOGGER, "inventory", () -> new ManagementBean(entityManager).inventory(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AuthenticateResponse authenticated(final Authentication request) {
        return logRequest(LOGGER, "authenticated", () -> new ManagementBean(entityManager).authenticated(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        return logRequest(LOGGER, "fetchMembers", () -> new ManagementBean(entityManager).fetchMembers(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        return logRequest(LOGGER, "processMember[" + request.getAction() + "]", () -> new ManagementBean(entityManager).processMember(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        return logRequest(LOGGER, "fetchCircles", () -> new ManagementBean(entityManager).fetchCircles(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        return logRequest(LOGGER, "processCircle[" + request.getAction() + "]", () -> new ManagementBean(entityManager).processCircle(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        return logRequest(LOGGER, "fetchTrustees", () -> new ManagementBean(entityManager).fetchTrustees(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        return logRequest(LOGGER, "processTrustee[" + request.getAction() + "]", () -> new ManagementBean(entityManager).processTrustee(request));
    }
}
