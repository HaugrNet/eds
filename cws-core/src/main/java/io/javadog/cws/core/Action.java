package io.javadog.cws.core;

import io.javadog.cws.api.common.TrustLevel;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public enum Action {

    FETCH_CIRCLE(TrustLevel.GUEST, ""),
    FETCH_FOLDER(TrustLevel.GUEST, ""),
    FETCH_MEMBER(TrustLevel.GUEST, ""),
    FETCH_OBJECT(TrustLevel.GUEST, ""),
    FETCH_OBJECT_TYPE(TrustLevel.GUEST, ""),
    PROCESS_CIRCLE(TrustLevel.WRITE, ""),
    PROCESS_FOLDER(TrustLevel.WRITE, ""),
    PROCESS_MEMBER(TrustLevel.WRITE, ""),
    PROCESS_OBJECT(TrustLevel.WRITE, ""),
    PROCESS_OBJECT_TYPE(TrustLevel.WRITE, ""),
    SETTING(TrustLevel.ADMIN, "");

    // =========================================================================
    // Internal Functionality
    // =========================================================================

    private final TrustLevel trustLevel;
    private final String description;

    Action(final TrustLevel trustLevel, final String description) {
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
