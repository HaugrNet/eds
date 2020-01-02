/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
 * mailto: cws AT JavaDog DOT io
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
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;
import io.javadog.cws.fitnesse.exceptions.StopTestException;
import io.javadog.cws.fitnesse.utils.Converter;
import java.util.Map;

/**
 * <p>FitNesse Fixture for the CWS MasterKey feature.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
public final class MasterKey extends CwsRequest<MasterKeyResponse> {

    private byte[] secret = null;
    private String url = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setSecret(final String secret) {
        this.secret = Converter.convertBytes(secret);
    }

    public void setUrl(final String url) {
        this.url = Converter.preCheck(url);
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class);
        request.setSecret(secret);
        request.setUrl(url);
        final Map<String, String> errors = request.validate();
        if (!errors.isEmpty()) {
            throw new StopTestException(errors.toString());
        }

        response = CallManagement.masterKey(requestType, requestUrl, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.secret = null;
        this.url = null;
    }
}
