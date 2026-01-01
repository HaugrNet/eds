/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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

import net.haugr.eds.api.dtos.Circle;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.responses.FetchCircleResponse;
import java.util.List;
import net.haugr.eds.fitnesse.callers.CallManagement;

/**
 * <p>FitNesse Fixture for the EDS FetchCircles feature.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FetchCircles extends EDSRequest<FetchCircleResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String circles() {
        //noinspection DuplicatedCode
        final StringBuilder builder = new StringBuilder("[");
        if (response != null) {
            final List<Circle> circles = response.getCircles();
            addCircleInfo(builder, circles);
        }
        builder.append(']');

        return builder.toString();
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class);

        response = CallManagement.fetchCircles(requestUrl, request);
    }
}
