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
import io.javadog.cws.common.exceptions.CryptoException;
import io.javadog.cws.common.keys.CWSKey;
import io.javadog.cws.common.keys.CWSKeyPair;
import io.javadog.cws.common.keys.SecretCWSKey;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.Charset;
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
    public void testGeneratingPasswordKeyWithInvalidAlgorithm() {
        thrown.expect(CryptoException.class);
        thrown.expectMessage("RSA/ECB/PKCS1Padding SecretKeyFactory not available");

        final Settings settings = new Settings();
        settings.set(Settings.PBE_ALGORITHM, "RSA2048");
        final Crypto crypto = new Crypto(settings);
        final String salt = UUID.randomUUID().toString();
        crypto.generatePasswordKey(KeyAlgorithm.AES128, "my secret", salt);
    }

    @Test
    public void testGeneratingSymmetricKeyWithInvalidAlgorithm() {
        thrown.expect(CryptoException.class);
        thrown.expectMessage("RSA KeyGenerator not available");

        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);
        crypto.generateSymmetricKey(KeyAlgorithm.RSA2048);
    }

    @Test
    public void testGeneratingAsymmetricKeyWithInvalidAlgorithm() {
        thrown.expect(CryptoException.class);
        thrown.expectMessage("AES KeyPairGenerator not available");

        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);
        crypto.generateAsymmetricKey(KeyAlgorithm.AES128);
    }

    @Test
    public void testGeneratingChecksumWithInvalidAlgorithm() {
        thrown.expect(CryptoException.class);
        thrown.expectMessage("No enum constant io.javadog.cws.common.enums.HashAlgorithm.AES128");

        final Settings settings = new Settings();
        settings.set(Settings.HASH_ALGORITHM, "AES128");
        final Crypto crypto = new Crypto(settings);
        crypto.generateChecksum("Bla bla bla");
    }

    @Test
    public void testSigningWithInvalidAlgorithm() {
        thrown.expect(CryptoException.class);
        thrown.expectMessage("AES/CBC/PKCS5Padding Signature not available");

        final Settings settings = new Settings();
        settings.set(Settings.SIGNATURE_ALGORITHM, "AES256");
        final Crypto crypto = new Crypto(settings);
        final CWSKeyPair key = crypto.generateAsymmetricKey(KeyAlgorithm.RSA2048);
        crypto.sign(key.getPrivate().getKey(), "bla bla bla".getBytes(settings.getCharset()));
    }

    @Test
    public void testSignature() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);
        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final byte[] message = "Message to Sign".getBytes(settings.getCharset());
        final String signature = crypto.sign(keyPair.getPrivate().getKey(), message);
        final boolean verified = crypto.verify(keyPair.getPublic().getKey(), message, signature);

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

        final CWSKeyPair key = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredKey = crypto.armoringPublicKey(key.getPublic().getKey());
        final PublicKey dearmoredKey = crypto.dearmoringPublicKey(armoredKey);

        assertThat(dearmoredKey, is(key.getPublic().getKey()));
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
        final SecretCWSKey cryptoKeys = crypto.generatePasswordKey(settings.getSymmetricAlgorithm(), password, salt);
        cryptoKeys.setSalt(UUID.randomUUID().toString());
        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredKey = crypto.armoringPrivateKey(cryptoKeys, keyPair.getPrivate().getKey());
        final PrivateKey dearmoredKey = crypto.dearmoringPrivateKey(cryptoKeys, armoredKey);

        assertThat(keyPair.getPrivate().getKey(), is(dearmoredKey));
    }

    @Test
    public void testArmoringAsymmetricKey() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);

        final String secret = "MySuperSecretPassword";
        final String salt = UUID.randomUUID().toString();
        final SecretCWSKey secretKey = crypto.generatePasswordKey(settings.getSymmetricAlgorithm(), secret, salt);
        secretKey.setSalt(salt);
        final CWSKeyPair pair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredPublicKey = crypto.armoringPublicKey(pair.getPublic().getKey());
        final String armoredPrivateKey = crypto.armoringPrivateKey(secretKey, pair.getPrivate().getKey());

        final CWSKeyPair dearmoredPair = crypto.extractAsymmetricKey(pair.getAlgorithm(), secretKey, salt, armoredPublicKey, armoredPrivateKey);
        assertThat(dearmoredPair.getAlgorithm(), is(pair.getAlgorithm()));
        assertThat(dearmoredPair.getPublic(), is(pair.getPublic()));
        assertThat(dearmoredPair.getPrivate(), is(pair.getPrivate()));
        assertThat(dearmoredPair.getPublic().hashCode(), is(pair.getPublic().hashCode()));
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
        final SecretCWSKey key = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
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
        final CWSKeyPair key = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String cleartext = "This is just an example";
        final byte[] toEncrypt = crypto.stringToBytes(cleartext);
        final byte[] encrypted = crypto.encrypt(key.getPublic(), toEncrypt);

        final byte[] decrypted = crypto.decrypt(key.getPrivate(), encrypted);
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
        final SecretCWSKey key = crypto.generatePasswordKey(settings.getSymmetricAlgorithm(), password, salt);

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
        // Added this stupid assertion, as SonarQube failed to detect the
        // assertion at the end of the test.
        assertThat(Boolean.parseBoolean("Is SonarQube rule squid:S2699 working correctly ?"), is(false));

        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(new Settings());
        final Charset charset = new Settings().getCharset();
        final String dataSalt = UUID.randomUUID().toString();
        final SecretCWSKey key = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        key.setSalt(dataSalt);
        final byte[] rawdata = UUID.randomUUID().toString().getBytes(charset);
        final byte[] encryptedData = crypto.encrypt(key, rawdata);

        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final String armoredCircleKey = crypto.encryptAndArmorCircleKey(keyPair.getPublic(), key);
        final SecretCWSKey circleKey = crypto.extractCircleKey(key.getAlgorithm(), keyPair.getPrivate(), armoredCircleKey);
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

    @Test
    public void testCWSKeyPairEquality() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);
        final CWSKeyPair key1 = crypto.generateAsymmetricKey(KeyAlgorithm.RSA4096);
        final CWSKeyPair key2 = crypto.generateAsymmetricKey(KeyAlgorithm.RSA8192);
        final CWSKeyPair key3 = crypto.generateAsymmetricKey(KeyAlgorithm.RSA4096);
        final KeyPair pair1 = new KeyPair(key1.getPublic().getKey(), key1.getPrivate().getKey());
        final KeyPair pair2 = new KeyPair(key1.getPublic().getKey(), key2.getPrivate().getKey());
        final KeyPair pair3 = new KeyPair(key2.getPublic().getKey(), key2.getPrivate().getKey());
        final CWSKeyPair cwsPair1 = new CWSKeyPair(key1.getAlgorithm(), pair1);
        final CWSKeyPair cwsPair2 = new CWSKeyPair(key2.getAlgorithm(), pair2);
        final CWSKeyPair cwsPair3 = new CWSKeyPair(key3.getAlgorithm(), pair3);
        final CWSKey<PublicKey> key4 = key1.getPublic();
        final CWSKey<PrivateKey> key5 = key1.getPrivate();

        assertThat(key1.equals(key1), is(true));
        assertThat(key1.equals(key2), is(false));
        assertThat(key1.equals(key3), is(false));
        assertThat(key4.equals(key5), is(false));
        assertThat(key1.equals(null), is(false));
        assertThat(key1.equals("nope"), is(false));
        assertThat(cwsPair1.equals(cwsPair2), is(false));
        assertThat(cwsPair1.equals(cwsPair3), is(false));

        assertThat(key1.hashCode(), is(key1.hashCode()));
        assertThat(key1.hashCode(), is(not(key2.hashCode())));
    }

    @Test
    public void testCWSKeyEquality() {
        final Settings settings = new Settings();
        final Crypto crypto = new Crypto(settings);
        final SecretCWSKey key1 = crypto.generateSymmetricKey(KeyAlgorithm.AES128);
        final SecretCWSKey key2 = new SecretCWSKey(key1.getAlgorithm(), key1.getKey());
        final SecretCWSKey key3 = crypto.generateSymmetricKey(KeyAlgorithm.AES128);
        key2.setSalt(UUID.randomUUID().toString());
        key3.setSalt(UUID.randomUUID().toString());

        assertThat(key1.equals(key1), is(true));
        assertThat(key1.equals(key2), is(false));
        assertThat(key1.equals(key3), is(false));
        assertThat(key1.equals(null), is(false));
        assertThat(key1.equals("nope"), is(false));

        assertThat(key1.hashCode(), is(key1.hashCode()));
        assertThat(key1.hashCode(), is(not(key2.hashCode())));
    }

    private <E extends CWSException> void prepareCause(final Class<E> cause, final ReturnCode returnCode, final String returnMessage) {
        final String propertyName = "returnCode";
        thrown.expect(cause);
        thrown.expectMessage(returnMessage);
        thrown.expect(hasProperty(propertyName));
        thrown.expect(hasProperty(propertyName, is(returnCode)));
    }
}
