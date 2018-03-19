/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.fitnesse.callers.CallShare;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchDataTypes extends CwsRequest<FetchDataTypeResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String getDataTypes() {
        return response.getDataTypes().toString();
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final FetchDataTypeRequest request = prepareRequest(FetchDataTypeRequest.class);

        response = CallShare.fetchDataTypes(request);
        setDataTypes(response.getDataTypes());
    }
}
