/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchTrustees extends CwsRequest<FetchTrusteeResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class);


        response = CallManagement.fetchTrustees(request);
    }
}
