/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.test.fixtures;

import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.test.CallShare;
import io.javadog.cws.test.utils.Converter;
import io.javadog.cws.test.utils.ReturnObject;

import java.util.Date;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Sign extends ReturnObject<SignResponse> {

    private byte[] data = null;
    private Date expires = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setData(final String data) {
        this.data = Converter.convertBytes(data);
    }

    public void setExpires(final String expires) {
        this.expires = Converter.convertDate(expires);
    }

    public String signature() {
        return response.getSignature();
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final SignRequest request = new SignRequest();
        request.setAccountName(accountName);
        request.setCredential(credential);
        request.setData(data);
        request.setExpires(expires);

        response = CallShare.sign(request);
    }
}
