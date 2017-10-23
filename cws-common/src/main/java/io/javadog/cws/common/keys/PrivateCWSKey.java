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
public final class PrivateCWSKey extends CWSKey<PrivateKey> {

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    public PrivateCWSKey(final KeyAlgorithm algorithm, final PrivateKey key) {
        super(algorithm, key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrivateKey getKey() {
        return key;
    }
}
