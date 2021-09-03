/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
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

import net.haugr.cws.api.dtos.Circle;
import net.haugr.cws.api.requests.FetchCircleRequest;
import net.haugr.cws.api.responses.FetchCircleResponse;
import java.util.List;
import net.haugr.cws.fitnesse.callers.CallManagement;

/**
 * <p>FitNesse Fixture for the CWS FetchCircles feature.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FetchCircles extends CwsRequest<FetchCircleResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String circles() {
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
