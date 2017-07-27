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
import io.javadog.cws.common.enums.KeyAlgorithm;
import io.javadog.cws.common.exceptions.CWSException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.Charset;
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
        final CWSKey key = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final byte[] message = "Message to Sign".getBytes(settings.getCharset());
        final String signature = crypto.sign(key.getPrivate(), message);
        final boolean verified = crypto.verify(key.getPublic(), message, signature);

        assertThat(verified, is(true));
    }

    /**
     * The Public Key is stored armored in the database, meaning converted into
     * a Base64 encoded String, which can easily be read out again.
     */
    @Test
    public void testArmoringPublicKey() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);

        final CWSKey key = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredKey = crypto.armoringPublicKey(key.getPublic());
        final PublicKey dearmoredKey = crypto.dearmoringPublicKey(armoredKey);

        assertThat(dearmoredKey, is(key.getPublic()));
    }

    /**
     * The Private Key of a Member, is stored encrypted using a Member provided
     * passphrase which is turned into a Key. The PBE based Key is generated
     * using both a Member Salt and System Salt.
     */
    @Test
    public void testArmoringPrivateKey() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);

        final String password = "MySuperSecretPassword";
        final String salt = UUID.randomUUID().toString();
        final CWSKey cryptoKeys = crypto.generatePasswordKey(settings.getSymmetricAlgorithm(), password, salt);
        cryptoKeys.setSalt(UUID.randomUUID().toString());
        final CWSKey keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredKey = crypto.armoringPrivateKey(cryptoKeys, keyPair.getPrivate());
        final PrivateKey dearmoredKey = crypto.dearmoringPrivateKey(cryptoKeys, armoredKey);

        assertThat(keyPair.getPrivate(), is(dearmoredKey));
    }

    @Test
    public void testArmoringSecretKey() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);

        final String salt = UUID.randomUUID().toString();
        final CWSKey secretKey = crypto.generateSymmetricKey(KeyAlgorithm.AES128, salt);
        final CWSKey keyPair = crypto.generateAsymmetricKey(KeyAlgorithm.RSA2048);

        final String armoredKey = crypto.armoringSecretKey(keyPair, secretKey);
        final CWSKey dearmoredKey = crypto.dearmoringSecretKey(secretKey.getAlgorithm(), keyPair, armoredKey);

        assertThat(dearmoredKey.getType(), is(secretKey.getType()));
        assertThat(dearmoredKey.getAlgorithm(), is(secretKey.getAlgorithm()));
        assertThat(dearmoredKey.getKey(), is(secretKey.getKey()));
    }

    @Test
    public void testArmoringAsymmetricKey() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);

        final String secret = "MySuperSecretPassword";
        final String salt = UUID.randomUUID().toString();
        final CWSKey secretKey = crypto.generatePasswordKey(settings.getSymmetricAlgorithm(), secret, salt);
        secretKey.setSalt(salt);
        final CWSKey pair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredPublicKey = crypto.armoringPublicKey(pair.getPublic());
        final String armoredPrivateKey = crypto.armorPrivateKey(secretKey, pair.getPrivate());

        final CWSKey dearmoredPair = crypto.extractAsymmetricKey(pair.getAlgorithm(), secretKey, salt, armoredPublicKey, armoredPrivateKey);
        assertThat(dearmoredPair.getAlgorithm(), is(pair.getAlgorithm()));
        assertThat(dearmoredPair.getPublic(), is(pair.getPublic()));
        assertThat(dearmoredPair.getPrivate(), is(pair.getPrivate()));
    }

    /**
     * There's two types of Cryptography applied in CWS. This test will
     * demonstrate the Symmetric Encryption part, which is used for all Data
     * shared within Circles.
     */
    @Test
    public void testObjectEncryption() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);
        final String salt = UUID.randomUUID().toString();
        final CWSKey key = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm(), salt);
        key.setSalt(UUID.randomUUID().toString());

        // Now, we're going to encrypt some data
        final String cleartext = "This is just an example";
        final byte[] toEncrypt = crypto.stringToBytes(cleartext);
        final byte[] encrypted = crypto.encrypt(key, toEncrypt);

        // And decrypt it so we can verifyS it
        final byte[] decrypted = crypto.decrypt(key, encrypted);
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
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);
        final CWSKey key = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String cleartext = "This is just an example";
        final byte[] toEncrypt = crypto.stringToBytes(cleartext);
        final byte[] encrypted = crypto.encrypt(key, toEncrypt);

        final byte[] decrypted = crypto.decrypt(key, encrypted);
        final String result = crypto.bytesToString(decrypted);

        assertThat(result, is(cleartext));
    }

    /**
     * <p>Members of a Circle must have both a Public and a Private Key. If they
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
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(new Settings());
        final String password = "MySuperSecretPassword";
        final String salt = "SystemSpecificSalt";
        final CWSKey key = crypto.generatePasswordKey(settings.getSymmetricAlgorithm(), password, salt);

        // Now, we're going to encrypt some data
        key.setSalt(UUID.randomUUID().toString());
        final String cleartext = "This is just an example";
        final byte[] encrypted = crypto.encrypt(key, crypto.stringToBytes(cleartext));

        // And decrypt it so we can verify it
        final byte[] decrypted = crypto.decrypt(key, encrypted);
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
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(new Settings());
        final Charset charset = new Settings().getCharset();
        final String dataSalt = UUID.randomUUID().toString();
        final CWSKey key = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm(), dataSalt);
        final byte[] rawdata = UUID.randomUUID().toString().getBytes(charset);
        final byte[] encryptedData = crypto.encrypt(key, rawdata);

        final CWSKey keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final String armoredCircleKey = crypto.encryptAndArmorCircleKey(keyPair, key);
        final CWSKey circleKey = crypto.extractCircleKey(key.getAlgorithm(), keyPair, armoredCircleKey);
        circleKey.setSalt(dataSalt);
        final byte[] decryptedData = crypto.decrypt(circleKey, encryptedData);

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
        final String str = "Alpha Beta æøåßöäÿ";
        final byte[] bytes = str.getBytes(settings.getCharset());
        final String garbage = "INVALID_ENCODING";
        settings.set(Settings.CWS_CHARSET, garbage);

        prepareCause(CWSException.class, ReturnCode.PROPERTY_ERROR, "UnsupportedCharsetException: " + garbage);
        assertThat(bytes, is(not(nullValue())));
        final String reversed = crypto.bytesToString(bytes);
        assertThat(reversed, is(str));
    }

    private <E extends CWSException> void prepareCause(final Class<E> cause, final ReturnCode returnCode, final String returnMessage) {
        final String propertyName = "returnCode";
        thrown.expect(cause);
        thrown.expectMessage(returnMessage);
        thrown.expect(hasProperty(propertyName));
        thrown.expect(hasProperty(propertyName, is(returnCode)));
    }
}
