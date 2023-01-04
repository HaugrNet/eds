/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.core.jce;

import net.haugr.cws.core.enums.KeyAlgorithm;
import java.security.KeyPair;

/**
 * <p>The CWS KeyPair consists of a CWS PublicKey &amp; PrivateKey.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
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
