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

import net.haugr.cws.api.requests.SanityRequest;
import net.haugr.cws.api.responses.SanityResponse;
import java.time.LocalDateTime;
import net.haugr.cws.fitnesse.callers.CallManagement;
import net.haugr.cws.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the CWS Sanitized feature.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class Sanitized extends CwsRequest<SanityResponse> {

    private String circleId = null;
    private LocalDateTime since = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setCircleId(final String circleId) {
        this.circleId = Converter.preCheck(circleId);
    }

    public void setSince(final String since) {
        this.since = Converter.convertDate(since);
    }

    public String failures() {
        return response.getSanities().toString();
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final SanityRequest request = prepareRequest(SanityRequest.class);
        request.setCircleId(circleId);
        request.setSince(since);

        response = CallManagement.sanitized(requestUrl, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.circleId = null;
        this.since = null;
    }
}
