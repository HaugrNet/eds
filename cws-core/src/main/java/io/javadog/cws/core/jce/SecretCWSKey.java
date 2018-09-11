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

import javax.crypto.SecretKey;

/**
 * <p>This is the SecretKey extension of the CWS Key, which is used for
 * symmetric encryption / decryption.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SecretCWSKey extends CWSKey<SecretKey> {

    private IVSalt salt = null;

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    public SecretCWSKey(final KeyAlgorithm algorithm, final SecretKey key) {
        super(algorithm, key);
    }

    public void setSalt(final IVSalt salt) {
        this.salt = salt;
    }

    public IVSalt getSalt() {
        return salt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecretKey getKey() {
        return key;
    }

    public void destroy() {
        if (!destroyed) {
            // Updating the flag first, so any further attempts at
            // destroying the Key will be ignored.
            destroyed = true;
            destroyKey();
        }
    }
}
