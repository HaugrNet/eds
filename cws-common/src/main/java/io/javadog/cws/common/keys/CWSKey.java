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
import java.util.Objects;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public abstract class CWSKey<T extends Key> {

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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        boolean result = false;

        if ((obj != null) && (getClass() == obj.getClass())) {
            final CWSKey<T> that = (CWSKey<T>) obj;
            result = (algorithm == that.algorithm) && Objects.equals(key, that.key);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(algorithm, key);
    }
}
