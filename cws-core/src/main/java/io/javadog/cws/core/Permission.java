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

    FETCH_CIRCLE(TrustLevel.GUEST, "Fetch Circles."),
    FETCH_MEMBER(TrustLevel.GUEST, "Fetch Members."),
    FETCH_DATA(TrustLevel.READ, "Fetch Data."),
    FETCH_DATA_TYPE(TrustLevel.GUEST, "Fetch Data Types."),
    PROCESS_CIRCLE(TrustLevel.WRITE, "Process Circle."),
    PROCESS_MEMBER(TrustLevel.WRITE, "Process Member."),
    PROCESS_DATA(TrustLevel.WRITE, "Process Data."),
    PROCESS_DATA_TYPE(TrustLevel.WRITE, "Process Data Type."),
    CREATE_SIGNATURE(TrustLevel.WRITE, "Create Digital Signature."),
    VERIFY_SIGNATURE(TrustLevel.WRITE, "Verify Digital Signature."),
    SETTING(TrustLevel.ADMIN, "Settings.");

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
