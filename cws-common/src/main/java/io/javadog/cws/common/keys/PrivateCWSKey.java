/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common.keys;

import io.javadog.cws.common.enums.KeyAlgorithm;
import io.javadog.cws.common.exceptions.CryptoException;

import javax.security.auth.DestroyFailedException;
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

    /**
     * <p>Destroys the Private Key.</p>
     */
    public void destroy() {
        if (!destroyed) {
            try {
                // Updating the flag first, so any further attempts at
                // destroying the Key will be ignored.
                destroyed = true;
                key.destroy();
            } catch (DestroyFailedException e) {
                throw new CryptoException(e.getMessage(), e);
            }
        }
    }
}
