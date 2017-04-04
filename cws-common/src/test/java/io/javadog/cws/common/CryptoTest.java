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

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.common.exceptions.CWSException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyPair;
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
        final String armoredPublicKey = Crypto.armorPublicKey(keyPair.getPublic());
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

        // And decrypt it so we can verify it
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
        final Charset charset = crypto.getCharSet();
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

        prepareCause(CWSException.class, Constants.PROPERTY_ERROR, "UnsupportedEncodingException: " + garbage);
        assertThat(str, is(not(nullValue())));
        crypto.stringToBytes(str);
    }

    @Test
    public void testBytesToStringConversion() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);
        final byte[] bytes = "Alpha Beta æøåßöäÿ".getBytes();
        final String garbage = "INVALID_ENCODING";
        settings.set(Settings.CWS_CHARSET, garbage);

        prepareCause(CWSException.class, Constants.PROPERTY_ERROR, "UnsupportedEncodingException: " + garbage);
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

        final byte[] bytes = string.getBytes();
        Crypto.clearSensitiveData(bytes);
        assertThat(bytes, is(emptyBytes));

        final char[] chars = string.toCharArray();
        Crypto.clearSensitiveData(chars);
        assertThat(chars, is(emptyChars));
    }

    /**
     * <p>Although the Charset is set via the Settings, it is used primarily
     * together with the Crypto Library. It can be argued where it belongs, but
     * for now it resides in the Crypto Library so the testing of it is also
     * via the Crypto Library.</p>
     */
    @Test
    public void testCharset() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);

        // First part of the test, expecting that that the default Settings
        // Charset is the same as the one we get from the Crypto Library.
        final String charset = settings.getCharset();
        assertThat(crypto.getCharSet().name(), is(charset));

        // Now, update the Charset, and ensure that it is also updated in the
        // Crypto Library.
        final String latin9 = "ISO-8859-15";
        settings.set(Settings.CWS_CHARSET, latin9);
        assertThat(crypto.getCharSet().name(), is(latin9));

        // Final part of the test, set the Charset to an invalid entry, this
        // should result in an Exception, with the following details.
        final String garbage = "INVALID_ENCODING";
        thrown.expect(CWSException.class);
        thrown.expectMessage("UnsupportedCharsetException: " + garbage);
        thrown.expect(hasProperty("returnCode"));
        thrown.expect(hasProperty("returnCode", is(Constants.PROPERTY_ERROR)));

        settings.set(Settings.CWS_CHARSET, garbage);
        crypto.getCharSet();
    }

    private <E extends CWSException> void prepareCause(final Class<E> cause, final int returnCode, final String returnMessage) {
        final String propertyName = "returnCode";
        thrown.expect(cause);
        thrown.expectMessage(returnMessage);
        thrown.expect(hasProperty(propertyName));
        thrown.expect(hasProperty(propertyName, is(returnCode)));
    }
}
