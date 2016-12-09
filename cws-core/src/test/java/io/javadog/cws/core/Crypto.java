package io.javadog.cws.core;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Crypto {

    private static final String CHARSETNAME = "UTF-8";

    private final IvParameterSpec iv;
    private final KeyPair keyPair;
    private final Key key;
    private final String algorithm;

    // =========================================================================
    // Constructors
    // =========================================================================

    public Crypto(final KeyPair keyPair, final String algorithm) {
        this.iv = null;
        this.keyPair = keyPair;
        this.key = null;
        this.algorithm = algorithm;
    }

    public Crypto(final IvParameterSpec iv, final Key key, final String algorithm) {
        this.iv = iv;
        this.key = key;
        this.keyPair = null;
        this.algorithm = algorithm;
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
            final Cipher cipher = Cipher.getInstance(algorithm);

            if (key != null) {
                cipher.init(mode, key, iv);
            } else {
                if (mode == Cipher.ENCRYPT_MODE) {
                    cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
                } else {
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

    public static SecretKey generateSymmetricKey(final String algorithm, final int keysize) {
        try {
            final String toUse;
            if (algorithm.contains("/")) {
                toUse = algorithm.substring(0, algorithm.indexOf('/'));
            } else {
                toUse = algorithm;
            }
            final KeyGenerator keyGenerator = KeyGenerator.getInstance(toUse);
            keyGenerator.init(keysize);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new CWSException(e);
        }
    }

    public static KeyPair generateAsymmetricKey(final String algorithm, final int keysize) {
        try {
            final KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(algorithm);
            keyGenerator.initialize(keysize);
            return keyGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
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
}
