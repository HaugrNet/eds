/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.test.fixtures;

import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.test.CallShare;
import io.javadog.cws.test.utils.Converter;
import io.javadog.cws.test.utils.ReturnObject;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Verify extends ReturnObject<VerifyResponse> {

    private byte[] data = null;
    private String signature = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setData(final String data) {
        this.data = Converter.convertBytes(data);
    }

    public void setSignature(final String signature) {
        this.signature = signature;
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
        final VerifyRequest request = new VerifyRequest();
        request.setAccountName(accountName);
        request.setCredential(credential);
        request.setData(data);
        request.setSignature(signature);

        response = CallShare.verify(request);
    }
}
