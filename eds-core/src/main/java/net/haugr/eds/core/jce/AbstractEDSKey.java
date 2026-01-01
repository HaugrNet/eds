/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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

import java.security.Key;
import net.haugr.eds.core.enums.KeyAlgorithm;

/**
 * <p>Common EDS Key, used for all crypto operations.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public abstract class AbstractEDSKey<T extends Key> {

    /** Key. */
    protected final T key;

    private final KeyAlgorithm algorithm;

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    protected AbstractEDSKey(final KeyAlgorithm algorithm, final T key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    /**
     * Retrieves the Key.
     *
     * @return Key
     */
    public abstract T getKey();

    /**
     * Retrieves the Encoded Bytes.
     *
     * @return Encoded Bytes
     */
    public final byte[] getEncoded() {
        return key.getEncoded();
    }

    /**
     * Retrieves the Algorithm.
     *
     * @return Algorithm
     */
    public final KeyAlgorithm getAlgorithm() {
        return algorithm;
    }
}
