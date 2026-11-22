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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.LongAdder;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KDF;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.exceptions.CryptoException;
import net.haugr.eds.core.model.Settings;

/**
 * <p>This library contains all the Cryptographic Operations, needed for EDS to
 * support the features required. JCA, Java Cryptography Architecture, contains
 * all the features needed and is flexible enough that it can be extended by
 * providing different vendors - which will then allow using stronger encryption
 * if needed.</p>
 *
 * <p>EDS uses two (three) types of Encryption. Symmetric Encryption of all the
 * actual Data to be shared and Asymmetric Encryption to storing the Symmetric
 * keys. Additionally, all Private Keys are stored encrypted, and a Key is
 * derived (using PBKDF2) from the Credentials to unlock it.</p>
 *
 * <p>The default Algorithms and Key sizes have been chosen, so they will work
 * with a standard Java 8 (build 161+) installation, these uses the maximum key
 * size allowed, if an older Java is used, then either install the Java 8
 * Security extension from Oracle or change the default configuration of EDS
 * accordingly.</p>
 *
 * <p>Although Cryptography is the cornerstone of the EDS, there are no attempts
 * made towards creating or inventing various Algorithms. The risk of making
 * mistakes is too high. Instead, the EDS relies on the wisdom and maturity of
 * existing JCE implementations in Java.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class Crypto {

    private static final String HMAC_ALGORITHM = "HmacSHA512";
    private static final String HKDF_ALGORITHM = "HKDF-SHA512";
    private static final int DERIVED_KEY_BYTES = 32;
    // Counts the PBKDF2 derivations, so tests can prove that requests with a
    // Session don't run PBKDF2 (PLAN.md, C3).
    private static final LongAdder PASSWORD_KEY_DERIVATIONS = new LongAdder();
    private static final byte LABEL_SEPARATOR = 0;

    private final MasterKey masterKey;
    private final Settings settings;

    public Crypto(final Settings settings) {
        masterKey = MasterKey.getInstance();
        this.settings = settings;
    }

    // =========================================================================
    // Public Methods to generate Keys
    // =========================================================================

    /**
     * <p>Converts the given Salted Password to a Key, which can be used for the
     * initial Cryptographic Operations. With the help of the PBKDF2 algorithm,
     * it creates a symmetric Key over 'n' iterations, where 'n' is configurable
     * as it may be required to have stronger checks. However, for the Key to be
     * of a good enough Quality, it should be having a length of at least 16
     * characters and the same applies to the Salt.</p>
     *
     * @param algorithm PBE Algorithm to generate Account Symmetric Key
     * @param secret    Provided Passphrase or Secret
     * @param salt       Member-specific random Salt
     * @param iterations PBKDF2 iterations: those of the Member's account, or
     *                   the current setting for a new passphrase
     * @return Symmetric Key
     * @throws CryptoException if an error occurred
     */
    // Part of the instance API of Crypto, which every caller holds; made
    // static, each call through an instance would be a finding instead.
    @SuppressWarnings("java:S2325")
    public SecretEDSKey generatePasswordKey(final KeyAlgorithm algorithm, final byte[] secret, final byte[] salt, final int iterations) {
        try {
            PASSWORD_KEY_DERIVATIONS.increment();
            final char[] extendedSecret = generateSecretChars(secret);

            final SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm.getTransformationValue());
            final KeySpec spec = new PBEKeySpec(extendedSecret, salt, iterations, algorithm.getLength());
            Arrays.fill(extendedSecret, (char) 0);
            final SecretKey tmpKey = factory.generateSecret(spec);
            final SecretKey secretKey = new SecretKeySpec(tmpKey.getEncoded(), algorithm.getName());

            return new SecretEDSKey(algorithm.getDerived(), secretKey);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    /**
     * The secret as chars for PBKDF2, one char per byte, without going over a
     * String, which couldn't be wiped afterwards.
     */
    private static char[] generateSecretChars(final byte[] secret) {
        final char[] chars = new char[secret.length];
        for (int i = 0; i < secret.length; i++) {
            chars[i] = (char) secret[i];
        }

        return chars;
    }

    /**
     * Returns the number of PBKDF2 derivations made since the start.
     *
     * @return Number of PBKDF2 derivations
     */
    public static long passwordKeyDerivations() {
        return PASSWORD_KEY_DERIVATIONS.sum();
    }

    public static SecretEDSKey generateSymmetricKey(final KeyAlgorithm algorithm) {
        try {
            final KeyGenerator generator = KeyGenerator.getInstance(algorithm.getName(), algorithm.getProvider());
            generator.init(algorithm.getLength());
            final SecretKey key = generator.generateKey();

            return new SecretEDSKey(algorithm, key);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public String generateChecksum(final byte[] bytes) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(settings.getHashAlgorithm().getAlgorithm());
            final byte[] hashed = digest.digest(bytes);

            return Base64.getEncoder().encodeToString(hashed);
        } catch (IllegalArgumentException | NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    // =========================================================================
    // Standard Cryptographic Operations; HMAC, Encrypt & Decrypt
    // =========================================================================

    /**
     * <p>Generates a keyed lookup token, HMAC-SHA512, for the given value.
     * The HMAC key is the MasterKey's {@value MasterKey#HMAC_LABEL} subkey,
     * and the label separates the purposes, so a token for one purpose can't
     * be used for another. Used for values which must be found again, but not
     * be readable from the database, such as invitation tokens and sessions.</p>
     *
     * <p>As the key is derived from the MasterKey, all tokens become void if
     * the MasterKey changes.</p>
     *
     * @param label Purpose of the token, e.g. "eds/invitation"
     * @param value The value to generate the token for
     * @return Base64 encoded HMAC-SHA512 of label and value
     */
    public String hmacWithMasterKey(final String label, final byte[] value) {
        return Base64.getEncoder().encodeToString(hmacBytesWithMasterKey(label, value));
    }

    /**
     * Same as {@link #hmacWithMasterKey(String, byte[])}, returning the raw
     * HMAC bytes.
     *
     * @param label Purpose of the token
     * @param value The value to generate the token for
     * @return HMAC-SHA512 of label and value
     */
    public byte[] hmacBytesWithMasterKey(final String label, final byte[] value) {
        try {
            final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(masterKey.getHmacKey(), HMAC_ALGORITHM));
            mac.update(stringToBytes(label));
            mac.update(LABEL_SEPARATOR);

            return mac.doFinal(value);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    /**
     * <p>Encrypts the value with the MasterKey wrap key, using a random nonce
     * and the given AAD extended with the MasterKey version. Two encryptions
     * of the same value never give the same result.</p>
     *
     * @param aad   Additional authenticated data, binding the value to its use
     * @param value The value to protect
     * @return Base64 encoded nonce and ciphertext
     */
    public String sealWithMasterKey(final String aad, final byte[] value) {
        final MasterKey.Installed current = masterKey.current();
        final byte[] sealed = seal(current.material().wrapKey(), masterKeyAad(aad, current.version()), value);
        return Base64.getEncoder().encodeToString(sealed);
    }

    /**
     * Reverses {@link #sealWithMasterKey(String, byte[])}. Fails if the AAD,
     * the MasterKey or its version differ.
     *
     * @param aad    Additional authenticated data, as given when sealing
     * @param sealed Base64 encoded nonce and ciphertext
     * @return The protected value
     */
    public byte[] openWithMasterKey(final String aad, final String sealed) {
        try {
            final MasterKey.Installed current = masterKey.current();
            return open(current.material().wrapKey(), masterKeyAad(aad, current.version()), Base64.getDecoder().decode(sealed));
        } catch (IllegalArgumentException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    /**
     * Re-wraps a value from the current MasterKey to a new one, for a
     * MasterKey rotation (PLAN.md, Crypto §1). The value itself is not exposed.
     *
     * @param aad        AAD of the value
     * @param sealed     Value, sealed with the current MasterKey
     * @param newKey     The new MasterKey
     * @param newVersion The version of the new MasterKey
     * @return The value, sealed with the new MasterKey
     */
    public String rewrapWithMasterKey(final String aad, final String sealed, final MasterKey.Material newKey, final int newVersion) {
        final byte[] value = openWithMasterKey(aad, sealed);
        final byte[] resealed = seal(newKey.wrapKey(), masterKeyAad(aad, newVersion), value);
        Arrays.fill(value, (byte) 0);

        return Base64.getEncoder().encodeToString(resealed);
    }

    private byte[] masterKeyAad(final String aad, final int version) {
        return stringToBytes(aad + "|mk" + version + '|' + KeyAlgorithm.AES_GCM_256.name());
    }

    /**
     * <p>AES-GCM encryption with an explicit nonce and AAD. The caller is
     * responsible for never reusing a nonce with the same key.</p>
     *
     * @param key   Symmetric Key
     * @param nonce 96-bit nonce
     * @param aad   Additional authenticated data, may be empty
     * @param plain Plaintext
     * @return Ciphertext with the authentication tag appended
     */
    public static byte[] seal(final SecretEDSKey key, final Nonce nonce, final byte[] aad, final byte[] plain) {
        try {
            final Cipher cipher = prepareGcmCipher(key, nonce, Cipher.ENCRYPT_MODE);
            cipher.updateAAD(aad);
            return cipher.doFinal(plain);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    /**
     * Reverses {@link #seal(SecretEDSKey, Nonce, byte[], byte[])}, and
     * verifies the authentication tag.
     *
     * @param key        Symmetric Key
     * @param nonce      96-bit nonce, as used when sealing
     * @param aad        Additional authenticated data, as used when sealing
     * @param ciphertext Ciphertext with the authentication tag
     * @return Plaintext
     */
    public static byte[] open(final SecretEDSKey key, final Nonce nonce, final byte[] aad, final byte[] ciphertext) {
        try {
            final Cipher cipher = prepareGcmCipher(key, nonce, Cipher.DECRYPT_MODE);
            cipher.updateAAD(aad);
            return cipher.doFinal(ciphertext);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    /**
     * Seals with a random nonce, which is prepended to the result.
     *
     * @param key   Symmetric Key
     * @param aad   Additional authenticated data
     * @param plain Plaintext
     * @return Nonce followed by ciphertext and tag
     */
    public static byte[] seal(final SecretEDSKey key, final byte[] aad, final byte[] plain) {
        final Nonce nonce = Nonce.random();
        final byte[] ciphertext = seal(key, nonce, aad, plain);
        final byte[] result = new byte[Constants.GCM_NONCE_BYTES + ciphertext.length];
        System.arraycopy(nonce.getBytes(), 0, result, 0, Constants.GCM_NONCE_BYTES);
        System.arraycopy(ciphertext, 0, result, Constants.GCM_NONCE_BYTES, ciphertext.length);

        return result;
    }

    /**
     * Reverses {@link #seal(SecretEDSKey, byte[], byte[])}.
     *
     * @param key    Symmetric Key
     * @param aad    Additional authenticated data
     * @param sealed Nonce followed by ciphertext and tag
     * @return Plaintext
     */
    public static byte[] open(final SecretEDSKey key, final byte[] aad, final byte[] sealed) {
        if ((sealed == null) || (sealed.length < Constants.GCM_NONCE_BYTES)) {
            throw new CryptoException("The sealed value is too short.");
        }
        final Nonce nonce = Nonce.of(Arrays.copyOf(sealed, Constants.GCM_NONCE_BYTES));
        final byte[] ciphertext = Arrays.copyOfRange(sealed, Constants.GCM_NONCE_BYTES, sealed.length);

        return open(key, nonce, aad, ciphertext);
    }

    private static Cipher prepareGcmCipher(final SecretEDSKey key, final Nonce nonce, final int mode)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        if (key.getAlgorithm().getType() != KeyAlgorithm.Type.SYMMETRIC) {
            throw new CryptoException("Cannot prepare Cipher for this Algorithm Type " + key.getAlgorithm().getType() + '.');
        }

        final Cipher cipher = Cipher.getInstance(key.getAlgorithm().getTransformationValue());
        cipher.init(mode, key.getKey(), new GCMParameterSpec(Constants.GCM_TAG_BITS, nonce.getBytes()));

        return cipher;
    }

    // =========================================================================
    // Member Keys & Circle Keys
    // =========================================================================

    /**
     * <p>Protects the Member's private keys with two layers (PLAN.md, Crypto §2):
     * the inner layer with the given key (password key or session key), the
     * outer layer with the MasterKey. Without the MasterKey, a copy of the
     * database gives nothing to test password guesses against.</p>
     *
     * @param innerKey Password or Session Key
     * @param innerAad AAD of the inner layer
     * @param outerAad AAD of the outer (MasterKey) layer
     * @param keyPair  The Member's keys
     * @return Armored, double wrapped private keys
     */
    public String protectPrivateKeys(final SecretEDSKey innerKey, final byte[] innerAad, final String outerAad, final MemberKeyPair keyPair) {
        final byte[] privateBytes = keyPair.privateBytes();
        final byte[] inner = seal(innerKey, innerAad, privateBytes);
        Arrays.fill(privateBytes, (byte) 0);

        return sealWithMasterKey(outerAad, inner);
    }

    /**
     * Reverses {@link #protectPrivateKeys(SecretEDSKey, byte[], String, MemberKeyPair)}.
     *
     * @param innerKey   Password or Session Key
     * @param innerAad   AAD of the inner layer
     * @param outerAad   AAD of the outer (MasterKey) layer
     * @param publicKeys The Member's public keys
     * @param protectedKeys Armored, double wrapped private keys
     * @return The Member's keys
     */
    public MemberKeyPair openPrivateKeys(final SecretEDSKey innerKey, final byte[] innerAad, final String outerAad, final MemberKeyPair.Public publicKeys, final String protectedKeys) {
        final byte[] inner = openWithMasterKey(outerAad, protectedKeys);
        final byte[] privateBytes = open(innerKey, innerAad, inner);
        final MemberKeyPair keyPair = MemberKeyPair.of(publicKeys, privateBytes);
        Arrays.fill(privateBytes, (byte) 0);

        return keyPair;
    }

    /**
     * Wraps a Circle key to a Member's public keys, using the hybrid KEM.
     *
     * @param recipient The Member's public keys
     * @param aad       AAD, binding the Circle key to its use
     * @param circleKey The Circle key
     * @return Armored, wrapped Circle key
     */
    public static String wrapCircleKey(final MemberKeyPair.Public recipient, final byte[] aad, final SecretEDSKey circleKey) {
        final byte[] encoded = circleKey.getEncoded();
        final byte[] wrapped = HybridKem.wrap(recipient, aad, encoded);
        Arrays.fill(encoded, (byte) 0);

        return Base64.getEncoder().encodeToString(wrapped);
    }

    /**
     * Unwraps a Circle key, wrapped to the Member's keys.
     *
     * @param algorithm        Algorithm of the Circle key
     * @param own              The Member's keys
     * @param aad              AAD, as used when wrapping
     * @param armoredCircleKey Armored, wrapped Circle key
     * @return The Circle key
     */
    public static SecretEDSKey unwrapCircleKey(final KeyAlgorithm algorithm, final MemberKeyPair own, final byte[] aad, final String armoredCircleKey) {
        try {
            final byte[] raw = HybridKem.unwrap(own, aad, Base64.getDecoder().decode(armoredCircleKey));
            final SecretKey key = new SecretKeySpec(raw, algorithm.getName());
            Arrays.fill(raw, (byte) 0);

            return new SecretEDSKey(algorithm, key);
        } catch (IllegalArgumentException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    /**
     * Derives a key from a high-entropy secret with HKDF-SHA512, e.g. the
     * Session key from a Session token (PLAN.md, Crypto §2a). Unlike PBKDF2, this
     * is cheap, so it may run on every request.
     *
     * @param secret High-entropy secret, at least 256 bits
     * @param salt   HKDF salt
     * @param info   HKDF info, binding the key to its use
     * @return AES-256-GCM key
     */
    public static SecretEDSKey deriveKey(final byte[] secret, final byte[] salt, final byte[] info) {
        try {
            final byte[] derived = KDF.getInstance(HKDF_ALGORITHM).deriveData(HKDFParameterSpec.ofExtract()
                    .addIKM(secret)
                    .addSalt(salt)
                    .thenExpand(info, DERIVED_KEY_BYTES));
            final KeyAlgorithm algorithm = KeyAlgorithm.AES_GCM_256;
            final SecretEDSKey key = new SecretEDSKey(algorithm, new SecretKeySpec(derived, algorithm.getName()));
            Arrays.fill(derived, (byte) 0);

            return key;
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public byte[] stringToBytes(final String string) {
        return string.getBytes(settings.getCharset());
    }

    public String bytesToString(final byte[] bytes) {
        return new String(bytes, settings.getCharset());
    }
}
