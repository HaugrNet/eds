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

import java.security.PrivateKey;

/**
 * <p>This is the PrivateKey extension of the CWS Key, which is used for
 * asymmetric decryption and signing.</p>
 *
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

    /**
     * <p>Destroys the Private Key.</p>
     */
    public void destroy() {
        if (!destroyed) {
            // Updating the flag first, so any further attempts at
            // destroying the Key will be ignored.
            destroyed = true;
            destroyKey();
        }
    }
}
