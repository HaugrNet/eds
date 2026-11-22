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

import java.security.SecureRandom;
import java.util.Arrays;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.core.exceptions.CryptoException;

/**
 * <p>A 96-bit AES-GCM nonce. The nonce is passed explicitly to
 * {@link Crypto#seal(SecretEDSKey, Nonce, byte[], byte[])} and
 * {@link Crypto#open(SecretEDSKey, Nonce, byte[], byte[])}, it is never
 * kept as state on a key.</p>
 *
 * <p>Random nonces are only safe as long as a key isn't used for more than
 * 2^32 encryptions (NIST SP 800-38D); EDS uses random nonces for keys that
 * wrap other keys, and per-object keys for data (PLAN.md, Crypto).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class Nonce {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] bytes;

    private Nonce(final byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Generates a new random Nonce.
     *
     * @return Random Nonce
     */
    public static Nonce random() {
        final byte[] bytes = new byte[Constants.GCM_NONCE_BYTES];
        RANDOM.nextBytes(bytes);

        return new Nonce(bytes);
    }

    /**
     * Creates a Nonce from existing bytes, which must be exactly
     * {@link Constants#GCM_NONCE_BYTES} long.
     *
     * @param bytes Nonce bytes
     * @return Nonce
     * @throws CryptoException if the length is wrong
     */
    public static Nonce of(final byte[] bytes) {
        if ((bytes == null) || (bytes.length != Constants.GCM_NONCE_BYTES)) {
            throw new CryptoException("A GCM nonce must be " + Constants.GCM_NONCE_BYTES + " bytes.");
        }

        return new Nonce(Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * Returns a copy of the Nonce bytes.
     *
     * @return Nonce bytes
     */
    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }
}
