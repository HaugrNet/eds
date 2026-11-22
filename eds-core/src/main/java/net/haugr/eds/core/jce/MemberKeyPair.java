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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.exceptions.CryptoException;

/**
 * <p>The asymmetric keys of a Member (PLAN.md, Crypto §2): an ML-KEM-1024 key pair
 * and an ECDH P-384 key pair, used together by the {@link HybridKem}. A
 * Member's public keys are stored armored; the private keys are stored double
 * wrapped (password key inside, MasterKey outside).</p>
 *
 * <p>Encoding: the public keys are X.509 and the private keys PKCS#8, each
 * prefixed with its length as a 4-byte big-endian integer, ML-KEM first.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class MemberKeyPair {

    static final String MLKEM_ALGORITHM = "ML-KEM";
    static final String MLKEM_PARAMETERS = "ML-KEM-1024";
    static final String EC_ALGORITHM = "EC";
    static final String EC_CURVE = "secp384r1";

    /**
     * The public keys of a Member.
     *
     * @param algorithm Member Key Algorithm
     * @param mlkem     ML-KEM-1024 public key
     * @param ec        ECDH P-384 public key
     */
    public record Public(KeyAlgorithm algorithm, PublicKey mlkem, PublicKey ec) {

        /**
         * Returns the public keys as Base64 encoded text, for storage.
         *
         * @return Armored public keys
         */
        public String armored() {
            return Base64.getEncoder().encodeToString(encode(mlkem.getEncoded(), ec.getEncoded()));
        }

        /**
         * Reads armored public keys, see {@link #armored()}.
         *
         * @param algorithm Member Key Algorithm
         * @param armored   Armored public keys
         * @return Public keys
         */
        public static Public fromArmored(final KeyAlgorithm algorithm, final String armored) {
            try {
                final byte[][] parts = decode(Base64.getDecoder().decode(armored));
                final PublicKey mlkem = KeyFactory.getInstance(MLKEM_ALGORITHM).generatePublic(new X509EncodedKeySpec(parts[0]));
                final PublicKey ec = KeyFactory.getInstance(EC_ALGORITHM).generatePublic(new X509EncodedKeySpec(parts[1]));

                return new Public(checkAlgorithm(algorithm), mlkem, ec);
            } catch (GeneralSecurityException | IllegalArgumentException e) {
                throw new CryptoException("The public keys could not be read: " + e.getMessage(), e);
            }
        }
    }

    private final Public publicKeys;
    private final PrivateKey mlkem;
    private final PrivateKey ec;

    private MemberKeyPair(final Public publicKeys, final PrivateKey mlkem, final PrivateKey ec) {
        this.publicKeys = publicKeys;
        this.mlkem = mlkem;
        this.ec = ec;
    }

    /**
     * Generates new Member keys.
     *
     * @param algorithm Member Key Algorithm, must be {@link KeyAlgorithm#MLKEM1024_P384}
     * @return New Member keys
     */
    public static MemberKeyPair generate(final KeyAlgorithm algorithm) {
        try {
            final KeyPair mlkemPair = KeyPairGenerator.getInstance(MLKEM_PARAMETERS).generateKeyPair();
            final KeyPairGenerator ecGenerator = KeyPairGenerator.getInstance(EC_ALGORITHM);
            ecGenerator.initialize(new ECGenParameterSpec(EC_CURVE));
            final KeyPair ecPair = ecGenerator.generateKeyPair();

            return new MemberKeyPair(new Public(checkAlgorithm(algorithm), mlkemPair.getPublic(), ecPair.getPublic()), mlkemPair.getPrivate(), ecPair.getPrivate());
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    /**
     * Rebuilds the Member keys from the public keys and the decrypted
     * private key bytes, see {@link #privateBytes()}.
     *
     * @param publicKeys   Public keys
     * @param privateBytes Encoded private keys
     * @return Member keys
     */
    public static MemberKeyPair of(final Public publicKeys, final byte[] privateBytes) {
        try {
            final byte[][] parts = decode(privateBytes);
            final PrivateKey mlkem = KeyFactory.getInstance(MLKEM_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(parts[0]));
            final PrivateKey ec = KeyFactory.getInstance(EC_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(parts[1]));

            return new MemberKeyPair(publicKeys, mlkem, ec);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new CryptoException("The private keys could not be read: " + e.getMessage(), e);
        }
    }

    public Public getPublic() {
        return publicKeys;
    }

    public KeyAlgorithm getAlgorithm() {
        return publicKeys.algorithm();
    }

    PrivateKey getMlkemPrivate() {
        return mlkem;
    }

    PrivateKey getEcPrivate() {
        return ec;
    }

    /**
     * Returns the encoded private keys. The caller must protect and clear
     * the result.
     *
     * @return Encoded private keys
     */
    public byte[] privateBytes() {
        return encode(mlkem.getEncoded(), ec.getEncoded());
    }

    private static KeyAlgorithm checkAlgorithm(final KeyAlgorithm algorithm) {
        if (algorithm != KeyAlgorithm.MLKEM1024_P384) {
            throw new CryptoException("Unsupported Member Key Algorithm " + algorithm + '.');
        }

        return algorithm;
    }

    static byte[] encode(final byte[]... parts) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (final byte[] part : parts) {
            stream.writeBytes(ByteBuffer.allocate(Integer.BYTES).putInt(part.length).array());
            stream.writeBytes(part);
        }

        return stream.toByteArray();
    }

    static byte[][] decode(final byte[] encoded) {
        try {
            final ByteBuffer buffer = ByteBuffer.wrap(encoded);
            final byte[] first = readPart(buffer);
            final byte[] second = readPart(buffer);
            requireEnd(buffer);

            return new byte[][] { first, second };
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated key encoding.", e);
        }
    }

    private static void requireEnd(final ByteBuffer buffer) {
        if (buffer.hasRemaining()) {
            throw new IllegalArgumentException("Unexpected trailing bytes.");
        }
    }

    static byte[] readPart(final ByteBuffer buffer) {
        final int length = buffer.getInt();
        if ((length < 0) || (length > buffer.remaining())) {
            throw new IllegalArgumentException("Invalid length in key encoding.");
        }
        final byte[] part = new byte[length];
        buffer.get(part);

        return part;
    }
}
