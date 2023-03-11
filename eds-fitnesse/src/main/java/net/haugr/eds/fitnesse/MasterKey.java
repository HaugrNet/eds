/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
package net.haugr.eds.fitnesse;

import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.responses.MasterKeyResponse;
import java.util.Map;
import net.haugr.eds.fitnesse.callers.CallManagement;
import net.haugr.eds.fitnesse.exceptions.StopTestException;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the EDS MasterKey feature.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1
 */
public final class MasterKey extends EDSRequest<MasterKeyResponse> {

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
