/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common.keys;

import io.javadog.cws.common.enums.KeyAlgorithm;

import java.security.PublicKey;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class PublicCWSKey implements CommonKey<PublicKey> {

    private final KeyAlgorithm algorithm;
    private final PublicKey key;

    public PublicCWSKey(final KeyAlgorithm algorithm, final PublicKey key) {
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
    public PublicKey getKey() {
        return key;
    }
}
