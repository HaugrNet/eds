package io.javadog.cws.core;

import io.javadog.cws.core.exceptions.CWSException;

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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.UUID;

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

    private static final String SYMMETRIC_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String ASYMMETRIC_ALGORITHM = "RSA";
    private static final int ASYMMETRIC_KEYLENGTH = 2048;
    private static final int SYMMETRIC_KEYLENGTH = 256;
    private static final String CHARSETNAME = "UTF-8";

    private final IvParameterSpec iv;
    private final KeyPair keyPair;
    private final Key key;

    // =========================================================================
    // Constructors
    // =========================================================================

    public Crypto(final KeyPair keyPair) {
        this.iv = null;
        this.keyPair = keyPair;
        this.key = null;
    }

    public Crypto(final IvParameterSpec iv, final Key key) {
        this.iv = iv;
        this.key = key;
        this.keyPair = null;
    }

    // =========================================================================
    // Public Methods
    // =========================================================================

    public byte[] encrypt(final byte[] toEncrypt) {
        return cryptoOperation(Cipher.ENCRYPT_MODE, toEncrypt);
    }

    public byte[] decrypt(final byte[] toEncrypt) {
        return cryptoOperation(Cipher.DECRYPT_MODE, toEncrypt);
    }

    // =========================================================================
    // Internal Methods
    // =========================================================================

    private Cipher prepareCipher(final int mode) {
        try {
            final Cipher cipher;

            if (key != null) {
                cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
                cipher.init(mode, key, iv);
            } else {
                if (mode == Cipher.ENCRYPT_MODE) {
                    cipher = Cipher.getInstance(keyPair.getPublic().getAlgorithm());
                    cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
                } else {
                    cipher = Cipher.getInstance(keyPair.getPrivate().getAlgorithm());
                    cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                }
            }

            return cipher;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new CWSException(e);
        }
    }

    private byte[] cryptoOperation(final int cipherMode, final byte[] bytes) {
        try {
            final Cipher cipher = prepareCipher(cipherMode);
            return cipher.doFinal(bytes);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new CWSException(e);
        }
    }

    // =========================================================================
    // Static Helper Methods
    // =========================================================================

    public static IvParameterSpec generateNewInitialVector() {
        final String random = UUID.randomUUID().toString();
        final byte[] bytes = new byte[16];
        System.arraycopy(random.getBytes(), 0, bytes, 0, bytes.length);

        return new IvParameterSpec(bytes);
    }

    public static SecretKey generateSymmetricKey() {
        try {
            final String algorithm;
            if (SYMMETRIC_ALGORITHM.contains("/")) {
                algorithm = SYMMETRIC_ALGORITHM.substring(0, SYMMETRIC_ALGORITHM.indexOf('/'));
            } else {
                algorithm = SYMMETRIC_ALGORITHM;
            }

            final KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
            keyGenerator.init(SYMMETRIC_KEYLENGTH);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new CWSException(e);
        }
    }

    public static KeyPair generateAsymmetricKey() {
        try {
            final KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM);
            keyGenerator.initialize(ASYMMETRIC_KEYLENGTH);
            return keyGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new CWSException(e);
        }
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
    public static SecretKey convertPasswordToKey(final char[] password, final String salt) {
        try {
            final String algorithm = "PBKDF2WithHmacSHA256";
            final byte[] secret = stringToBytes(salt);

            final SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            final KeySpec spec = new PBEKeySpec(password, secret, 1024, SYMMETRIC_KEYLENGTH);
            final SecretKey tmpKey = factory.generateSecret(spec);

            return new SecretKeySpec(tmpKey.getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CWSException(e);
        }
    }

    public static byte[] stringToBytes(final String string) {
        try {
            return string.getBytes(CHARSETNAME);
        } catch (UnsupportedEncodingException e) {
            throw new CWSException(e);
        }
    }

    public static String bytesToString(final byte[] bytes) {
        try {
            return new String(bytes, CHARSETNAME);
        } catch (UnsupportedEncodingException e) {
            throw new CWSException(e);
        }
    }

    public static String base64Encode(final byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] base64Decode(final String str) {
        return Base64.getDecoder().decode(str);
    }
}
