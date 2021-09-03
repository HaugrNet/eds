/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
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
package net.haugr.cws.fitnesse;

import net.haugr.cws.api.requests.MasterKeyRequest;
import net.haugr.cws.api.responses.MasterKeyResponse;
import java.util.Map;
import net.haugr.cws.fitnesse.callers.CallManagement;
import net.haugr.cws.fitnesse.exceptions.StopTestException;
import net.haugr.cws.fitnesse.utils.Converter;

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

        response = CallManagement.masterKey(requestUrl, request);
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
