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
public final class PublicCWSKey extends CWSKey<PublicKey> {

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    public PublicCWSKey(final KeyAlgorithm algorithm, final PublicKey key) {
        super(algorithm, key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PublicKey getKey() {
        return key;
    }
}
