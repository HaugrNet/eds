/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common;

import io.javadog.cws.api.common.Constants;
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
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
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
 * support the features needed. JCA, Java Cryptography Architecture, contains
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

    private static final int INITIAL_VECTOR_SIZE = 16;
    private final Settings settings;

    public Crypto(final Settings settings) {
        this.settings = settings;
    }

    // =========================================================================
    // Public Methods
    // =========================================================================

    public byte[] encrypt(final Key key, final byte[] toEncrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.ENCRYPT_MODE, null);
            return cipher.doFinal(toEncrypt);
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException e) {
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

    public byte[] decrypt(final Key key, final byte[] toDecrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.DECRYPT_MODE, null);
            return cipher.doFinal(toDecrypt);
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

    public String sign(final KeyPair keyPair, final String message) {
        try {
            final Signature signer = Signature.getInstance(settings.getSignatureAlgorithm());
            signer.initSign(keyPair.getPrivate());
            signer.update(message.getBytes());
            final byte[] signed = signer.sign();

            return Base64.getEncoder().encodeToString(signed);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    public boolean verify(final KeyPair keyPair, final String message, final String signature) {
        try {
            final byte[] bytes = Base64.getDecoder().decode(signature);
            final Signature verifier = Signature.getInstance(settings.getSignatureAlgorithm());
            verifier.initVerify(keyPair.getPublic());
            verifier.update(message.getBytes());

            return verifier.verify(bytes);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    private static Cipher prepareCipher(final Key key, final int mode, final IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        final String algorithm = key.getAlgorithm();
        final Cipher cipher = Cipher.getInstance(algorithm);

        if ((iv != null) && algorithm.contains("CBC")) {
            cipher.init(mode, key, iv);
        } else {
            cipher.init(mode, key);
        }

        return cipher;
    }

    public IvParameterSpec generateInitialVector(final String salt) {
        final byte[] bytes = new byte[INITIAL_VECTOR_SIZE];
        System.arraycopy(salt.getBytes(), 0, bytes, 0, bytes.length);

        return new IvParameterSpec(bytes);
    }

    public SecretKey generateSymmetricKey() {
        try {
            final String algorithm = settings.getSymmetricAlgorithmName();

            final KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
            keyGenerator.init(settings.getSymmetricKeylength());
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new CWSException(e);
        }
    }

    public KeyPair generateAsymmetricKey() {
        try {
            final KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(settings.getAsymmetricAlgorithmName());
            keyGenerator.initialize(settings.getAsymmetricKeylength());
            return keyGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new CWSException(e);
        }
    }

    public SecretKey convertCredentialToKey(final char[] secret) {
        return new SecretKeySpec(base64Decode(secret), settings.getSymmetricAlgorithm());
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

    /**
     * The Public RSA Key stored in CWS, is simply saved in x.509 format, stored
     * Base64 encoded.
     *
     * @param key Public RSA key to armor (Base64 encoded x.509 Key)
     * @return String representation of the Key
     */
    public static String armorPublicKey(final PublicKey key) {
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
     * @param salt       Salt to generate the Initial Vector from
     * @param privateKey The Private RSA Key to encrypt and armor
     * @return Armored (Base64 encoded encrypted key in PCKS8 format)
     */
    public String armorPrivateKey(final Key key, final String salt, final PrivateKey privateKey) {
        final IvParameterSpec iv = generateInitialVector(salt);
        final byte[] encryptedPrivateKey = encrypt(key, iv, privateKey.getEncoded());
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

    public byte[] stringToBytes(final String string) {
        try {
            return string.getBytes(settings.getCharset());
        } catch (UnsupportedEncodingException e) {
            throw new CWSException(Constants.PROPERTY_ERROR, e);
        }
    }

    public String bytesToString(final byte[] bytes) {
        try {
            return new String(bytes, settings.getCharset());
        } catch (UnsupportedEncodingException e) {
            throw new CWSException(Constants.PROPERTY_ERROR, e);
        }
    }

    private byte[] base64Decode(final char[] chars) {
        final CharBuffer charBuffer = CharBuffer.wrap(chars);
        final ByteBuffer byteBuffer = getCharSet().encode(charBuffer);
        final byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), 0, byteBuffer.limit());

        // To ensure that no traces of the sensitive data still exists, we're
        // filling the array with null's.
        clearSensitiveData(charBuffer.array());

        return Base64.getDecoder().decode(bytes);
    }

    public Charset getCharSet() {
        try {
            return Charset.forName(settings.getCharset());
        } catch (IllegalArgumentException e) {
            throw new CWSException(Constants.PROPERTY_ERROR, e);
        }
    }

    public static void clearSensitiveData(final char[] chars) {
        Arrays.fill(chars, '\u0000');
    }

    public static void clearSensitiveData(final byte[] bytes) {
        Arrays.fill(bytes, (byte) 0x00);
    }
}
