/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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

import net.haugr.cws.api.requests.SignRequest;
import net.haugr.cws.api.responses.SignResponse;
import java.time.LocalDateTime;
import net.haugr.cws.fitnesse.callers.CallShare;
import net.haugr.cws.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the CWS Sign feature.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class Sign extends CwsRequest<SignResponse> {

    private byte[] data = null;
    private LocalDateTime expires = null;

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
        return getSignatureKey(response.getSignature());
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final SignRequest request = prepareRequest(SignRequest.class);
        request.setData(data);
        request.setExpires(expires);

        response = CallShare.sign(requestUrl, request);
        setSignature(accountName + EXTENSION_SIGNATURE, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.data = null;
        this.expires = null;
    }
}
