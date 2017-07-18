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
    private IvParameterSpec iv = null;

    public CWSKey(final KeyAlgorithm algorithm, final KeyPair keyPair) {
        this.algorithm = algorithm;
        this.keyPair = keyPair;

        // rest is set to null, as this is an Asymmetric Key
        this.key = null;
    }

    public CWSKey(final KeyAlgorithm algorithm, final Key key, final IvParameterSpec iv) {
        this.algorithm = algorithm;
        this.key = key;
        this.iv = iv;

        // rest is set to null, as this is a Symmetric Key
        this.keyPair = null;
    }

    public KeyAlgorithm.Type getType() {
        return algorithm.getType();
    }

    public KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public PublicKey getPublic() {
        return (keyPair != null) ? keyPair.getPublic() : null;
    }

    public PrivateKey getPrivate() {
        return (keyPair != null) ? keyPair.getPrivate() : null;
    }

    public Key getKey() {
        return key;
    }

    public byte[] getEncoded() {
        return (key != null) ? key.getEncoded() : keyPair.getPrivate().getEncoded();
    }

    public void setIv(final IvParameterSpec iv) {
        this.iv = iv;
    }

    public IvParameterSpec getIv() {
        return iv;
    }
}
