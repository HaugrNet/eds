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
        final ProcessDataTypeResponse response;

        if ((request != null) && (request.getAction() != null)) {
            switch (request.getAction()) {
                case PROCESS:
                    response = runRequest(ProcessDataTypeResponse.class, "/dataTypes/process", request);
                    break;
                case DELETE:
                    response = runRequest(ProcessDataTypeResponse.class, "/dataTypes/delete", request);
                    break;
                default:
                    throw new CWSClientException("Unsupported Operation: " + request.getAction());
            }
        } else {
            throw new CWSClientException("Cannot perform request, as the Request Object is missing or incomplete.");
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        return runRequest(FetchDataTypeResponse.class, "/dataTypes/fetch", request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataResponse processData(final ProcessDataRequest request) {
        final ProcessDataResponse response;

        if ((request != null) && (request.getAction() != null)) {
            switch (request.getAction()) {
                case ADD:
                    response = runRequest(ProcessDataResponse.class, "/data/add", request);
                    break;
                case UPDATE:
                    response = runRequest(ProcessDataResponse.class, "/data/update", request);
                    break;
                case DELETE:
                    response = runRequest(ProcessDataResponse.class, "/data/delete", request);
                    break;
                default:
                    throw new CWSClientException("Unsupported Operation: " + request.getAction());
            }
        } else {
            throw new CWSClientException("Cannot perform request, as the Request Object is missing or incomplete.");
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataResponse fetchData(final FetchDataRequest request) {
        return runRequest(FetchDataResponse.class, "/data/fetch", request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SignResponse sign(final SignRequest request) {
        return runRequest(SignResponse.class, "/signatures/signDocument", request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyResponse verify(final VerifyRequest request) {
        return runRequest(VerifyResponse.class, "/signatures/verifySignature", request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchSignatureResponse fetchSignatures(final FetchSignatureRequest request) {
        return runRequest(FetchSignatureResponse.class, "/signatures/fetch", request);
    }
}
