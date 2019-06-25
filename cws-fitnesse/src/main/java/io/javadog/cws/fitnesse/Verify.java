/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
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

import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.fitnesse.callers.CallShare;
import io.javadog.cws.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the CWS Verify feature.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class Verify extends CwsRequest<VerifyResponse> {

    private byte[] data = null;
    private String signature = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setData(final String data) {
        this.data = Converter.convertBytes(data);
    }

    public void setSignature(final String signature) {
        final String checked = Converter.preCheck(signature);
        if ((checked != null) && checked.contains(EXTENSION_SIGNATURE)) {
            this.signature = getSignature(signature);
        }
    }

    public String verified() {
        return response.isVerified() ? "true" : "false";
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final VerifyRequest request = prepareRequest(VerifyRequest.class);
        request.setData(data);
        request.setSignature(signature);

        response = CallShare.verify(requestType, requestUrl, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.data = null;
        this.signature = null;
    }
}
