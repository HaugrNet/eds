package io.javadog.cws.common;

import io.javadog.cws.common.exceptions.CWSCryptoException;
import io.javadog.cws.common.exceptions.CWSException;

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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
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
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new CWSCryptoException(e);
        }
    }

    public byte[] encrypt(final Key key, final IvParameterSpec iv, final byte[] toEncrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.ENCRYPT_MODE, iv);
            return cipher.doFinal(toEncrypt);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new CWSCryptoException(e);
        }
    }

    public byte[] decrypt(final Key key, final byte[] toDecrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.DECRYPT_MODE, null);
            return cipher.doFinal(toDecrypt);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new CWSCryptoException(e);
        }
    }

    public byte[] decrypt(final Key key, final IvParameterSpec iv, final byte[] toDecrypt) {
        try {
            final Cipher cipher = prepareCipher(key, Cipher.DECRYPT_MODE, iv);
            return cipher.doFinal(toDecrypt);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new CWSCryptoException(e);
        }
    }

    private static Cipher prepareCipher(final Key key, final int mode, final IvParameterSpec iv) {
        try {
            final String algorithm = key.getAlgorithm();
            final Cipher cipher = Cipher.getInstance(algorithm);

            if (algorithm.contains("CBC") && (iv != null)) {
                cipher.init(mode, key, iv);
            } else {
                cipher.init(mode, key);
            }

            return cipher;
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CWSCryptoException(e);
        }
    }

    public IvParameterSpec generateInitialVector(final String salt) {
        final byte[] bytes = new byte[16];
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

    public byte[] stringToBytes(final String string) {
        try {
            return string.getBytes(settings.getCharset());
        } catch (UnsupportedEncodingException e) {
            throw new CWSException(e);
        }
    }

    public String bytesToString(final byte[] bytes) {
        try {
            return new String(bytes, settings.getCharset());
        } catch (UnsupportedEncodingException e) {
            throw new CWSException(e);
        }
    }

    public static String base64Encode(final byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private byte[] base64Decode(final char[] chars) {
        final CharBuffer charBuffer = CharBuffer.wrap(chars);
        final ByteBuffer byteBuffer = Charset.forName(settings.getCharset()).encode(charBuffer);
        final byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), 0, byteBuffer.limit());

        // To ensure that no traces of the sensitive data still exists, we're
        // filling the array with null's.
        Arrays.fill(charBuffer.array(), '\u0000');

        return Base64.getDecoder().decode(bytes);
    }

    public static byte[] base64Decode(final String str) {
        return Base64.getDecoder().decode(str);
    }

    public static void clearSensitiveData(final char[] chars) {
        Arrays.fill(chars, '\u0000');
    }
}
