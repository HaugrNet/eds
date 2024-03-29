/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.exceptions.CryptoException;
import net.haugr.eds.core.model.Settings;

/**
 * <p>This library contain all the Cryptographic Operations, needed for EDS, to
 * support the features required. JCA, Java Cryptography Architecture, contains
 * all the features needed, and is flexible enough, that it can be extended by
 * providing different vendors - which will then allow using stronger encryption
 * if needed.</p>
 *
 * <p>EDS uses two (three) types of Encryption. Symmetric Encryption of all the
 * actual Data to be shared and Asymmetric Encryption to storing the Symmetric
 * keys. Additionally, all Private Key are be stored encrypted, and a Key is
 * derived (using PBKDF2) from the Credentials to unlock it.</p>
 *
 * <p>The default Algorithms and Key sizes have been chosen, so they will work
 * with a standard Java 8 (build 161+) installation, these uses the maximum key
 * size allowed, if an older Java is used, then either install the Java 8
 * Security extension from Oracle, or change the default configuration of EDS
 * accordingly.</p>
 *
 * <p>Although Cryptography is the cornerstone of the EDS, there is no attempts
 * made towards creating or inventing various Algorithms. The risk of making
 * mistakes is too high. Instead, the EDS relies on the wisdom and maturity of
 * existing JCE implementations in Java.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class Crypto {

    private final MasterKey masterKey;
    private final Settings settings;

    public Crypto(final Settings settings) {
        masterKey = MasterKey.getInstance(settings);
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
     * @param salt      System specific Salt
     * @return Symmetric Key
     * @throws CryptoException if an error occurred
     */
    public SecretEDSKey generatePasswordKey(final KeyAlgorithm algorithm, final byte[] secret, final String salt) {
        try {
            final char[] extendedSecret = convertSecret(secret);
            final byte[] secretSalt = stringToBytes(salt);

            final SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm.getTransformationValue());
            final KeySpec spec = new PBEKeySpec(extendedSecret, secretSalt, settings.getPasswordIterations(), algorithm.getLength());
            final SecretKey tmpKey = factory.generateSecret(spec);
            final SecretKey secretKey = new SecretKeySpec(tmpKey.getEncoded(), algorithm.getName());
            final SecretEDSKey key = new SecretEDSKey(algorithm.getDerived(), secretKey);
            key.setSalt(new IVSalt(salt));

            return key;
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    /**
     * <p>Converting the given secret (byte array) into a char array with the
     * salt from the system appended without using the String Object is not
     * trivial.</p>
     *
     * <p>From <a href="https://stackoverflow.com/a/9855338">Stackoverflow</a>,
     * it is clear that the solution can be complex, but as all which is needed
     * here is a way to convert the bytes to chars so a Key can be generated, a
     * simpler conversion may be sufficient.</p>
     *
     * @param secret Provided Passphrase or Secret
     * @return Extended secret as char array
     */
    private char[] convertSecret(final byte[] secret) {
        final char[] secretChars = MasterKey.generateSecretChars(secret);
        final char[] salt = settings.getSalt().toCharArray();
        final char[] chars = new char[secretChars.length + salt.length];

        System.arraycopy(secretChars, 0, chars, 0, secretChars.length);
        System.arraycopy(salt, 0, chars, secretChars.length, salt.length);

        return chars;
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

    public static EDSKeyPair generateAsymmetricKey(final KeyAlgorithm algorithm) {
        try {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm.getName());
            generator.initialize(algorithm.getLength());
            final KeyPair keyPair = generator.generateKeyPair();

            return new EDSKeyPair(algorithm, keyPair);
        } catch (IllegalArgumentException | NoSuchAlgorithmException e) {
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
    // Standard Cryptographic Operations; Sign, Verify, Encrypt & Decrypt
    // =========================================================================

    public byte[] sign(final PrivateKey key, final byte[] message) {
        try {
            final Signature signer = Signature.getInstance(settings.getSignatureAlgorithm().getTransformationValue());
            signer.initSign(key);
            signer.update(message);

            return signer.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public boolean verify(final PublicKey key, final byte[] message, final byte[] signature) {
        try {
            final Signature verifier = Signature.getInstance(settings.getSignatureAlgorithm().getTransformationValue());
            verifier.initVerify(key);
            verifier.update(message);

            return verifier.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | IllegalArgumentException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public byte[] encryptWithMasterKey(final byte[] toEncrypt) {
        return encrypt(masterKey.getKey(), toEncrypt);
    }

    public String encryptWithMasterKey(final String toEncrypt) {
        final byte[] bytes = stringToBytes(toEncrypt);
        final byte[] encrypted = encryptWithMasterKey(bytes);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decryptWithMasterKey(final String toDecrypt) {
        final byte[] encrypted = Base64.getDecoder().decode(toDecrypt);
        final byte[] decrypted = decrypt(masterKey.getKey(), encrypted);
        return bytesToString(decrypted);
    }

    public static byte[] encrypt(final SecretEDSKey key, final byte[] toEncrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.ENCRYPT_MODE);
            return cipher.doFinal(toEncrypt);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public static byte[] encrypt(final PublicEDSKey key, final byte[] toEncrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.ENCRYPT_MODE);
            return cipher.doFinal(toEncrypt);
        } catch (ClassCastException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public static byte[] decrypt(final SecretEDSKey key, final byte[] toDecrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.DECRYPT_MODE);
            return cipher.doFinal(toDecrypt);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public static byte[] decrypt(final PrivateEDSKey key, final byte[] toDecrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.DECRYPT_MODE);
            return cipher.doFinal(toDecrypt);
        } catch (ClassCastException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    private static Cipher prepareCipher(final AbstractEDSKey<?> key, final int type) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        AlgorithmParameterSpec iv = null;
        final String instanceName;

        if (key.getAlgorithm().getType() == KeyAlgorithm.Type.ASYMMETRIC) {
            instanceName = key.getAlgorithm().getName();
        } else if (key.getAlgorithm().getType() == KeyAlgorithm.Type.SYMMETRIC) {
            final KeyAlgorithm algorithm = key.getAlgorithm();
            instanceName = algorithm.getTransformationValue();
            iv = switch (key.getAlgorithm().getTransformation()) {
                case AES_CBC ->
                    // SonarQube rule S3329 (http://localhost:9000/coding_rules?open=squid:S3329&rule_key=squid:S3329)
                    // is marking this place as a vulnerability, as it cannot
                    // ascertain that the salt is generated randomly using
                    // SecureRandom, and also stored armored in the database.
                    // As the same salt must be used for both encryption and
                    // decryption - the rule is simply not good enough.
                        new IvParameterSpec(((SecretEDSKey) key).getSalt().getBytes());
                case AES_GCM_128, AES_GCM_192, AES_GCM_256 ->
                        new GCMParameterSpec(Constants.GCM_IV_LENGTH, ((SecretEDSKey) key).getSalt().getBytes());
                default ->
                    // Unreachable Code by design, only 2 AES transformation
                    // Algorithms exists, and they are both checked.
                        throw new CryptoException("Cannot prepare Cipher for this Symmetric Algorithm " + key.getAlgorithm().getTransformation() + '.');
            };
        } else {
            throw new CryptoException("Cannot prepare Cipher for this Algorithm Type " + key.getAlgorithm().getType() + '.');
        }

        final Cipher cipher;
        cipher = Cipher.getInstance(instanceName);
        cipher.init(type, key.getKey(), iv);

        return cipher;
    }

    // =========================================================================
    // Key Protection, Encrypting & Armoring - De-armoring & Decrypting Keys
    // =========================================================================

    /**
     * The Public RSA Key stored in EDS, is simply saved in x.509 format, stored
     * Base64 encoded.
     *
     * @param key Public RSA key to armor (Base64 encoded x.509 Key)
     * @return String representation of the Key
     */
    public static String armoringPublicKey(final Key key) {
        final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key.getEncoded());
        final byte[] rawKey = keySpec.getEncoded();

        return Base64.getEncoder().encodeToString(rawKey);
    }

    public PublicKey dearmoringPublicKey(final String armoredKey) {
        try {
            final KeyAlgorithm algorithm = settings.getAsymmetricAlgorithm();
            final KeyFactory keyFactory = KeyFactory.getInstance(algorithm.getName());
            final byte[] rawKey = Base64.getDecoder().decode(armoredKey);
            final KeySpec x509KeySpec = new X509EncodedKeySpec(rawKey);

            return keyFactory.generatePublic(x509KeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    /**
     * The Private RSA Key stored in EDS, is stored encrypted, so it cannot be
     * extracted without some effort. To do this, a Key is needed, together with
     * a Salt which the Initial Vector is based on. The encrypted Key, is then
     * converted into PKCS8 and converted using Base64 encoding. The result of
     * this will make the key safe for storage in the database.
     *
     * @param encryptionKey Symmetric Key to encrypt the Private RSA Key with
     * @param privateKey    The Private RSA Key to encrypt and armor
     * @return Armored (PKCS8 and Base64 encoded encrypted key)
     */
    public static String encryptAndArmorPrivateKey(final SecretEDSKey encryptionKey, final Key privateKey) {
        final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        final byte[] rawKey = keySpec.getEncoded();
        final byte[] encryptedKey = encrypt(encryptionKey, rawKey);

        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    public PrivateKey dearmoringPrivateKey(final SecretEDSKey decryptionKey, final String armoredKey) {
        try {
            // We only need the name of the Asymmetric Algorithm here, not the
            // keySize. As all Asymmetric Algorithms share the same basic
            // algorithm and thus name, we can use the Asymmetric Algorithm
            // from the Settings.
            final KeyAlgorithm algorithm = settings.getAsymmetricAlgorithm();
            final KeyFactory keyFactory = KeyFactory.getInstance(algorithm.getName());
            final byte[] dearmored = Base64.getDecoder().decode(armoredKey);
            final byte[] rawKey = decrypt(decryptionKey, dearmored);
            final KeySpec keySpec = new PKCS8EncodedKeySpec(rawKey);

            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public static String encryptAndArmorCircleKey(final PublicEDSKey publicKey, final SecretEDSKey circleKey) {
        final byte[] encryptedCircleKey = encrypt(publicKey, circleKey.getEncoded());

        return Base64.getEncoder().encodeToString(encryptedCircleKey);
    }

    public static SecretEDSKey extractCircleKey(final KeyAlgorithm algorithm, final PrivateEDSKey privateKey, final String armoredCircleKey) {
        final byte[] dearmoredCircleKey = Base64.getDecoder().decode(armoredCircleKey);
        final byte[] decryptedCircleKey = decrypt(privateKey, dearmoredCircleKey);
        final SecretKey key = new SecretKeySpec(decryptedCircleKey, algorithm.getName());

        return new SecretEDSKey(algorithm, key);
    }

    /**
     * <p>The RSA KeyPair for each Member Account, is stored with an encrypted
     * Private Key and armored and the Public Key armored. This way, it is easy
     * to verify that the Key's are correctly stored as they are stored purely
     * as text and nothing else.</p>
     *
     * <p>To recreate the Key Pair the Private Key has to be decrypted and then
     * both the Public and Private Keys must be converted.</p>
     *
     * @param algorithm         EDS Key Algorithm
     * @param key               Symmetric Key to decrypt the Private Key with
     * @param salt              Base for the Initial Vector, used for decrypting
     * @param armoredPublicKey  Armored unencrypted Public Key
     * @param armoredPrivateKey Armored and encrypted Private Key
     * @return RSA KeyPair with the Public and Private Keys
     * @throws CryptoException if an error occurred
     */
    public EDSKeyPair extractAsymmetricKey(final KeyAlgorithm algorithm, final SecretEDSKey key, final String salt, final String armoredPublicKey, final String armoredPrivateKey) {
        key.setSalt(new IVSalt(salt));

        // Extracting the Public & Private Keys
        final PublicKey publicKey = dearmoringPublicKey(armoredPublicKey);
        final PrivateKey privateKey = dearmoringPrivateKey(key, armoredPrivateKey);

        // Build the EDSKeyPair
        final KeyPair keyPair = new KeyPair(publicKey, privateKey);
        return new EDSKeyPair(algorithm, keyPair);
    }

    public byte[] stringToBytes(final String string) {
        return string.getBytes(settings.getCharset());
    }

    public String bytesToString(final byte[] bytes) {
        return new String(bytes, settings.getCharset());
    }
}
