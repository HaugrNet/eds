/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.api.responses.SanityResponse;
import java.time.LocalDateTime;
import net.haugr.eds.fitnesse.callers.CallManagement;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the EDS Sanitized feature.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class Sanitized extends EDSRequest<SanityResponse> {

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
