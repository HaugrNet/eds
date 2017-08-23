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
public final class CWSKeyPair implements CommonKey<KeyPair> {

    private final KeyAlgorithm algorithm;
    private final KeyPair key;

    public CWSKeyPair(final KeyAlgorithm algorithm, final KeyPair key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KeyPair getKey() {
        return key;
    }
}
