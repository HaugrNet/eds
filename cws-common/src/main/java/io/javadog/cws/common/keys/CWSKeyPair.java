/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common.keys;

import io.javadog.cws.common.enums.KeyAlgorithm;

import java.security.KeyPair;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CWSKeyPair extends CommonKey<KeyPair> {

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    public CWSKeyPair(final KeyAlgorithm algorithm, final KeyPair key) {
        super(algorithm, key);
    }
}
