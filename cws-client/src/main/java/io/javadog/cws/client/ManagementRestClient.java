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
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
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
        final Response response =  target.request().accept(MediaType.APPLICATION_XML_TYPE).get();
        final VersionResponse versionResponse = response.readEntity(VersionResponse.class);
        close(response);

        return versionResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingResponse settings(final SettingRequest request) {
        final Response response = runRequest("/settings", request);
        final SettingResponse cwsResponse = response.readEntity(SettingResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SanityResponse sanitized(final SanityRequest request) {
        final Response response = runRequest("/sanity/sanitized", request);
        final SanityResponse cwsResponse = response.readEntity(SanityResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        final Response response = runRequest("/members/fetch", request);
        final FetchMemberResponse cwsResponse = response.readEntity(FetchMemberResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        final Response response;

        if ((request != null) && (request.getAction() != null)) {
            switch (request.getAction()) {
                case CREATE:
                    response = runRequest("/members/createMember", request);
                    break;
                case INVITE:
                    response = runRequest("/members/inviteMember", request);
                    break;
                case UPDATE:
                    response = runRequest("/members/updateMember", request);
                    break;
                case INVALIDATE:
                    response = runRequest("/members/invalidate", request);
                    break;
                case DELETE:
                    response = runRequest("/members/deleteMember", request);
                    break;
                default:
                    throw new CWSClientException("Unsupported Operation: " + request.getAction());
            }
        } else {
            throw new CWSClientException("Cannot perform request, as the Request Object is missing or incomplete.");
        }

        final ProcessMemberResponse cwsResponse = response.readEntity(ProcessMemberResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        final Response response = runRequest("/circles/fetch", request);
        final FetchCircleResponse cwsResponse = response.readEntity(FetchCircleResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        final Response response;

        if ((request != null) && (request.getAction() != null)) {
            switch (request.getAction()) {
                case CREATE:
                    response = runRequest("/circles/createCircle", request);
                    break;
                case UPDATE:
                    response = runRequest("/circles/updateCircle", request);
                    break;
                case DELETE:
                    response = runRequest("/circles/deleteCircle", request);
                    break;
                default:
                    throw new CWSClientException("Unsupported Operation: " + request.getAction());
            }
        } else {
            throw new CWSClientException("Cannot perform request, as the Request Object is missing or incomplete.");
        }

        final ProcessCircleResponse cwsResponse = response.readEntity(ProcessCircleResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        final Response response = runRequest("/trustees/fetch", request);
        final FetchTrusteeResponse cwsResponse = response.readEntity(FetchTrusteeResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        final Response response;

        if ((request != null) && (request.getAction() != null)) {
            switch (request.getAction()) {
                case ADD:
                    response = runRequest("/trustees/addTrustee", request);
                    break;
                case ALTER:
                    response = runRequest("/trustees/alterTrustee", request);
                    break;
                case REMOVE:
                    response = runRequest("/trustees/removeTrustee", request);
                    break;
                default:
                    throw new CWSClientException("Unsupported Operation: " + request.getAction());
            }
        } else {
            throw new CWSClientException("Cannot perform request, as the Request Object is missing or incomplete.");
        }

        final ProcessTrusteeResponse cwsResponse = response.readEntity(ProcessTrusteeResponse.class);
        close(response);

        return cwsResponse;
    }
}
