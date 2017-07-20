/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common;

import io.javadog.cws.common.enums.KeyAlgorithm;

import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CWSKey {

    private final KeyAlgorithm algorithm;
    private final KeyPair keyPair;
    private final Key key;
    private String salt = null;

    public CWSKey(final KeyAlgorithm algorithm, final KeyPair keyPair) {
        if ((algorithm == null) || (keyPair == null)) {
            throw new IllegalArgumentException("Missing information");
        }

        this.algorithm = algorithm;
        this.keyPair = keyPair;
        this.key = keyPair.getPrivate();
    }

    public CWSKey(final KeyAlgorithm algorithm, final Key key) {
        if ((algorithm == null) || (key == null)) {
            throw new IllegalArgumentException("Missing information");
        }

        this.algorithm = algorithm;
        this.keyPair = null;
        this.key = key;
    }

    public KeyAlgorithm.Type getType() {
        return algorithm.getType();
    }

    public KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public PublicKey getPublic() {
        final PublicKey theKey;

        if (algorithm.getType() == KeyAlgorithm.Type.ASYMMETRIC) {
            if (keyPair != null) {
                theKey = keyPair.getPublic();
            } else {
                theKey = (PublicKey) key;
            }
        } else {
            theKey = null;
        }

        return theKey;
    }

    public PrivateKey getPrivate() {
        final PrivateKey theKey;

        if (algorithm.getType() == KeyAlgorithm.Type.ASYMMETRIC) {
            theKey = (PrivateKey) key;
        } else {
            theKey = null;
        }

        return theKey;
    }

    public Key getKey() {
        return key;
    }

    public byte[] getEncoded() {
        return key.getEncoded();
    }

    public void setSalt(final String salt) {
        this.salt = salt;
    }

    public String getSalt() {
        return salt;
    }
}
