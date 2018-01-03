/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.enums;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public enum HashAlgorithm {

    SHA256("SHA-256"),
    SHA384("SHA-384"),
    SHA512("SHA-512");

    // =========================================================================
    // Internal Functionality
    // =========================================================================

    private final String algorithm;

    HashAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
