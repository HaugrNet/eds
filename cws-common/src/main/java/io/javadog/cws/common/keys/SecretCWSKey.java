/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common.keys;

import io.javadog.cws.common.enums.KeyAlgorithm;

import javax.crypto.SecretKey;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SecretCWSKey implements CommonKey<SecretKey> {

    private final KeyAlgorithm algorithm;
    private final SecretKey key;
    private String salt = null;

    public SecretCWSKey(final KeyAlgorithm algorithm, final SecretKey key) {
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
    public SecretKey getKey() {
        return key;
    }

    public void setSalt(final String salt) {
        this.salt = salt;
    }

    public String getSalt() {
        return salt;
    }
}
