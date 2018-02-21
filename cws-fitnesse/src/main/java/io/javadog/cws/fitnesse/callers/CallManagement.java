/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse.callers;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.client.ManagementSoapClient;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CallManagement {

    private static final Management MANAGEMENT = new ManagementSoapClient("http://localhost:8080/cws");

    // =========================================================================
    // Management Interface Functionality
    // =========================================================================

    public static VersionResponse version() {
        return MANAGEMENT.version();
    }
}
