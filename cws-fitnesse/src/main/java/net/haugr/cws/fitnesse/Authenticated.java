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
package net.haugr.cws.fitnesse;

import net.haugr.cws.api.requests.Authentication;
import net.haugr.cws.api.responses.AuthenticateResponse;
import net.haugr.cws.fitnesse.callers.CallManagement;

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

        response = CallManagement.authenticated(requestUrl, request);
    }
}
