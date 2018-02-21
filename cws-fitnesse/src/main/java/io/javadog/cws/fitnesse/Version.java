/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Version extends CwsRequest<VersionResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String cwsVersion() {
        return response.getVersion();
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        response = CallManagement.version();
    }
}
