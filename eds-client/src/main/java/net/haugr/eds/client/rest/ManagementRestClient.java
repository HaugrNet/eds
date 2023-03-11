/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
package net.haugr.eds.client.rest;

import net.haugr.eds.api.Management;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.ActionRequest;
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

/**
 * <p>Gson based REST Client for the EDS Management functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ManagementRestClient extends GsonRestClient implements Management {

    private static final String UNSUPPORTED_OPERATION = "Unsupported Operation: ";
    private static final String INVALID_REQUEST = "Cannot perform request, as the Request Object is missing or incomplete.";

    /**
     * Constructor for the EDS System REST Client. It takes the base URL for the
     * EDS Instance to communicate with, which is the protocol, hostname, port
     * and deployment name. For example; &quot;http://localhost:8080/eds&quot;.
     *
     * @param baseURL Base URL for the EDS Instance
     */
    public ManagementRestClient(final String baseURL) {
        super(baseURL);
    }

    // =========================================================================
    // Implementation of the Management Interface
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionResponse version() {
        return runRequest(VersionResponse.class, Constants.REST_VERSION, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingResponse settings(final SettingRequest request) {
        return runRequest(SettingResponse.class, Constants.REST_SETTINGS, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MasterKeyResponse masterKey(final MasterKeyRequest request) {
        return runRequest(MasterKeyResponse.class, Constants.REST_MASTERKEY, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SanityResponse sanitized(final SanityRequest request) {
        return runRequest(SanityResponse.class, Constants.REST_SANITIZED, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InventoryResponse inventory(final InventoryRequest request) {
        return runRequest(InventoryResponse.class, Constants.REST_INVENTORY, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthenticateResponse authenticated(final Authentication request) {
        return runRequest(AuthenticateResponse.class, Constants.REST_AUTHENTICATED, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        return runRequest(FetchMemberResponse.class, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_FETCH, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        final String base = Constants.REST_MEMBERS_BASE;
        final ProcessMemberResponse response;
        throwExceptionIfInvalid(request);

        switch (request.getAction()) {
            case CREATE:
                response = runRequest(ProcessMemberResponse.class, base + Constants.REST_MEMBERS_CREATE, request);
                break;
            case INVITE:
                response = runRequest(ProcessMemberResponse.class, base + Constants.REST_MEMBERS_INVITE, request);
                break;
            case LOGIN:
                response = runRequest(ProcessMemberResponse.class, base + Constants.REST_MEMBERS_LOGIN, request);
                break;
            case LOGOUT:
                response = runRequest(ProcessMemberResponse.class, base + Constants.REST_MEMBERS_LOGOUT, request);
                break;
            case ALTER:
                response = runRequest(ProcessMemberResponse.class, base + Constants.REST_MEMBERS_ALTER, request);
                break;
            case UPDATE:
                response = runRequest(ProcessMemberResponse.class, base + Constants.REST_MEMBERS_UPDATE, request);
                break;
            case INVALIDATE:
                response = runRequest(ProcessMemberResponse.class, base + Constants.REST_MEMBERS_INVALIDATE, request);
                break;
            case DELETE:
                response = runRequest(ProcessMemberResponse.class, base + Constants.REST_MEMBERS_DELETE, request);
                break;
            default:
                throw new RESTClientException(UNSUPPORTED_OPERATION + request.getAction());
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        return runRequest(FetchCircleResponse.class, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_FETCH, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        final String base = Constants.REST_CIRCLES_BASE;
        final ProcessCircleResponse response;
        throwExceptionIfInvalid(request);

        switch (request.getAction()) {
            case CREATE:
                response = runRequest(ProcessCircleResponse.class, base + Constants.REST_CIRCLES_CREATE, request);
                break;
            case UPDATE:
                response = runRequest(ProcessCircleResponse.class, base + Constants.REST_CIRCLES_UPDATE, request);
                break;
            case DELETE:
                response = runRequest(ProcessCircleResponse.class, base + Constants.REST_CIRCLES_DELETE, request);
                break;
            default:
                throw new RESTClientException(UNSUPPORTED_OPERATION + request.getAction());
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        return runRequest(FetchTrusteeResponse.class, Constants.REST_TRUSTEES_BASE + Constants.REST_TRUSTEES_FETCH, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        final String base = Constants.REST_TRUSTEES_BASE;
        final ProcessTrusteeResponse response;
        throwExceptionIfInvalid(request);

        switch (request.getAction()) {
            case ADD:
                response = runRequest(ProcessTrusteeResponse.class, base + Constants.REST_TRUSTEES_ADD, request);
                break;
            case ALTER:
                response = runRequest(ProcessTrusteeResponse.class, base + Constants.REST_TRUSTEES_ALTER, request);
                break;
            case REMOVE:
                response = runRequest(ProcessTrusteeResponse.class, base + Constants.REST_TRUSTEES_REMOVE, request);
                break;
            default:
                throw new RESTClientException(UNSUPPORTED_OPERATION + request.getAction());
        }

        return response;
    }

    private static void throwExceptionIfInvalid(final ActionRequest request) {
        if ((request == null) || (request.getAction() == null)) {
            throw new RESTClientException(INVALID_REQUEST);
        }
    }
}
