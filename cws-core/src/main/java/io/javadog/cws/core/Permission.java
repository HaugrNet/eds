/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import io.javadog.cws.api.common.TrustLevel;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public enum Permission {

    FETCH_CIRCLE(TrustLevel.ALL, "Fetch Circles."),
    FETCH_MEMBER(TrustLevel.ALL, "Fetch Members."),
    FETCH_DATA(TrustLevel.READ, "Fetch Data."),
    FETCH_DATA_TYPE(TrustLevel.READ, "Fetch Data Types."),
    FETCH_SIGNATURES(TrustLevel.ALL, "Fetch Signatures."),
    PROCESS_CIRCLE(TrustLevel.ALL, "Process a Circle."),
    PROCESS_MEMBER(TrustLevel.WRITE, "Process a Member."),
    PROCESS_DATA(TrustLevel.WRITE, "Process Data."),
    PROCESS_DATA_TYPE(TrustLevel.ADMIN, "Process Data Type."),
    CREATE_SIGNATURE(TrustLevel.ALL, "Create Digital Signature."),
    VERIFY_SIGNATURE(TrustLevel.ALL, "Verify Digital Signature."),
    SETTING(TrustLevel.SYSOP, "Process Settings.");

    // =========================================================================
    // Internal Functionality
    // =========================================================================

    private final TrustLevel trustLevel;
    private final String description;

    Permission(final TrustLevel trustLevel, final String description) {
        this.trustLevel = trustLevel;
        this.description = description;
    }

    public TrustLevel getTrustLevel() {
        return trustLevel;
    }

    public String getDescription() {
        return description;
    }
}
