/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.jce;

import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.exceptions.CryptoException;
import io.javadog.cws.core.model.Settings;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * <p>This library contain all the Cryptographic Operations, needed for CWS, to
 * support the features required. JCA, Java Cryptography Architecture, contains
 * all the features needed, and is flexible enough, that it can be extended by
 * providing different vendors - which will then allow using stronger encryption
 * if needed.</p>
 *
 * <p>CWS uses two (three) types of Encryption. Symmetric Encryption of all the
 * actual Data to be shared and Asymmetric Encryption to storing the Symmetric
 * keys. Additionally, all Private Key are be stored encrypted, and a Key is
 * derived from the Credentials to unlock it.</p>
 *
 * <p>The default Algorithms and Key sizes have been chosen, so they will work
 * with a standard Java 8+ installation, if larger keys are requested, then the
 * Java installation must be configured accordingly.</p>
 *
 * <p>Although Cryptography is the cornerstone of the CWS, there is no attempts
 * made towards creating or inventing various Algorithms. The risk of making
 * mistakes is too high. Instead, the CWS relies on the wisdom and maturity of
 * existing implementations.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Crypto {

    // For AES, the blocksize is always 128 bit and thus the IV must also be of
    // the same size. See: https://en.wikipedia.org/wiki/Initialization_vector
    private static final int IV_SIZE = 16;

    private final Settings settings;

    public Crypto(final Settings settings) {
        this.settings = settings;
    }

    // =========================================================================
    // Public Methods to generate Keys
    // =========================================================================

    /**
     * <p>Converts the given Salted Password to a Key, which can be used for the
     * initial Cryptographic Operations. With the help of the PBKDF2 algorithm,
     * it creates a 256 byte Key over 1024 iterations. However, for the Key to
     * be of a good enough Quality, it should be having a length of at least 16
     * characters and the same applies to the Salt.</p>
     *
     * @param secret Provided Passphrase or Secret
     * @param salt   System specific Salt
     * @return Symmetric Key
     */
    public SecretCWSKey generatePasswordKey(final KeyAlgorithm algorithm, final String secret, final String salt) {
        try {
            final char[] extendedSecret = (secret + settings.getSalt()).toCharArray();
            final byte[] secretSalt = stringToBytes(salt);

            final SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm.getTransformation());
            final KeySpec spec = new PBEKeySpec(extendedSecret, secretSalt, 1024, algorithm.getLength());
            final SecretKey tmpKey = factory.generateSecret(spec);
            final SecretKey secretKey = new SecretKeySpec(tmpKey.getEncoded(), algorithm.getName());
            final SecretCWSKey key = new SecretCWSKey(algorithm.getDerived(), secretKey);
            key.setSalt(salt);

            return key;
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public SecretCWSKey generateSymmetricKey(final KeyAlgorithm algorithm) {
        try {
            final KeyGenerator generator = KeyGenerator.getInstance(algorithm.getName());
            generator.init(algorithm.getLength());
            final SecretKey key = generator.generateKey();

            return new SecretCWSKey(algorithm, key);
        } catch (IllegalArgumentException | NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public CWSKeyPair generateAsymmetricKey(final KeyAlgorithm algorithm) {
        try {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm.getName());
            generator.initialize(algorithm.getLength());
            final KeyPair keyPair = generator.generateKeyPair();

            return new CWSKeyPair(algorithm, keyPair);
        } catch (IllegalArgumentException | NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public String generateChecksum(final String value) {
        return generateChecksum(value.getBytes(settings.getCharset()));
    }

    public String generateChecksum(final byte[] bytes) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(settings.getHashAlgorithm().getAlgorithm());
            final byte[] hashed = digest.digest(bytes);

            return new String(hashed, settings.getCharset());
        } catch (IllegalArgumentException | NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    // =========================================================================
    // Standard Cryptographic Operations; Sign, Verify, Encrypt & Decrypt
    // =========================================================================

    public String sign(final PrivateKey key, final byte[] message) {
        try {
            final Signature signer = Signature.getInstance(settings.getSignatureAlgorithm().getTransformation());
            signer.initSign(key);
            signer.update(message);
            final byte[] signed = signer.sign();

            return Base64.getEncoder().encodeToString(signed);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public boolean verify(final PublicKey key, final byte[] message, final String signature) {
        try {
            final byte[] bytes = Base64.getDecoder().decode(signature);
            final Signature verifier = Signature.getInstance(settings.getSignatureAlgorithm().getTransformation());
            verifier.initVerify(key);
            verifier.update(message);

            return verifier.verify(bytes);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | IllegalArgumentException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public byte[] encrypt(final SecretCWSKey key, final byte[] toEncrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.ENCRYPT_MODE);
            return cipher.doFinal(toEncrypt);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public byte[] decrypt(final SecretCWSKey key, final byte[] toDecrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.DECRYPT_MODE);
            return cipher.doFinal(toDecrypt);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public byte[] encrypt(final PublicCWSKey key, final byte[] toEncrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.ENCRYPT_MODE);
            return cipher.doFinal(toEncrypt);
        } catch (ClassCastException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public byte[] decrypt(final PrivateCWSKey key, final byte[] toDecrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.DECRYPT_MODE);
            return cipher.doFinal(toDecrypt);
        } catch (ClassCastException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    private Cipher prepareCipher(final CWSKey<?> key, final int type) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        IvParameterSpec iv = null;
        final String instanceName;
        final Cipher cipher;

        if (key.getAlgorithm().getType() == KeyAlgorithm.Type.ASYMMETRIC) {
            instanceName = key.getAlgorithm().getName();
        } else {
            final KeyAlgorithm algorithm = key.getAlgorithm();
            instanceName = algorithm.getTransformation();
            final String salt = ((SecretCWSKey) key).getSalt();
            final byte[] bytes = new byte[IV_SIZE];
            System.arraycopy(salt.getBytes(settings.getCharset()), 0, bytes, 0, bytes.length);

            iv = new IvParameterSpec(bytes);
        }

        cipher = Cipher.getInstance(instanceName);
        cipher.init(type, key.getKey(), iv);

        return cipher;
    }

    // =========================================================================
    // Key Protection, Encrypting & Armoring - De-armoring & Decrypting Keys
    // =========================================================================

    /**
     * The Public RSA Key stored in CWS, is simply saved in x.509 format, stored
     * Base64 encoded.
     *
     * @param key Public RSA key to armor (Base64 encoded x.509 Key)
     * @return String representation of the Key
     */
    public String armoringPublicKey(final Key key) {
        final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key.getEncoded());
        final byte[] rawKey = keySpec.getEncoded();

        return Base64.getEncoder().encodeToString(rawKey);
    }

    public PublicKey dearmoringPublicKey(final String armoredKey) {
        try {
            final KeyAlgorithm algorithm = settings.getAsymmetricAlgorithm();
            final KeyFactory keyFactory = KeyFactory.getInstance(algorithm.getName());
            final byte[] rawKey = Base64.getDecoder().decode(armoredKey);
            final X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(rawKey);

            return keyFactory.generatePublic(x509KeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    /**
     * The Private RSA Key stored in CWS, is stored encrypted, so it cannot be
     * extracted without some effort. To do this, a Key is needed, together with
     * a Salt which the Initial Vector is based on. The encrypted Key, is then
     * converted into PKCS8 and converted using Base64 encoding. The result of
     * this will make the key safe for storage in the database.
     *
     * @param encryptionKey Symmetric Key to encrypt the Private RSA Key with
     * @param privateKey    The Private RSA Key to encrypt and armor
     * @return Armored (PKCS8 and Base64 encoded encrypted key)
     */
    public String armoringPrivateKey(final SecretCWSKey encryptionKey, final Key privateKey) {
        final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        final byte[] rawKey = keySpec.getEncoded();
        final byte[] encryptedKey = encrypt(encryptionKey, rawKey);

        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    public PrivateKey dearmoringPrivateKey(final SecretCWSKey decryptionKey, final String armoredKey) {
        try {
            final KeyAlgorithm algorithm = settings.getAsymmetricAlgorithm();
            final KeyFactory keyFactory = KeyFactory.getInstance(algorithm.getName());
            final byte[] dearmored = Base64.getDecoder().decode(armoredKey);
            final byte[] rawKey = decrypt(decryptionKey, dearmored);
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(rawKey);

            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public String encryptAndArmorCircleKey(final PublicCWSKey publicKey, final SecretCWSKey circleKey) {
        final byte[] encryptedCircleKey = encrypt(publicKey, circleKey.getEncoded());

        return Base64.getEncoder().encodeToString(encryptedCircleKey);
    }

    public SecretCWSKey extractCircleKey(final KeyAlgorithm algorithm, final PrivateCWSKey privateKey, final String armoredCircleKey) {
        final byte[] dearmoredCircleKey = Base64.getDecoder().decode(armoredCircleKey);
        final byte[] decryptedCircleKey = decrypt(privateKey, dearmoredCircleKey);
        final SecretKey key = new SecretKeySpec(decryptedCircleKey, algorithm.getName());

        return new SecretCWSKey(algorithm, key);
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
     * @param key               Symmetric Key to decrypt the Private Key with
     * @param salt              Base for the Initial Vector, used for decrypting
     * @param armoredPublicKey  Armored unencrypted Public Key
     * @param armoredPrivateKey Armored and encrypted Private Key
     * @return RSA KeyPair with the Public and Private Keys
     */
    public CWSKeyPair extractAsymmetricKey(final KeyAlgorithm algorithm, final SecretCWSKey key, final String salt, final String armoredPublicKey, final String armoredPrivateKey) {
        key.setSalt(salt);

        // Extracting the Public Key
        final PublicKey publicKey = dearmoringPublicKey(armoredPublicKey);

        // Extracting the Private Key
        final PrivateKey privateKey = dearmoringPrivateKey(key, armoredPrivateKey);

        // Build the CWSKeyPair
        final KeyPair keyPair = new KeyPair(publicKey, privateKey);
        return new CWSKeyPair(algorithm, keyPair);
    }

    public byte[] stringToBytes(final String string) {
        return string.getBytes(settings.getCharset());
    }

    public String bytesToString(final byte[] bytes) {
        return new String(bytes, settings.getCharset());
    }
}