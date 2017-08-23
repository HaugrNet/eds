/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common.keys;

import io.javadog.cws.common.enums.KeyAlgorithm;

import java.security.PrivateKey;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class PrivateCWSKey implements CommonKey<PrivateKey> {

    private final KeyAlgorithm algorithm;
    private final PrivateKey key;

    public PrivateCWSKey(final KeyAlgorithm algorithm, final PrivateKey key) {
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
    public PrivateKey getKey() {
        return key;
    }
}
