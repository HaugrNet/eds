/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common.keys;

import io.javadog.cws.common.enums.KeyAlgorithm;

import java.security.Key;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class CWSKey<T extends Key> {

    protected boolean destroyed = false;
    private final KeyAlgorithm algorithm;
    protected final T key;

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    protected CWSKey(final KeyAlgorithm algorithm, final T key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    public abstract T getKey();

    public byte[] getEncoded() {
        return key.getEncoded();
    }

    public KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
