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
    FETCH_FOLDER(TrustLevel.GUEST, "Fetch Folders."),
    FETCH_MEMBER(TrustLevel.GUEST, "Fetch Members."),
    FETCH_OBJECT(TrustLevel.READ, "Fetch Object."),
    FETCH_OBJECT_TYPE(TrustLevel.GUEST, "Fetch Object Types."),
    PROCESS_CIRCLE(TrustLevel.WRITE, "Process Circle."),
    PROCESS_FOLDER(TrustLevel.WRITE, "Process Folder."),
    PROCESS_MEMBER(TrustLevel.WRITE, "Process Member."),
    PROCESS_OBJECT(TrustLevel.WRITE, "Process Object."),
    PROCESS_OBJECT_TYPE(TrustLevel.WRITE, "Process Object Type."),
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
