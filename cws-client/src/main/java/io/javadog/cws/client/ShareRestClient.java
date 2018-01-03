/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import io.javadog.cws.api.Share;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;

import javax.ws.rs.core.Response;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ShareRestClient extends BaseRestClient implements Share {

    /**
     * Constructor for the CWS Share REST Client. It takes the base URL for the
     * CWS Instance to communicate with, which is the protocol, hostname, port
     * and deployment name. For example; &quot;http://localhost:8080/cws&quot;.
     *
     * @param baseURL Base URL for the CWS Instance
     */
    public ShareRestClient(final String baseURL) {
        super(baseURL);
    }

    // =========================================================================
    // Implementation of the Share Interface
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataTypeResponse processDataType(final ProcessDataTypeRequest request) {
        final Response response;

        if ((request != null) && (request.getAction() != null)) {
            switch (request.getAction()) {
                case PROCESS:
                    response = runRequest("/dataTypes/process", request);
                    break;
                case DELETE:
                    response = runRequest("/dataTypes/delete", request);
                    break;
                default:
                    throw new CWSClientException("Unsupported Operation: " + request.getAction());
            }
        } else {
            throw new CWSClientException("Cannot perform request, as the Request Object is missing or incomplete.");
        }

        final ProcessDataTypeResponse cwsResponse = response.readEntity(ProcessDataTypeResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        final Response response = runRequest("/dataTypes/fetch", request);
        final FetchDataTypeResponse cwsResponse = response.readEntity(FetchDataTypeResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataResponse processData(final ProcessDataRequest request) {
        final Response response;

        if ((request != null) && (request.getAction() != null)) {
            switch (request.getAction()) {
                case ADD:
                    response = runRequest("/data/add", request);
                    break;
                case UPDATE:
                    response = runRequest("/data/update", request);
                    break;
                case DELETE:
                    response = runRequest("/data/delete", request);
                    break;
                default:
                    throw new CWSClientException("Unsupported Operation: " + request.getAction());
            }
        } else {
            throw new CWSClientException("Cannot perform request, as the Request Object is missing or incomplete.");
        }

        final ProcessDataResponse cwsResponse = response.readEntity(ProcessDataResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataResponse fetchData(final FetchDataRequest request) {
        final Response response = runRequest("/data/fetch", request);
        final FetchDataResponse cwsResponse = response.readEntity(FetchDataResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SignResponse sign(final SignRequest request) {
        final Response response = runRequest("/signatures/signDocument", request);
        final SignResponse cwsResponse = response.readEntity(SignResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyResponse verify(final VerifyRequest request) {
        final Response response = runRequest("/signatures/verifySignature", request);
        final VerifyResponse cwsResponse = response.readEntity(VerifyResponse.class);
        close(response);

        return cwsResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchSignatureResponse fetchSignatures(final FetchSignatureRequest request) {
        final Response response = runRequest("/signatures/fetch", request);
        final FetchSignatureResponse cwsResponse = response.readEntity(FetchSignatureResponse.class);
        close(response);

        return cwsResponse;
    }
}
