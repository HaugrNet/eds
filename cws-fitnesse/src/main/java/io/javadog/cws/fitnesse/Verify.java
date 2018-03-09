/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.fitnesse.callers.CallShare;
import io.javadog.cws.fitnesse.utils.Converter;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
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
        final VerifyRequest request = prepareRequest(VerifyRequest.class);
        request.setData(data);
        request.setSignature(signature);

        response = CallShare.verify(request);
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
