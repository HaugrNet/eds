/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.jce;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.exceptions.CryptoException;
import io.javadog.cws.core.model.Settings;
import org.junit.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
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
public final class CryptoTest extends DatabaseSetup {

    @Test
    public void testGCMEncryption() {
        final String cleartext = "This is just an example";

        final KeyAlgorithm algorithm = KeyAlgorithm.AES_GCM_128;
        final SecretCWSKey key = crypto.generateSymmetricKey(algorithm);
        final IVSalt ivSalt = new IVSalt();
        key.setSalt(ivSalt);

        final byte[] cleartextBytes = crypto.stringToBytes(cleartext);
        final byte[] encryptedBytes = crypto.encrypt(key, cleartextBytes);
        final byte[] decryptedBytes = crypto.decrypt(key, encryptedBytes);
        final String decrypted = crypto.bytesToString(decryptedBytes);

        assertThat(decrypted, is(cleartext));
    }

    @Test
    public void testShaEncryption() {
        thrown.expect(CryptoException.class);
        thrown.expectMessage("Cannot prepare Cipher for this Algorithm Type SIGNATURE.");

        final KeyAlgorithm algorithm = KeyAlgorithm.SHA_256;
        final PublicCWSKey key = crypto.generateAsymmetricKey(KeyAlgorithm.RSA_2048).getPublic();
        final PublicCWSKey fakeKey = new PublicCWSKey(algorithm, key.getKey());
        final byte[] toEncrypt = { (byte) 1, (byte) 2, (byte) 3, (byte) 4 };

        crypto.encrypt(fakeKey, toEncrypt);
    }

    @Test
    public void testGeneratingPasswordKeyWithInvalidAlgorithm() {
        thrown.expect(CryptoException.class);
        thrown.expectMessage("AES/CBC/PKCS5Padding SecretKeyFactory not available");
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.PBE_ALGORITHM.getKey(), "RSA_2048");
        final Crypto myCrypto = new Crypto(mySettings);

        final String salt = UUID.randomUUID().toString();
        myCrypto.generatePasswordKey(KeyAlgorithm.AES_CBC_128, crypto.stringToBytes("my secret"), salt);
    }

    @Test
    public void testPasswordWithWeirdCharacters() {
        final byte[] secret = new byte[256];
        for (int i = 0; i<256; i++) {
            secret[i] = (byte) i;
        }
        final KeyAlgorithm algorithm = KeyAlgorithm.PBE_128;
        final String salt = UUID.randomUUID().toString();
        final SecretCWSKey key = crypto.generatePasswordKey(algorithm, secret, salt);
        assertThat(key, is(not(nullValue())));
    }

    @Test
    public void testGeneratingSymmetricKeyWithInvalidAlgorithm() {
        thrown.expect(CryptoException.class);
        thrown.expectMessage("RSA KeyGenerator not available");

        crypto.generateSymmetricKey(KeyAlgorithm.RSA_2048);
    }

    @Test
    public void testGeneratingAsymmetricKeyWithInvalidAlgorithm() {
        thrown.expect(CryptoException.class);
        thrown.expectMessage("AES KeyPairGenerator not available");

        crypto.generateAsymmetricKey(KeyAlgorithm.AES_CBC_128);
    }

    @Test
    public void testGeneratingChecksumWithInvalidAlgorithm() {
        thrown.expect(CryptoException.class);
        thrown.expectMessage("No enum constant io.javadog.cws.core.enums.HashAlgorithm.AES_128");

        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.HASH_ALGORITHM.getKey(), "AES_128");
        final Crypto myCrypto = new Crypto(mySettings);
        myCrypto.generateChecksum("Bla bla bla".getBytes(settings.getCharset()));
    }

    @Test
    public void testSigningWithInvalidAlgorithm() {
        thrown.expect(CryptoException.class);
        thrown.expectMessage("AES/CBC/PKCS5Padding Signature not available");

        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SIGNATURE_ALGORITHM.getKey(), "AES_CBC_256");
        final Crypto myCrypto = new Crypto(mySettings);
        final CWSKeyPair key = myCrypto.generateAsymmetricKey(KeyAlgorithm.RSA_2048);
        myCrypto.sign(key.getPrivate().getKey(), "bla bla bla".getBytes(mySettings.getCharset()));
    }

    @Test
    public void testSignature() {
        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final byte[] message = "Message to Sign".getBytes(settings.getCharset());
        final byte[] signature = crypto.sign(keyPair.getPrivate().getKey(), message);
        final boolean verified = crypto.verify(keyPair.getPublic().getKey(), message, signature);

        assertThat(verified, is(true));
    }

    /**
     * The Public Key is stored armored in the database, meaning converted into
     * a Base64 encoded String, which can easily be read out again.
     */
    @Test
    public void testArmoringPublicKey() {
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
        final String password = "MySuperSecretPassword";
        final String salt = UUID.randomUUID().toString();
        final SecretCWSKey cryptoKeys = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes(password), salt);
        cryptoKeys.setSalt(new IVSalt(UUID.randomUUID().toString()));
        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredKey = crypto.armoringPrivateKey(cryptoKeys, keyPair.getPrivate().getKey());
        final PrivateKey dearmoredKey = crypto.dearmoringPrivateKey(cryptoKeys, armoredKey);

        assertThat(keyPair.getPrivate().getKey(), is(dearmoredKey));
    }

    @Test
    public void testArmoringAsymmetricKey() {
        final String secret = "MySuperSecretPassword";
        final String salt = UUID.randomUUID().toString();
        final SecretCWSKey secretKey = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes(secret), salt);
        secretKey.setSalt(new IVSalt(salt));
        final CWSKeyPair pair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredPublicKey = crypto.armoringPublicKey(pair.getPublic().getKey());
        final String armoredPrivateKey = crypto.armoringPrivateKey(secretKey, pair.getPrivate().getKey());

        final CWSKeyPair dearmoredPair = crypto.extractAsymmetricKey(pair.getAlgorithm(), secretKey, salt, armoredPublicKey, armoredPrivateKey);
        assertThat(dearmoredPair.getAlgorithm(), is(pair.getAlgorithm()));
        assertThat(dearmoredPair.getPublic().getKey(), is(pair.getPublic().getKey()));
        assertThat(dearmoredPair.getPrivate().getKey(), is(pair.getPrivate().getKey()));
        assertThat(dearmoredPair.getPublic().getKey().hashCode(), is(pair.getPublic().getKey().hashCode()));
    }

    /**
     * There's two types of Cryptography applied in CWS. This test will
     * demonstrate the Symmetric Encryption part, which is used for all Data
     * shared within Circles.
     */
    @Test
    public void testObjectEncryption() {
        final SecretCWSKey key = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        key.setSalt(new IVSalt(UUID.randomUUID().toString()));

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
        final String password = "MySuperSecretPassword";
        final String salt = "SystemSpecificSalt";
        final SecretCWSKey key = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes(password), salt);

        // Now, we're going to encrypt some data
        key.setSalt(new IVSalt(UUID.randomUUID().toString()));
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

        final String dataSalt = UUID.randomUUID().toString();
        final SecretCWSKey key = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        key.setSalt(new IVSalt(dataSalt));
        final byte[] rawdata = generateData(1048576);
        final byte[] encryptedData = crypto.encrypt(key, rawdata);

        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final String armoredCircleKey = crypto.encryptAndArmorCircleKey(keyPair.getPublic(), key);
        final SecretCWSKey circleKey = crypto.extractCircleKey(key.getAlgorithm(), keyPair.getPrivate(), armoredCircleKey);
        circleKey.setSalt(new IVSalt(dataSalt));
        final byte[] decryptedData = crypto.decrypt(circleKey, encryptedData);

        assertThat(decryptedData, is(rawdata));
    }

    @Test
    public void testStringToBytesConversion() {
        final Settings mySettings = newSettings();
        final Crypto myCrypto = new Crypto(mySettings);
        final String str = "Alpha Beta æøåßöäÿ";

        final String garbage = "INVALID_ENCODING";
        mySettings.set(StandardSetting.CWS_CHARSET.getKey(), garbage);

        prepareCause(CWSException.class, ReturnCode.SETTING_ERROR, "java.nio.charset.UnsupportedCharsetException: " + garbage);
        assertThat(str, is(not(nullValue())));
        myCrypto.stringToBytes(str);
    }

    @Test
    public void testBytesToStringConversion() {
        final Settings mySettings = newSettings();
        final Crypto myCrypto = new Crypto(mySettings);
        final String str = "Alpha Beta æøåßöäÿ";
        final byte[] bytes = str.getBytes(mySettings.getCharset());
        final String garbage = "INVALID_ENCODING";
        mySettings.set(StandardSetting.CWS_CHARSET.getKey(), garbage);

        prepareCause(CWSException.class, ReturnCode.SETTING_ERROR, "UnsupportedCharsetException: " + garbage);
        assertThat(bytes, is(not(nullValue())));
        final String reversed = myCrypto.bytesToString(bytes);
        assertThat(reversed, is(str));
    }

    @Test
    public void testDestroyingKeys() {
        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final PrivateCWSKey privateKey = keyPair.getPrivate();
        final SecretCWSKey secretKey = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());

        assertThat(privateKey.isDestroyed(), is(false));
        assertThat(secretKey.isDestroyed(), is(false));

        // First attempt at destroying should also update the flag
        privateKey.destroy();
        secretKey.destroy();
        assertThat(privateKey.isDestroyed(), is(true));
        assertThat(secretKey.isDestroyed(), is(true));

        // Second attempt at destroying should be ignored
        privateKey.destroy();
        secretKey.destroy();
        assertThat(privateKey.isDestroyed(), is(true));
        assertThat(secretKey.isDestroyed(), is(true));
    }

    @Test
    public void testInvalidSymmetricKeyEncryption() throws NoSuchAlgorithmException {
        prepareCause(CryptoException.class, ReturnCode.CRYPTO_ERROR, "No installed provider supports this key: javax.crypto.spec.SecretKeySpec");

        final SecretCWSKey key = prepareSecretCwsKey();
        final byte[] data = generateData(524288);

        final byte[] encrypted = crypto.encrypt(key, data);
        assertThat(encrypted.length > 0, is(true));
    }

    @Test
    public void testInvalidSymmetricKeyDecryption() throws NoSuchAlgorithmException {
        prepareCause(CryptoException.class, ReturnCode.CRYPTO_ERROR, "No installed provider supports this key: javax.crypto.spec.SecretKeySpec");

        final SecretCWSKey key = prepareSecretCwsKey();
        final byte[] data = generateData(524288);

        final byte[] decrypted = crypto.decrypt(key, data);
        assertThat(decrypted.length > 0, is(true));
    }

    @Test
    public void testInvalidAsymmetricKeyEncryption() {
        // Java 8 & Java 11 are providing different messages. to make sure that
        // CWS builds under Java 11, the expected error message must be
        // corrected.
        //    8: io.javadog.cws.core.jce.PublicCWSKey cannot be cast to io.javadog.cws.core.jce.SecretCWSKey
        //   11: class io.javadog.cws.core.jce.PublicCWSKey cannot be cast to class io.javadog.cws.core.jce.SecretCWSKey (io.javadog.cws.core.jce.PublicCWSKey and io.javadog.cws.core.jce.SecretCWSKey are in unnamed module of loader 'app')
        prepareCause(CryptoException.class, ReturnCode.CRYPTO_ERROR, "io.javadog.cws.core.jce.PublicCWSKey cannot be cast");

        final CWSKeyPair keyPair = generateKeyPair();
        final byte[] data = generateData(524288);

        final byte[] encrypted = crypto.encrypt(keyPair.getPublic(), data);
        assertThat(encrypted.length > 0, is(true));
    }

    @Test
    public void testInvalidAsymmetricKeyDecryption() {
        // Java 8 & Java 11 are providing different messages. to make sure that
        // CWS builds under Java 11, the expected error message must be
        // corrected.
        //    8: io.javadog.cws.core.jce.PrivateCWSKey cannot be cast to io.javadog.cws.core.jce.SecretCWSKey
        //   11: class io.javadog.cws.core.jce.PrivateCWSKey cannot be cast to class io.javadog.cws.core.jce.SecretCWSKey (io.javadog.cws.core.jce.PrivateCWSKey and io.javadog.cws.core.jce.SecretCWSKey are in unnamed module of loader 'app')
        prepareCause(CryptoException.class, ReturnCode.CRYPTO_ERROR, "io.javadog.cws.core.jce.PrivateCWSKey cannot be cast");

        final CWSKeyPair keyPair = generateKeyPair();
        final byte[] data = generateData(524288);

        final byte[] decrypted = crypto.decrypt(keyPair.getPrivate(), data);
        assertThat(decrypted.length > 0, is(true));
    }

    @Test
    public void testInvalidDearmoringPublicKey() {
        prepareCause(CryptoException.class, ReturnCode.CRYPTO_ERROR, "AES KeyFactory not available");

        final Settings mySettings = newSettings();
        final Crypto myCrypto = new Crypto(mySettings);
        final CWSKeyPair keyPair = myCrypto.generateAsymmetricKey(mySettings.getAsymmetricAlgorithm());
        final String armoredPublicKey = myCrypto.armoringPublicKey(keyPair.getPublic().getKey());

        mySettings.set(StandardSetting.ASYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.AES_CBC_128.name());
        final PublicKey publicKey = myCrypto.dearmoringPublicKey(armoredPublicKey);
        assertThat(publicKey, is(not(nullValue())));
    }

    @Test
    public void testInvalidDearmoringPrivateKey() {
        prepareCause(CryptoException.class, ReturnCode.CRYPTO_ERROR, "AES KeyFactory not available");

        final Settings mySettings = newSettings();
        final Crypto myCrypto = new Crypto(mySettings);
        final CWSKeyPair keyPair = myCrypto.generateAsymmetricKey(mySettings.getAsymmetricAlgorithm());
        final SecretCWSKey secretKey = myCrypto.generateSymmetricKey(mySettings.getSymmetricAlgorithm());
        secretKey.setSalt(new IVSalt(UUID.randomUUID().toString()));

        final String armoredPrivateKey = myCrypto.armoringPrivateKey(secretKey, keyPair.getPrivate().getKey());
        mySettings.set(StandardSetting.ASYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.AES_CBC_128.name());
        final PrivateKey privateKey = myCrypto.dearmoringPrivateKey(secretKey, armoredPrivateKey);
        assertThat(privateKey, is(not(nullValue())));
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private SecretCWSKey prepareSecretCwsKey() throws NoSuchAlgorithmException {
        final KeyAlgorithm algorithm = settings.getSymmetricAlgorithm();
        final KeyGenerator generator = KeyGenerator.getInstance(algorithm.getName());
        generator.init(algorithm.getLength());

        final SecretKey secretKey = generator.generateKey();
        final SecretCWSKey key = new SecretCWSKey(settings.getAsymmetricAlgorithm(), secretKey);
        key.setSalt(new IVSalt(UUID.randomUUID().toString()));

        return key;
    }

    private CWSKeyPair generateKeyPair() {
        final CWSKeyPair generated = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final KeyPair keys = new KeyPair(generated.getPublic().getKey(), generated.getPrivate().getKey());
        final CWSKeyPair keyPair = new CWSKeyPair(settings.getSymmetricAlgorithm(), keys);

        return keyPair;
    }
}
