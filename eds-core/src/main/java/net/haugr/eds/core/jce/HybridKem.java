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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.KDF;
import javax.crypto.KEM;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.exceptions.CryptoException;

/**
 * <p>Hybrid key encapsulation, ML-KEM-1024 + ECDH P-384 (PLAN.md, Crypto §3), used
 * to wrap a secret (e.g. a Circle key) to a Member's public keys. Both
 * components must be broken to recover the secret.</p>
 *
 * <p>Wrapping, format version 1:</p>
 * <ol>
 *   <li>ML-KEM-1024 encapsulation to the Member's ML-KEM key gives the shared
 *   secret {@code ss_kem} and the ciphertext {@code ct_kem}.</li>
 *   <li>An ephemeral P-384 key pair and ECDH with the Member's EC key gives
 *   {@code ss_ecdh}; {@code epk} is the ephemeral public key (X.509).</li>
 *   <li>Combiner: {@code transcript = SHA-512(ct_kem | epk | pk_kem | pk_ec)}
 *   (public keys X.509), and
 *   {@code kek = HKDF-SHA512(salt = "eds/hybrid-kem/v1", ikm = ss_kem | ss_ecdh,
 *   info = "eds/hybrid-kem/v1/kek" | transcript, L = 32)}. Binding the
 *   ciphertext, the ephemeral key and the recipient keys means that neither
 *   component can be swapped or stripped on its own.</li>
 *   <li>The secret is sealed with AES-256-GCM under {@code kek}, a random
 *   nonce and the caller's AAD.</li>
 * </ol>
 *
 * <p>Output: {@code 0x01 | len(ct_kem) | ct_kem | len(epk) | epk | nonce |
 * ciphertext+tag}, lengths as 4-byte big-endian integers.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class HybridKem {

    static final byte VERSION = 1;
    static final byte[] SALT = "eds/hybrid-kem/v1".getBytes(StandardCharsets.UTF_8);
    static final byte[] KEK_LABEL = "eds/hybrid-kem/v1/kek".getBytes(StandardCharsets.UTF_8);
    private static final int KEK_BYTES = 32;

    private HybridKem() {
        // Utility class.
    }

    /**
     * Wraps the secret to the given public keys.
     *
     * @param recipient The recipient Member's public keys
     * @param aad       Additional authenticated data, binding the secret to its use
     * @param secret    The secret to wrap, e.g. a Circle key
     * @return Wrapped secret
     */
    public static byte[] wrap(final MemberKeyPair.Public recipient, final byte[] aad, final byte[] secret) {
        try {
            final KEM.Encapsulated encapsulated = KEM.getInstance(MemberKeyPair.MLKEM_ALGORITHM).newEncapsulator(recipient.mlkem()).encapsulate();
            final byte[] ctKem = encapsulated.encapsulation();
            final byte[] ssKem = encapsulated.key().getEncoded();

            final KeyPairGenerator generator = KeyPairGenerator.getInstance(MemberKeyPair.EC_ALGORITHM);
            generator.initialize(new ECGenParameterSpec(MemberKeyPair.EC_CURVE));
            final KeyPair ephemeral = generator.generateKeyPair();
            final byte[] ssEcdh = ecdh(ephemeral.getPrivate(), recipient.ec());
            final byte[] epk = ephemeral.getPublic().getEncoded();

            final SecretEDSKey kek = combine(ssKem, ssEcdh, ctKem, epk, recipient);
            final byte[] sealed = Crypto.seal(kek, aad, secret);

            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(VERSION);
            stream.writeBytes(MemberKeyPair.encode(ctKem, epk));
            stream.writeBytes(sealed);

            return stream.toByteArray();
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    /**
     * Unwraps a secret, wrapped to the given Member's keys.
     *
     * @param own     The Member's keys
     * @param aad     Additional authenticated data, as used when wrapping
     * @param wrapped Wrapped secret
     * @return The secret
     */
    public static byte[] unwrap(final MemberKeyPair own, final byte[] aad, final byte[] wrapped) {
        try {
            if ((wrapped == null) || (wrapped.length < 1) || (wrapped[0] != VERSION)) {
                throw new CryptoException("Unsupported hybrid KEM format.");
            }
            final ByteBuffer buffer = ByteBuffer.wrap(wrapped, 1, wrapped.length - 1);
            final byte[] ctKem = MemberKeyPair.readPart(buffer);
            final byte[] epk = MemberKeyPair.readPart(buffer);
            final byte[] sealed = new byte[buffer.remaining()];
            buffer.get(sealed);

            final byte[] ssKem = KEM.getInstance(MemberKeyPair.MLKEM_ALGORITHM).newDecapsulator(own.getMlkemPrivate()).decapsulate(ctKem).getEncoded();
            final PublicKey ephemeral = KeyFactory.getInstance(MemberKeyPair.EC_ALGORITHM).generatePublic(new X509EncodedKeySpec(epk));
            final byte[] ssEcdh = ecdh(own.getEcPrivate(), ephemeral);

            final SecretEDSKey kek = combine(ssKem, ssEcdh, ctKem, epk, own.getPublic());
            return Crypto.open(kek, aad, sealed);
        } catch (GeneralSecurityException | IllegalArgumentException | BufferUnderflowException e) {
            throw new CryptoException("The wrapped secret could not be unwrapped: " + e.getMessage(), e);
        }
    }

    private static byte[] ecdh(final java.security.PrivateKey privateKey, final PublicKey publicKey) throws GeneralSecurityException {
        final KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(privateKey);
        agreement.doPhase(publicKey, true);

        return agreement.generateSecret();
    }

    /**
     * The combiner, see the class documentation. Package-private, so the
     * test vectors can check it directly.
     */
    // SALT is HKDF's public domain separation label (RFC 5869), not a key.
    @SuppressWarnings("PMD.HardCodedCryptoKey")
    static SecretEDSKey combine(final byte[] ssKem, final byte[] ssEcdh, final byte[] ctKem, final byte[] epk, final MemberKeyPair.Public recipient) throws GeneralSecurityException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-512");
        digest.update(ctKem);
        digest.update(epk);
        digest.update(recipient.mlkem().getEncoded());
        digest.update(recipient.ec().getEncoded());
        final byte[] transcript = digest.digest();

        final byte[] ikm = new byte[ssKem.length + ssEcdh.length];
        System.arraycopy(ssKem, 0, ikm, 0, ssKem.length);
        System.arraycopy(ssEcdh, 0, ikm, ssKem.length, ssEcdh.length);
        final byte[] info = new byte[KEK_LABEL.length + transcript.length];
        System.arraycopy(KEK_LABEL, 0, info, 0, KEK_LABEL.length);
        System.arraycopy(transcript, 0, info, KEK_LABEL.length, transcript.length);

        final byte[] kek = KDF.getInstance("HKDF-SHA512").deriveData(HKDFParameterSpec.ofExtract()
                .addIKM(ikm)
                .addSalt(SALT)
                .thenExpand(info, KEK_BYTES));
        Arrays.fill(ikm, (byte) 0);
        Arrays.fill(ssKem, (byte) 0);
        Arrays.fill(ssEcdh, (byte) 0);

        final KeyAlgorithm algorithm = KeyAlgorithm.AES_GCM_256;
        final SecretKey key = new SecretKeySpec(kek, algorithm.getName());
        Arrays.fill(kek, (byte) 0);

        return new SecretEDSKey(algorithm, key);
    }
}
