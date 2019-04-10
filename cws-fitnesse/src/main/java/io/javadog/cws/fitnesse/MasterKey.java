/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2019 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;
import io.javadog.cws.fitnesse.utils.Converter;

/**
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
