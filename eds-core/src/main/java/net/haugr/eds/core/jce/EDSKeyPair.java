/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.jce;

import net.haugr.eds.core.enums.KeyAlgorithm;

import java.security.KeyPair;

/**
 * <p>The EDS KeyPair consists of a EDS PublicKey &amp; PrivateKey.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class EDSKeyPair {

    /** Public EDS Key. */
    private final PublicEDSKey publicKey;
    /** Private EDS Key. */
    private final PrivateEDSKey privateKey;

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    public EDSKeyPair(final KeyAlgorithm algorithm, final KeyPair key) {
        publicKey = new PublicEDSKey(algorithm, key.getPublic());
        privateKey = new PrivateEDSKey(algorithm, key.getPrivate());
    }

    /**
     * Retrieve the Public EDS Key.
     *
     * @return Public EDS Key
     */
    public PublicEDSKey getPublic() {
        return publicKey;
    }

    /**
     * Retrieve the Private EDS Key.
     *
     * @return Private EDS Key
     */
    public PrivateEDSKey getPrivate() {
        return privateKey;
    }

    /**
     * Retrieve the Algorithm.
     *
     * @return Algorithm
     */
    public KeyAlgorithm getAlgorithm() {
        return publicKey.getAlgorithm();
    }
}
