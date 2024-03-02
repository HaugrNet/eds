/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

import net.haugr.eds.api.Share;
import net.haugr.eds.api.common.Constants;
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

/**
 * <p>Gson based REST Client for the EDS Share functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ShareRestClient extends GsonRestClient implements Share {

    /**
     * Constructor for the EDS Share REST Client. It takes the base URL for the
     * EDS Instance to communicate with, which is the protocol, hostname, port
     * and deployment name. For example; &quot;localhost:8080/eds&quot;.
     *
     * @param baseURL Base URL for the EDS Instance
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

            response = switch (request.getAction()) {
                case PROCESS -> runRequest(ProcessDataTypeResponse.class, base + Constants.REST_DATATYPES_PROCESS, request);
                case DELETE -> runRequest(ProcessDataTypeResponse.class, base + Constants.REST_DATATYPES_DELETE, request);
                default -> throw new RESTClientException("Unsupported Operation: " + request.getAction());
            };
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
            response = switch (request.getAction()) {
                case ADD -> runRequest(ProcessDataResponse.class, base + Constants.REST_DATA_ADD, request);
                case UPDATE -> runRequest(ProcessDataResponse.class, base + Constants.REST_DATA_UPDATE, request);
                case COPY -> runRequest(ProcessDataResponse.class, base + Constants.REST_DATA_COPY, request);
                case MOVE -> runRequest(ProcessDataResponse.class, base + Constants.REST_DATA_MOVE, request);
                case DELETE -> runRequest(ProcessDataResponse.class, base + Constants.REST_DATA_DELETE, request);
                default -> throw new RESTClientException("Unsupported Operation: " + request.getAction());
            };
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
