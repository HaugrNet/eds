/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import io.javadog.cws.api.System;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ws.rs.core.Response;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SystemRestClient extends BaseClient implements System {

    /**
     * Constructor for the CWS System REST Client. It takes the base URL for the
     * CWS Instance to communicate with, which is the protocol, hostname, port
     * and deployment name. For example; &quot;http://localhost:8080/cws&quot;.
     *
     * @param baseURL Base URL for the CWS Instance
     */
    public SystemRestClient(final String baseURL) {
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
        final Response response =  target.request().get();
        final VersionResponse versionResponse = response.readEntity(VersionResponse.class);
        response.close();

        return versionResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingResponse settings(final SettingRequest request) {
        final Response response = runRequest("/settings", request);
        final SettingResponse cwsResponse = response.readEntity(SettingResponse.class);
        response.close();

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SanityResponse sanitized(final SanityRequest request) {
        final Response response = runRequest("/sanity/sanitized", request);
        final SanityResponse cwsResponse = response.readEntity(SanityResponse.class);
        response.close();

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        final Response response = runRequest("/members/fetch", request);
        final FetchMemberResponse cwsResponse = response.readEntity(FetchMemberResponse.class);
        response.close();

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
                    response = runRequest("/members/create", request);
                    break;
                case INVITE:
                    response = runRequest("/members/invite", request);
                    break;
                case UPDATE:
                    response = runRequest("/members/update", request);
                    break;
                case DELETE:
                    response = runRequest("/members/delete", request);
                    break;
                default:
                    throw new CWSClientException("Unsupported Operation: " + request.getAction());
            }
        } else {
            throw new CWSClientException("Cannot perform request, as the Request Object is missing or incomplete.");
        }

        final ProcessMemberResponse cwsResponse = response.readEntity(ProcessMemberResponse.class);
        response.close();

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        final Response response = runRequest("/circles/fetch", request);
        final FetchCircleResponse cwsResponse = response.readEntity(FetchCircleResponse.class);
        response.close();

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
                    response = runRequest("/circles/create", request);
                    break;
                case UPDATE:
                    response = runRequest("/circles/update", request);
                    break;
                case DELETE:
                    response = runRequest("/circles/delete", request);
                    break;
                case ADD:
                    response = runRequest("/circles/add", request);
                    break;
                case ALTER:
                    response = runRequest("/circles/alter", request);
                    break;
                case REMOVE:
                    response = runRequest("/circles/remove", request);
                    break;
                default:
                    throw new CWSClientException("Unsupported Operation: " + request.getAction());
            }
        } else {
            throw new CWSClientException("Cannot perform request, as the Request Object is missing or incomplete.");
        }

        final ProcessCircleResponse cwsResponse = response.readEntity(ProcessCircleResponse.class);
        response.close();

        return cwsResponse;
    }
}
