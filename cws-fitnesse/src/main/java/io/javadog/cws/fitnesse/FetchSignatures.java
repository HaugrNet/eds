/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.fitnesse.callers.CallShare;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchSignatures extends CwsRequest<FetchSignatureResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String signatures() {
        return response.getSignatures().toString();
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final FetchSignatureRequest request = prepareRequest(FetchSignatureRequest.class);

        response = CallShare.fetchSignatures(request);
    }
}
