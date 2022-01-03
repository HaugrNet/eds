/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.client.rest;

import net.haugr.cws.api.Share;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.requests.FetchDataRequest;
import net.haugr.cws.api.requests.FetchDataTypeRequest;
import net.haugr.cws.api.requests.FetchSignatureRequest;
import net.haugr.cws.api.requests.ProcessDataRequest;
import net.haugr.cws.api.requests.ProcessDataTypeRequest;
import net.haugr.cws.api.requests.SignRequest;
import net.haugr.cws.api.requests.VerifyRequest;
import net.haugr.cws.api.responses.FetchDataResponse;
import net.haugr.cws.api.responses.FetchDataTypeResponse;
import net.haugr.cws.api.responses.FetchSignatureResponse;
import net.haugr.cws.api.responses.ProcessDataResponse;
import net.haugr.cws.api.responses.ProcessDataTypeResponse;
import net.haugr.cws.api.responses.SignResponse;
import net.haugr.cws.api.responses.VerifyResponse;

/**
 * <p>Gson based REST Client for the CWS Share functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ShareRestClient extends GsonRestClient implements Share {

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
            final String base = Constants.REST_DATATYPES_BASE;

            switch (request.getAction()) {
                case PROCESS:
                    response = runRequest(ProcessDataTypeResponse.class, base + Constants.REST_DATATYPES_PROCESS, request);
                    break;
                case DELETE:
                    response = runRequest(ProcessDataTypeResponse.class, base + Constants.REST_DATATYPES_DELETE, request);
                    break;
                default:
                    throw new RESTClientException("Unsupported Operation: " + request.getAction());
            }
        } else {
            throw new RESTClientException("Cannot perform request, as the Request Object is missing or incomplete.");
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        return runRequest(FetchDataTypeResponse.class, Constants.REST_DATATYPES_BASE + Constants.REST_DATATYPES_FETCH, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataResponse processData(final ProcessDataRequest request) {
        final ProcessDataResponse response;

        if ((request != null) && (request.getAction() != null)) {
            final String base = Constants.REST_DATA_BASE;
            switch (request.getAction()) {
                case ADD:
                    response = runRequest(ProcessDataResponse.class, base + Constants.REST_DATA_ADD, request);
                    break;
                case UPDATE:
                    response = runRequest(ProcessDataResponse.class, base + Constants.REST_DATA_UPDATE, request);
                    break;
                case COPY:
                    response = runRequest(ProcessDataResponse.class, base + Constants.REST_DATA_COPY, request);
                    break;
                case MOVE:
                    response = runRequest(ProcessDataResponse.class, base + Constants.REST_DATA_MOVE, request);
                    break;
                case DELETE:
                    response = runRequest(ProcessDataResponse.class, base + Constants.REST_DATA_DELETE, request);
                    break;
                default:
                    throw new RESTClientException("Unsupported Operation: " + request.getAction());
            }
        } else {
            throw new RESTClientException("Cannot perform request, as the Request Object is missing or incomplete.");
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataResponse fetchData(final FetchDataRequest request) {
        return runRequest(FetchDataResponse.class, Constants.REST_DATA_BASE + Constants.REST_DATA_FETCH, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SignResponse sign(final SignRequest request) {
        return runRequest(SignResponse.class, Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_SIGN, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyResponse verify(final VerifyRequest request) {
        return runRequest(VerifyResponse.class, Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_VERIFY, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchSignatureResponse fetchSignatures(final FetchSignatureRequest request) {
        return runRequest(FetchSignatureResponse.class, Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_FETCH, request);
    }
}
