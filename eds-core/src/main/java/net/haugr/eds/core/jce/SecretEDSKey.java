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

import javax.crypto.SecretKey;

/**
 * <p>This is the SecretKey extension of the EDS Key, which is used for
 * symmetric encryption / decryption.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class SecretEDSKey extends AbstractEDSKey<SecretKey> {

    private IVSalt salt = null;

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    public SecretEDSKey(final KeyAlgorithm algorithm, final SecretKey key) {
        super(algorithm, key);
    }

    public void setSalt(final IVSalt salt) {
        this.salt = salt;
    }

    public IVSalt getSalt() {
        return salt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecretKey getKey() {
        return key;
    }
}
