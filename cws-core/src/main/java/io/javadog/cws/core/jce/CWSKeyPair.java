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

import java.security.KeyPair;

/**
 * <p>The CWS KeyPair consists of a CWS PublicKey &amp; PrivateKey.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CWSKeyPair {

    private final PublicCWSKey publicKey;
    private final PrivateCWSKey privateKey;

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    public CWSKeyPair(final KeyAlgorithm algorithm, final KeyPair key) {
        publicKey = new PublicCWSKey(algorithm, key.getPublic());
        privateKey = new PrivateCWSKey(algorithm, key.getPrivate());
    }

    public PublicCWSKey getPublic() {
        return publicKey;
    }

    public PrivateCWSKey getPrivate() {
        return privateKey;
    }

    public KeyAlgorithm getAlgorithm() {
        return publicKey.getAlgorithm();
    }
}
