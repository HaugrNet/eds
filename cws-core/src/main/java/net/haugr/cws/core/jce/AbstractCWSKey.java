/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
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

import java.security.Key;
import net.haugr.cws.core.enums.KeyAlgorithm;

/**
 * <p>Common CWS Key, used for all crypto operations.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public abstract class AbstractCWSKey<T extends Key> {

    protected final T key;

    private final KeyAlgorithm algorithm;

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    protected AbstractCWSKey(final KeyAlgorithm algorithm, final T key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    public abstract T getKey();

    public final byte[] getEncoded() {
        return key.getEncoded();
    }

    public final KeyAlgorithm getAlgorithm() {
        return algorithm;
    }
}
