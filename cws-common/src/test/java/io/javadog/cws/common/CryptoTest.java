/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.common.exceptions.CryptoException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

/**
 * Proof Of Concept, showing that the simple Cryptographic Operations will work,
 * and how to build up the simple components.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CryptoTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSignature() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(new Settings());
        final KeyPair keyPair = crypto.generateAsymmetricKey();
        final byte[] message = "Message to Sign".getBytes(settings.getCharset());
        final String signed = crypto.sign(keyPair.getPrivate(), message);
        final boolean verified = crypto.verify(keyPair.getPublic(), message, signed);

        assertThat(verified, is(true));
    }

    @Test
    public void testArmoringPublicKey() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);

        final KeyPair keyPair = crypto.generateAsymmetricKey();
        final PublicKey key = keyPair.getPublic();

        final String armoredKey = crypto.armoringPublicKey(key);
        final PublicKey dearmoredKey = crypto.dearmoringPublicKey(armoredKey);

        assertThat(dearmoredKey.getAlgorithm(), is(key.getAlgorithm()));
        assertThat(dearmoredKey.getFormat(), is(key.getFormat()));
        assertThat(dearmoredKey.getEncoded(), is(key.getEncoded()));
    }

    @Test
    public void testArmoringPublicKeyInvalidAlgorithm() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);

        final KeyPair keyPair = crypto.generateAsymmetricKey();
        final PublicKey key = keyPair.getPublic();

        // Now, simulating incorrect settings
        settings.set(Settings.ASYMMETRIC_ALGORITHM, "AES");
        prepareCause(CryptoException.class, ReturnCode.CRYPTO_ERROR, "java.security.NoSuchAlgorithmException: AES KeyFactory not available");

        final String armoredKey = crypto.armoringPublicKey(key);
        assertThat(armoredKey.length(), is(392));
        crypto.dearmoringPublicKey(armoredKey);
    }

    @Test
    @Ignore("Error in the Asynchronous Encryption setup")
    public void testArmoringPrivateKey() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);

        final KeyPair cryptoKeys = crypto.generateAsymmetricKey();
        final KeyPair keyPair = crypto.generateAsymmetricKey();
        final PrivateKey key = keyPair.getPrivate();

        final String armoredKey = crypto.armoringPrivateKey(cryptoKeys.getPublic(), key);
        final PrivateKey dearmoredKey = crypto.dearmoringPrivateKey(cryptoKeys.getPrivate(), armoredKey);

        assertThat(dearmoredKey.getAlgorithm(), is(key.getAlgorithm()));
        assertThat(dearmoredKey.getFormat(), is(key.getFormat()));
        assertThat(dearmoredKey.getEncoded(), is(key.getEncoded()));
    }

    @Test
    @Ignore("Error in the Asynchronous Encryption setup")
    public void testArmoringPrivateKeyInvalidAlgorithm() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);

        final KeyPair cryptoKeys = crypto.generateAsymmetricKey();
        final KeyPair keyPair = crypto.generateAsymmetricKey();
        final PrivateKey key = keyPair.getPrivate();

        // Now, simulating incorrect settings
        settings.set(Settings.ASYMMETRIC_ALGORITHM, "AES");
        prepareCause(CryptoException.class, ReturnCode.CRYPTO_ERROR, "java.security.NoSuchAlgorithmException: AES KeyFactory not available");

        final String armoredKey = crypto.armoringPrivateKey(cryptoKeys.getPublic(), key);
        assertThat(armoredKey.length(), is(392));
        crypto.dearmoringPrivateKey(cryptoKeys.getPrivate(), armoredKey);
    }

    @Test
    public void testArmoringSecretKey() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);

        final KeyPair keyPair = crypto.generateAsymmetricKey();
        final SecretKey key = crypto.generateSymmetricKey();

        final String armoredKey = crypto.armoringSecretKey(keyPair.getPublic(), key);
        final SecretKey dearmoredKey = crypto.dearmoringSecretKey(keyPair.getPrivate(), armoredKey, key.getAlgorithm());

        assertThat(dearmoredKey.getAlgorithm(), is(key.getAlgorithm()));
        assertThat(dearmoredKey.getFormat(), is(key.getFormat()));
        assertThat(dearmoredKey.getEncoded(), is(key.getEncoded()));
    }

    /**
     * Testing how to properly Armor and De-armor a Key, meaning converting a
     * Binary Key into a String and converted back. For the process, CWS uses
     * the X.509 standard together with a simple Base64 encoding/decoding. The
     * first is the standard for Public/Private Keys and the latter allows the
     * result to be stored in an easily printable format that can be written
     * in files.
     */
    @Test
    public void testArmorAndDearmorAsymmetricKey() {
        final Crypto crypto = new Crypto(new Settings());
        final String salt = UUID.randomUUID().toString();
        final Key key = crypto.generateSymmetricKey();
        final KeyPair keyPair = crypto.generateAsymmetricKey();
        final String armoredPublicKey = Crypto.armorKey(keyPair.getPublic());
        final String armoredPrivateKey = crypto.armorPrivateKey(key, salt, keyPair.getPrivate());
        final KeyPair newPair = crypto.extractAsymmetricKey(key, salt, armoredPublicKey, armoredPrivateKey);

        assertThat(newPair.getPublic().getAlgorithm(), is(keyPair.getPublic().getAlgorithm()));
        assertThat(newPair.getPublic().getEncoded(), is(keyPair.getPublic().getEncoded()));
        assertThat(newPair.getPrivate().getAlgorithm(), is(keyPair.getPrivate().getAlgorithm()));
        assertThat(newPair.getPrivate().getEncoded(), is(keyPair.getPrivate().getEncoded()));
    }

    /**
     * There's two types of Cryptography applied in CWS. This test will
     * demonstrate the Symmetric Encryption part, which is used for all Objects
     * shared to a Group.
     */
    @Test
    public void testObjectEncryption() {
        final Crypto crypto = new Crypto(new Settings());
        final String salt = UUID.randomUUID().toString();
        final IvParameterSpec iv = crypto.generateInitialVector(salt);
        final SecretKey key = crypto.generateSymmetricKey();

        // Now, we're going to encrypt some data
        final String cleartext = "This is just an example";
        final byte[] toEncrypt = crypto.stringToBytes(cleartext);
        final byte[] encrypted = crypto.encrypt(key, iv, toEncrypt);

        // And decrypt it so we can verifyS it
        final byte[] decrypted = crypto.decrypt(key, iv, encrypted);
        final String result = crypto.bytesToString(decrypted);

        assertThat(result, is(cleartext));
    }

    /**
     * For the Group Members, we're storing the Symmetric Key per Member using
     * Asymmetric Encryption - this will allow that Group Members can be changed
     * independently of the Group Data. The Member's Public Key is stored with
     * the Member, encrypted using the Member's Public Key. The Private Key
     * which may be stored elsewhere or additionally encrypted can then be used
     * by the Member to access the data.
     */
    @Test
    public void testMemberEncryption() {
        final Crypto crypto = new Crypto(new Settings());
        final KeyPair key = crypto.generateAsymmetricKey();

        final String cleartext = "This is just an example";
        final byte[] toEncrypt = crypto.stringToBytes(cleartext);
        final byte[] encrypted = crypto.encrypt(key.getPublic(), toEncrypt);

        final byte[] decrypted = crypto.decrypt(key.getPrivate(), encrypted);
        final String result = crypto.bytesToString(decrypted);

        assertThat(result, is(cleartext));
    }

    /**
     * <p>Members of a Group must have both a Public and a Private Key. If they
     * are not providing a Private Key as part of initializing a Session, we
     * need a different way to retrieve it. We can, of course, generate a Key
     * Pair and store with the Member information, but storing the Private key
     * thus, can hardly be considered a good idea! So, instead we need to
     * encrypt it and store it so.</p>
     *
     * <p>This means that we must take a secret information provided by the
     * Member, and convert this into a Key, which we can then use. The standard
     * for this used to be PBKDF2 (Password-Based Key Derivation Function 2),
     * but as it has some weaknesses, a contest was made in 2015, which aimed at
     * replacing it. And the replacement is Argon2. However, as there is yet to
     * be added proper support for Argon2 in Java, we're sticking with PBKDF2
     * for our immediate needs.</p>
     */
    @Test
    public void testPasswordToKey() {
        final Crypto crypto = new Crypto(new Settings());
        final char[] password = "MySuperSecretPassword".toCharArray();
        final String salt = "SystemSpecificSalt";
        final SecretKey key = crypto.convertPasswordToKey(password, salt);

        assertThat(key.getAlgorithm(), is("AES"));

        final IvParameterSpec iv = crypto.generateInitialVector(salt);

        // Now, we're going to encrypt some data
        final String cleartext = "This is just an example";
        final byte[] encrypted = crypto.encrypt(key, iv, crypto.stringToBytes(cleartext));

        // And decrypt it so we can verify it
        final byte[] decrypted = crypto.decrypt(key, iv, encrypted);
        final String result = crypto.bytesToString(decrypted);

        assertThat(result, is(cleartext));
    }

    /**
     * <p>Circles have a Key generated, which is stored encrypted per Trustee,
     * i.e. Member with access to the Circle. The Circle Key is encrypted using
     * the Member's Public Key, and can be decrypted using the Member's Private
     * Key which again is unlocked during the Authentication Process.</p>
     */
    @Test
    public void testMemberAccessCircleKey() {
        final Crypto crypto = new Crypto(new Settings());
        final Charset charset = new Settings().getCharset();
        final String dataSalt = UUID.randomUUID().toString();
        final IvParameterSpec iv = crypto.generateInitialVector(dataSalt);
        final SecretKey key = crypto.generateSymmetricKey();
        final byte[] rawdata = UUID.randomUUID().toString().getBytes(charset);
        final byte[] encryptedData = crypto.encrypt(key, iv, rawdata);

        final KeyPair keyPair = crypto.generateAsymmetricKey();
        final String armoredCircleKey = crypto.encryptAndArmorCircleKey(keyPair.getPublic(), key);
        final SecretKey circleKey = crypto.extractCircleKey(keyPair.getPrivate(), armoredCircleKey, key.getAlgorithm());
        final byte[] decryptedData = crypto.decrypt(circleKey, iv, encryptedData);

        Crypto.clearSensitiveData(encryptedData);
        assertThat(decryptedData, is(rawdata));
    }

    @Test
    public void testStringToBytesConversion() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);
        final String str = "Alpha Beta æøåßöäÿ";

        final String garbage = "INVALID_ENCODING";
        settings.set(Settings.CWS_CHARSET, garbage);

        prepareCause(CWSException.class, ReturnCode.PROPERTY_ERROR, "java.nio.charset.UnsupportedCharsetException: " + garbage);
        assertThat(str, is(not(nullValue())));
        crypto.stringToBytes(str);
    }

    @Test
    public void testBytesToStringConversion() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);
        final byte[] bytes = "Alpha Beta æøåßöäÿ".getBytes(settings.getCharset());
        final String garbage = "INVALID_ENCODING";
        settings.set(Settings.CWS_CHARSET, garbage);

        prepareCause(CWSException.class, ReturnCode.PROPERTY_ERROR, "UnsupportedCharsetException: " + garbage);
        assertThat(bytes, is(not(nullValue())));
        crypto.bytesToString(bytes);
    }

    /**
     * Sensitive Data should only be handled via primitive arrays, and not
     * immutable Objects such as String, which will be kept in memory longer.
     * To ensure that the content of the Arrays is scrapped, a small method
     * exist for doing this. This test will simply ensure that the result
     * from the invocation is a destroyed array.
     */
    @Test
    public void testDeletingArrays() {
        final byte[] emptyBytes = {(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
        final char[] emptyChars = {(char) 0, (char) 0, (char) 0, (char) 0, (char) 0, (char) 0 };
        final String string = "String";

        final byte[] bytes = string.getBytes(new Settings().getCharset());
        Crypto.clearSensitiveData(bytes);
        assertThat(bytes, is(emptyBytes));

        final char[] chars = string.toCharArray();
        Crypto.clearSensitiveData(chars);
        assertThat(chars, is(emptyChars));
    }

    private <E extends CWSException> void prepareCause(final Class<E> cause, final ReturnCode returnCode, final String returnMessage) {
        final String propertyName = "returnCode";
        thrown.expect(cause);
        thrown.expectMessage(returnMessage);
        thrown.expect(hasProperty(propertyName));
        thrown.expect(hasProperty(propertyName, is(returnCode)));
    }
}
