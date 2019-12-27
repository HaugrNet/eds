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

import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.AuthenticateResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;

/**
 * <p>FitNesse Fixture for the CWS Authenticated feature.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
public final class Authenticated extends CwsRequest<AuthenticateResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String memberId() {
        return (response.getMemberId() != null) ? (accountName + EXTENSION_ID) : null;
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final Authentication request = prepareRequest(Authentication.class);

        response = CallManagement.authenticated(requestType, requestUrl, request);
    }
}
