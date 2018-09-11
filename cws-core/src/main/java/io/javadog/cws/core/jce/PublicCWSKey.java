/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.core.jce;

import io.javadog.cws.core.enums.KeyAlgorithm;

import java.security.PublicKey;

/**
 * <p>This is the PublicKey extension of the CWS Key, which is used for
 * asymmetric encryption and verifying of signatures.</p>
 *
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
