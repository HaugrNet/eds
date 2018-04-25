/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ManagementRestClient extends BaseRestClient implements Management {

    private static final String UNSUPPORTED_OPERATION = "Unsupported Operation: ";
    private static final String INVALID_REQUEST = "Cannot perform request, as the Request Object is missing or incomplete.";

    /**
     * Constructor for the CWS System REST Client. It takes the base URL for the
     * CWS Instance to communicate with, which is the protocol, hostname, port
     * and deployment name. For example; &quot;http://localhost:8080/cws&quot;.
     *
     * @param baseURL Base URL for the CWS Instance
     */
    public ManagementRestClient(final String baseURL) {
        super(baseURL);
    }

    // =========================================================================
    // Implementation of the System Interface
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionResponse version() {
        final String url = baseURL + "/version";
        final ResteasyWebTarget target = client.target(url);

        try (final Response response =  target.request().accept(MediaType.APPLICATION_XML_TYPE).get()) {
            return response.readEntity(VersionResponse.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingResponse settings(final SettingRequest request) {
        return runRequest(SettingResponse.class, "/settings", request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MasterKeyResponse masterKey(final MasterKeyRequest request) {
        return runRequest(MasterKeyResponse.class, "/masterKey", request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SanityResponse sanitized(final SanityRequest request) {
        return runRequest(SanityResponse.class, "/sanity/sanitized", request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        return runRequest(FetchMemberResponse.class, "/members/fetch", request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        final ProcessMemberResponse response;

        if ((request != null) && (request.getAction() != null)) {
            switch (request.getAction()) {
                case CREATE:
                    response = runRequest(ProcessMemberResponse.class, "/members/createMember", request);
                    break;
                case INVITE:
                    response = runRequest(ProcessMemberResponse.class, "/members/inviteMember", request);
                    break;
                case UPDATE:
                    response = runRequest(ProcessMemberResponse.class, "/members/updateMember", request);
                    break;
                case INVALIDATE:
                    response = runRequest(ProcessMemberResponse.class, "/members/invalidate", request);
                    break;
                case DELETE:
                    response = runRequest(ProcessMemberResponse.class, "/members/deleteMember", request);
                    break;
                default:
                    throw new CWSClientException(UNSUPPORTED_OPERATION + request.getAction());
            }
        } else {
            throw new CWSClientException(INVALID_REQUEST);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        return runRequest(FetchCircleResponse.class, "/circles/fetch", request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        final ProcessCircleResponse response;

        if ((request != null) && (request.getAction() != null)) {
            switch (request.getAction()) {
                case CREATE:
                    response = runRequest(ProcessCircleResponse.class, "/circles/createCircle", request);
                    break;
                case UPDATE:
                    response = runRequest(ProcessCircleResponse.class, "/circles/updateCircle", request);
                    break;
                case DELETE:
                    response = runRequest(ProcessCircleResponse.class, "/circles/deleteCircle", request);
                    break;
                default:
                    throw new CWSClientException(UNSUPPORTED_OPERATION + request.getAction());
            }
        } else {
            throw new CWSClientException(INVALID_REQUEST);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        return runRequest(FetchTrusteeResponse.class, "/trustees/fetch", request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        final ProcessTrusteeResponse response;

        if ((request != null) && (request.getAction() != null)) {
            switch (request.getAction()) {
                case ADD:
                    response = runRequest(ProcessTrusteeResponse.class, "/trustees/addTrustee", request);
                    break;
                case ALTER:
                    response = runRequest(ProcessTrusteeResponse.class, "/trustees/alterTrustee", request);
                    break;
                case REMOVE:
                    response = runRequest(ProcessTrusteeResponse.class, "/trustees/removeTrustee", request);
                    break;
                default:
                    throw new CWSClientException(UNSUPPORTED_OPERATION + request.getAction());
            }
        } else {
            throw new CWSClientException(INVALID_REQUEST);
        }

        return response;
    }
}
