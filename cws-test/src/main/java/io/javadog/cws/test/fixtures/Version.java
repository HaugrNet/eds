/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.test.fixtures;

import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.test.CallManagement;
import io.javadog.cws.test.utils.ReturnObject;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Version extends ReturnObject<VersionResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String version() {
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
