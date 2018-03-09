/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchCircles extends CwsRequest<FetchCircleResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String circles() {
        return getCircleNames();
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

        final FetchCircleResponse circleResponse = CallManagement.fetchCircles(request);
        setCircles(circleResponse);
        response = circleResponse;
    }
}
