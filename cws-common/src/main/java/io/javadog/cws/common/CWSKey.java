/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common;

import io.javadog.cws.common.enums.KeyAlgorithm;

import javax.crypto.spec.IvParameterSpec;
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
    private final IvParameterSpec iv;

    public CWSKey(final KeyAlgorithm algorithm, final KeyPair keyPair) {
        this.algorithm = algorithm;
        this.keyPair = keyPair;

        // rest is set to null, as this is an Asynchronous Key
        this.key = null;
        this.iv = null;
    }

    public CWSKey(final KeyAlgorithm algorithm, final Key key, final IvParameterSpec iv) {
        this.algorithm = algorithm;
        this.key = key;
        this.iv = iv;

        // rest is set to null, as this is a Synchronous Key
        this.keyPair = null;
    }

    public boolean synchronous() {
        return key != null;
    }

    public KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public PublicKey getPublicKey() {
        return (keyPair != null) ? keyPair.getPublic() : null;
    }

    public PrivateKey getPrivateKey() {
        return (keyPair != null) ? keyPair.getPrivate() : null;
    }

    public Key getKey() {
        return key;
    }

    public IvParameterSpec getIv() {
        return iv;
    }
}
