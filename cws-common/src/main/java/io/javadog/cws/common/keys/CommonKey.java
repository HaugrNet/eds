/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common.keys;

import io.javadog.cws.common.enums.KeyAlgorithm;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
class CommonKey<T> {

    private final KeyAlgorithm algorithm;
    private final T key;

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    protected CommonKey(final KeyAlgorithm algorithm, final T key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    /**
     * {@inheritDoc}
     */
    public KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * {@inheritDoc}
     */
    public T getKey() {
        return key;
    }
}
