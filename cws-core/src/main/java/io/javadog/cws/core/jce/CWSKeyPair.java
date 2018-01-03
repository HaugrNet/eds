/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.jce;

import io.javadog.cws.core.enums.KeyAlgorithm;

import java.security.KeyPair;

/**
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
