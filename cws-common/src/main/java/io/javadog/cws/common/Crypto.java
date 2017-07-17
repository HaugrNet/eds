/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common;

import io.javadog.cws.common.enums.KeyAlgorithm;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.common.exceptions.CryptoException;

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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
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
 * keys. Additionally, a Members Private Key can be stored encrypted within the
 * system, and a Key is derived from the Credentials to unlock it, that is, if
 * the Member is not storing the Private Key.</p>
 *
 * <p>The default Algorithms and Key sizes have been chosen, so they will work
 * with a standard Java 8+ installation, if larger keys are requested, then the
 * Java installation must be configured accordingly.</p>
 *
 * <p>Although Cryptography is the cornerstone of the CWS, there is no attempts
 * made towards creating or inventing various Algorithms. The risk of making
 * mistakes is too high. Instead, the CWS relies on the wisdom and maturity of
 * existing Algorithms.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Crypto {

    private final Settings settings;

    public Crypto(final Settings settings) {
        this.settings = settings;
    }

    // =========================================================================
    // Public Methods to generate Keys
    // =========================================================================

    public CWSKey generateKey(final KeyAlgorithm algorithm, final String salt) {
        try {
            if (!algorithm.synchronous()) {
                throw new CryptoException("Expected a Synchronous Algorithm & Salt.");
            }

            final byte[] bytes = new byte[algorithm.getLength() / 8];
            System.arraycopy(salt.getBytes(settings.getCharset()), 0, bytes, 0, bytes.length);
            final IvParameterSpec iv = new IvParameterSpec(bytes);

            final KeyGenerator generator = KeyGenerator.getInstance(algorithm.getAlgorithm());
            generator.init(settings.getSymmetricKeylength());
            final SecretKey key = generator.generateKey();

            return new CWSKey(algorithm, key, iv);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
    }

    public CWSKey generateKey(final KeyAlgorithm algorithm) {
        try {
            if (algorithm.synchronous()) {
                throw new CryptoException("Expected an Asynchronous Algorithm.");
            }

            final KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm);
            generator.initialize(settings.getAsymmetricKeylength());
            final KeyPair keyPair = generator.generateKeyPair();

            return new CWSKey(algorithm, keyPair);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
    }

    public IvParameterSpec generateInitialVector(final KeyAlgorithm algorithm, final String salt) {
        final byte[] bytes = new byte[algorithm.getLength() / 8];
        System.arraycopy(salt.getBytes(settings.getCharset()), 0, bytes, 0, bytes.length);

        return new IvParameterSpec(bytes);
    }

    public SecretKey generateSymmetricKey() {
        try {
            final String algorithm = settings.getSymmetricAlgorithmName();
            final KeyGenerator generator = KeyGenerator.getInstance(algorithm);
            generator.init(settings.getSymmetricKeylength());

            return generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new CWSException(e);
        }
    }

    public KeyPair generateAsymmetricKey() {
        try {
            final String algorithm = settings.getAsymmetricAlgorithmName();
            final KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm);
            generator.initialize(settings.getAsymmetricKeylength());

            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new CWSException(e);
        }
    }

    public SecretKey convertCredentialToKey(final char[] secret) {
        return new SecretKeySpec(base64Decode(secret), settings.getSymmetricAlgorithmName());
    }

    /**
     * <p>Converts the given Salted Password to a Key, which can be used for the
     * initial Cryptographic Operations. With the help of the PBKDF2 algorithm,
     * it creates a 256 byte Key over 1024 iterations. However, for the Key to
     * be of a good enough Quality, it should be having a length of at least 16
     * characters and the same applies to the Salt.</p>
     *
     * <p>Note, that it takes the Password as a char array, rather than a
     * String. The reason for this, is that a Char array can be overridden with
     * garbage once we don't need it anymore, whereas a String which is
     * immutable can't. This way we don't have to wait for the Garbage Collector
     * to clean up things.</p>
     *
     * @param password Provided Password or Secret
     * @param salt     System specific Salt
     * @return Symmetric Key
     */
    public SecretKey convertPasswordToKey(final char[] password, final String salt) {
        try {
            final String algorithm = settings.getPBEAlgorithm();
            final byte[] secret = stringToBytes(salt);

            final SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            final KeySpec spec = new PBEKeySpec(password, secret, 1024, settings.getSymmetricKeylength());
            final SecretKey tmpKey = factory.generateSecret(spec);

            return new SecretKeySpec(tmpKey.getEncoded(), settings.getSymmetricAlgorithmName());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CWSException(e);
        }
    }

    // =========================================================================
    // Standard Cryptographic Operations; Sign,  Verify, Encrypt & Decrypt
    // =========================================================================

    public String sign(final CWSKey key, final byte[] message) {
        try {
            if (key.synchronous()) {
                throw new CryptoException("Expected a KeyPair for signing.");
            }

            final String algorithm = key.getAlgorithm().getAlgorithm();
            final Signature signer = Signature.getInstance(algorithm);
            signer.initSign(key.getPrivateKey());
            signer.update(message);
            final byte[] signed = signer.sign();

            return Base64.getEncoder().encodeToString(signed);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    public String sign(final PrivateKey privateKey, final byte[] message) {
        try {
            final String algorithm = settings.getSignatureAlgorithm();
            final Signature signer = Signature.getInstance(algorithm);
            signer.initSign(privateKey);
            signer.update(message);
            final byte[] signed = signer.sign();

            return Base64.getEncoder().encodeToString(signed);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    public boolean verify(final PublicKey publicKey, final byte[] message, final String signature) {
        try {
            final String algorithm = settings.getSignatureAlgorithm();
            final byte[] bytes = Base64.getDecoder().decode(signature);
            final Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(publicKey);
            verifier.update(message);

            return verifier.verify(bytes);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    public byte[] encrypt(final PublicKey key, final byte[] toEncrypt) {
        try {
            final Cipher cipher = prepareRSACipher(key, Cipher.ENCRYPT_MODE);
            return cipher.doFinal(toEncrypt);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    public byte[] decrypt(final PrivateKey key, final byte[] toDecrypt) {
        try {
            final Cipher cipher = prepareRSACipher(key, Cipher.DECRYPT_MODE);
            return cipher.doFinal(toDecrypt);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    public byte[] encrypt(final Key key, final IvParameterSpec iv, final byte[] toEncrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.ENCRYPT_MODE, iv);
            return cipher.doFinal(toEncrypt);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    public byte[] decrypt(final Key key, final IvParameterSpec iv, final byte[] toDecrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.DECRYPT_MODE, iv);
            return cipher.doFinal(toDecrypt);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    private static Cipher prepareRSACipher(final Key key, final int mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        final Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(mode, key);

        return cipher;
    }

    private Cipher prepareCipher(final Key key, final int type, final IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        final String algorithm = key.getAlgorithm();
        final String mode = settings.getSymmetricCipherMode();
        final String padding = settings.getSymmetricPadding();
        final Cipher cipher = Cipher.getInstance(algorithm + '/' + mode + '/' + padding);

        if ((iv != null) && mode.contains("CBC")) {
            cipher.init(type, key, iv);
        } else {
            cipher.init(type, key);
        }

        return cipher;
    }

    // =========================================================================
    // Key Protection, Encrypting & Armoring - De-armoring & Decrypting Keys
    // =========================================================================

    public String armoringPublicKey(final PublicKey key) {
        final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key.getEncoded());
        final byte[] rawKey = keySpec.getEncoded();

        return Base64.getEncoder().encodeToString(rawKey);
    }

    public PublicKey dearmoringPublicKey(final String armoredKey) {
        try {
            final String algorithm = settings.getAsymmetricAlgorithmName();
            final KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            final byte[] rawKey = Base64.getDecoder().decode(armoredKey);
            final X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(rawKey);

            return keyFactory.generatePublic(x509KeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
    }

    public String armoringPrivateKey(final PublicKey encryptionKey, final PrivateKey key) {
        final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key.getEncoded());
        final byte[] rawKey = keySpec.getEncoded();
        final byte[] encryptedKey = encrypt(encryptionKey, rawKey);

        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    public PrivateKey dearmoringPrivateKey(final PrivateKey decryptionKey, final String armoredKey) {
        try {
            final String algorithm = settings.getAsymmetricAlgorithmName();
            final KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            final byte[] dearmored = Base64.getDecoder().decode(armoredKey);
            final byte[] rawKey = decrypt(decryptionKey, dearmored);

            final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(rawKey);

            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
    }

    public String armoringSecretKey(final PublicKey encryptionKey, final SecretKey key) {
        final byte[] encryptedCircleKey = encrypt(encryptionKey, key.getEncoded());

        return Base64.getEncoder().encodeToString(encryptedCircleKey);
    }

    public SecretKey dearmoringSecretKey(final PrivateKey decryptionKey, final String armoredKey, final String algorithm) {
        final byte[] dearmoredCircleKey = Base64.getDecoder().decode(armoredKey);
        final byte[] decryptedCircleKey = decrypt(decryptionKey, dearmoredCircleKey);

        return new SecretKeySpec(decryptedCircleKey, algorithm);
    }

    /**
     * The Public RSA Key stored in CWS, is simply saved in x.509 format, stored
     * Base64 encoded.
     *
     * @param key Public RSA key to armor (Base64 encoded x.509 Key)
     * @return String representation of the Key
     * @deprecated please use {@link #armoringPublicKey(PublicKey)}
     */
    @Deprecated
    public static String armorKey(final Key key) {
        final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key.getEncoded());
        final byte[] rawKey = keySpec.getEncoded();

        return Base64.getEncoder().encodeToString(rawKey);
    }

    /**
     * The Private RSA Key stored in CWS, is stored encrypted, so it cannot be
     * extracted without some effort. To do this, a Key is needed, together with
     * a Salt which the Initial Vector is based on. The encrypted Key, is then
     * converted into PKCS8 and converted using Base64 encoding. The result of
     * this will make the key save for storage in the database.
     *
     * @param key        Symmetric Key to encrypt the Private RSA Key with
---     * @param salt       Salt to generate the Initial Vector from
     * @param privateKey The Private RSA Key to encrypt and armor
     * @return Armored (Base64 encoded encrypted key in PCKS8 format)
     */
    public String armorPrivateKey(final CWSKey key, final PrivateKey privateKey) {
        final byte[] encryptedPrivateKey = encrypt(key.getKey(), key.getIv(), privateKey.getEncoded());
        final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encryptedPrivateKey);
        final byte[] rawKey = keySpec.getEncoded();

        return Base64.getEncoder().encodeToString(rawKey);
    }

    public String encryptAndArmorCircleKey(final PublicKey publicKey, final SecretKey circleKey) {
        final byte[] encryptedCircleKey = encrypt(publicKey, circleKey.getEncoded());

        return Base64.getEncoder().encodeToString(encryptedCircleKey);
    }

    public SecretKey extractCircleKey(final PrivateKey privateKey, final String armoredCircleKey, final String algorithm) {
        final byte[] dearmoredCircleKey = Base64.getDecoder().decode(armoredCircleKey);
        final byte[] decryptedCircleKey = decrypt(privateKey, dearmoredCircleKey);

        return new SecretKeySpec(decryptedCircleKey, algorithm);
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
    public KeyPair extractAsymmetricKey(final Key key, final String salt, final String armoredPublicKey, final String armoredPrivateKey) {
        try {
            final KeyFactory keyFactory = KeyFactory.getInstance(settings.getAsymmetricAlgorithmName());
            final Base64.Decoder decoder = Base64.getDecoder();
            final IvParameterSpec iv = generateInitialVector(salt);

            final byte[] rawPublicKey = decoder.decode(armoredPublicKey);
            final byte[] encryptedPrivateKey = decoder.decode(armoredPrivateKey);
            final byte[] rawPrivateKey = decrypt(key, iv, encryptedPrivateKey);

            final X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(rawPublicKey);
            final PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(rawPrivateKey);

            final PublicKey thePublicKey = keyFactory.generatePublic(x509KeySpec);
            final PrivateKey thePrivateKEy = keyFactory.generatePrivate(pkcs8KeySpec);

            return new KeyPair(thePublicKey, thePrivateKEy);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
    }

    public PublicKey extractPublicKey(final String armoredPublicKey) {
        try {
            final KeyFactory keyFactory = KeyFactory.getInstance(settings.getAsymmetricAlgorithmName());
            final Base64.Decoder decoder = Base64.getDecoder();
            final byte[] rawPublicKey = decoder.decode(armoredPublicKey);
            final X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(rawPublicKey);

            return keyFactory.generatePublic(x509KeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
    }

    public byte[] stringToBytes(final String string) {
        return string.getBytes(settings.getCharset());
    }

    public String bytesToString(final byte[] bytes) {
        return new String(bytes, settings.getCharset());
    }

    private byte[] base64Decode(final char[] chars) {
        final CharBuffer charBuffer = CharBuffer.wrap(chars);
        final ByteBuffer byteBuffer = settings.getCharset().encode(charBuffer);
        final byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), 0, byteBuffer.limit());

        // To ensure that no traces of the sensitive data still exists, we're
        // filling the array with null's.
        clearSensitiveData(charBuffer.array());

        return Base64.getDecoder().decode(bytes);
    }

    public static void clearSensitiveData(final char[] chars) {
        Arrays.fill(chars, '\u0000');
    }

    public static void clearSensitiveData(final byte[] bytes) {
        Arrays.fill(bytes, (byte) 0x00);
    }
}
