/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.core.jce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

/**
 * Testing the simple Cryptographic Operations.
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class CryptoTest extends DatabaseSetup {

    @Test
    void testGCM128Encryption() {
        final String cleartext = "This is just an example";

        final KeyAlgorithm algorithm = KeyAlgorithm.AES_GCM_128;
        final SecretCWSKey key = crypto.generateSymmetricKey(algorithm);
        final IVSalt ivSalt = new IVSalt();
        key.setSalt(ivSalt);

        final byte[] cleartextBytes = crypto.stringToBytes(cleartext);
        final byte[] encryptedBytes = crypto.encrypt(key, cleartextBytes);
        final byte[] decryptedBytes = crypto.decrypt(key, encryptedBytes);
        final String decrypted = crypto.bytesToString(decryptedBytes);

        assertEquals(cleartext, decrypted);
    }

    @Test
    void testShaEncryption() {
        final KeyAlgorithm algorithm = KeyAlgorithm.SHA_256;
        final PublicCWSKey key = crypto.generateAsymmetricKey(KeyAlgorithm.RSA_2048).getPublic();
        final PublicCWSKey fakeKey = new PublicCWSKey(algorithm, key.getKey());
        final byte[] toEncrypt = { (byte) 1, (byte) 2, (byte) 3, (byte) 4 };

        final CWSException cause = assertThrows(CWSException.class, () -> crypto.encrypt(fakeKey, toEncrypt));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("Cannot prepare Cipher for this Algorithm Type SIGNATURE.", cause.getMessage());
    }

    @Test
    void testGeneratingPasswordKeyWithInvalidAlgorithm() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.PBE_ALGORITHM.getKey(), "RSA_2048");
        final Crypto myCrypto = new Crypto(mySettings);
        final String salt = UUID.randomUUID().toString();

        final CWSException cause = assertThrows(CWSException.class, () -> myCrypto.generatePasswordKey(KeyAlgorithm.AES_CBC_128, crypto.stringToBytes("my secret"), salt));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("AES/CBC/PKCS5Padding SecretKeyFactory not available", cause.getMessage());
    }

    @Test
    void testPasswordWithWeirdCharacters() {
        final byte[] secret = new byte[256];
        for (int i = 0; i < 256; i++) {
            secret[i] = (byte) i;
        }
        final KeyAlgorithm algorithm = KeyAlgorithm.PBE_128;
        final String salt = UUID.randomUUID().toString();
        final SecretCWSKey key = crypto.generatePasswordKey(algorithm, secret, salt);
        assertNotNull(key);
    }

    @Test
    void testGeneratingSymmetricKeyWithInvalidAlgorithm() {
        final CWSException cause = assertThrows(CWSException.class, () -> crypto.generateSymmetricKey(KeyAlgorithm.RSA_2048));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("RSA KeyGenerator not available", cause.getMessage());
    }

    @Test
    void testGeneratingAsymmetricKeyWithInvalidAlgorithm() {
        final CWSException cause = assertThrows(CWSException.class, () -> crypto.generateAsymmetricKey(KeyAlgorithm.AES_CBC_128));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("AES KeyPairGenerator not available", cause.getMessage());
    }

    @Test
    void testGeneratingChecksumWithInvalidAlgorithm() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.HASH_ALGORITHM.getKey(), "AES_128");
        final Crypto myCrypto = new Crypto(mySettings);

        final CWSException cause = assertThrows(CWSException.class, () -> myCrypto.generateChecksum("Bla bla bla".getBytes(settings.getCharset())));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("No enum constant io.javadog.cws.core.enums.HashAlgorithm.AES_128", cause.getMessage());
    }

    @Test
    void testSigningWithInvalidAlgorithm() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SIGNATURE_ALGORITHM.getKey(), "AES_CBC_256");
        final Crypto myCrypto = new Crypto(mySettings);
        final CWSKeyPair key = myCrypto.generateAsymmetricKey(KeyAlgorithm.RSA_2048);

        final CWSException cause = assertThrows(CWSException.class, () ->  myCrypto.sign(key.getPrivate().getKey(), "bla bla bla".getBytes(mySettings.getCharset())));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("AES/CBC/PKCS5Padding Signature not available", cause.getMessage());
    }

    @Test
    void testSignature() {
        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final byte[] message = "Message to Sign".getBytes(settings.getCharset());
        final byte[] signature = crypto.sign(keyPair.getPrivate().getKey(), message);
        final boolean verified = crypto.verify(keyPair.getPublic().getKey(), message, signature);

        assertTrue(verified);
    }

    /**
     * The Public Key is stored armored in the database, meaning converted into
     * a Base64 encoded String, which can easily be read out again.
     */
    @Test
    void testArmoringPublicKey() {
        final CWSKeyPair key = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredKey = crypto.armoringPublicKey(key.getPublic().getKey());
        final PublicKey dearmoredKey = crypto.dearmoringPublicKey(armoredKey);

        assertEquals(key.getPublic().getKey(), dearmoredKey);
    }

    /**
     * The Private Key of a Member, is stored encrypted using a Member provided
     * passphrase which is turned into a Key. The PBE based Key is generated
     * using both a Member Salt and System Salt.
     */
    @Test
    void testArmoringPrivateKey() {
        final String password = "MySuperSecretPassword";
        final String salt = UUID.randomUUID().toString();
        final SecretCWSKey cryptoKeys = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes(password), salt);
        cryptoKeys.setSalt(new IVSalt(UUID.randomUUID().toString()));
        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredKey = crypto.armoringPrivateKey(cryptoKeys, keyPair.getPrivate().getKey());
        final PrivateKey dearmoredKey = crypto.dearmoringPrivateKey(cryptoKeys, armoredKey);

        assertEquals(dearmoredKey, keyPair.getPrivate().getKey());
    }

    @Test
    void testArmoringAsymmetricKey() {
        final String secret = "MySuperSecretPassword";
        final String salt = UUID.randomUUID().toString();
        final SecretCWSKey secretKey = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes(secret), salt);
        secretKey.setSalt(new IVSalt(salt));
        final CWSKeyPair pair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredPublicKey = crypto.armoringPublicKey(pair.getPublic().getKey());
        final String armoredPrivateKey = crypto.armoringPrivateKey(secretKey, pair.getPrivate().getKey());

        final CWSKeyPair dearmoredPair = crypto.extractAsymmetricKey(pair.getAlgorithm(), secretKey, salt, armoredPublicKey, armoredPrivateKey);
        assertEquals(pair.getAlgorithm(), dearmoredPair.getAlgorithm());
        assertEquals(pair.getPublic().getKey(), dearmoredPair.getPublic().getKey());
        assertEquals(pair.getPrivate().getKey(), dearmoredPair.getPrivate().getKey());
        assertEquals(pair.getPublic().getKey().hashCode(), dearmoredPair.getPublic().getKey().hashCode());
    }

    /**
     * There's two types of Cryptography applied in CWS. This test will
     * demonstrate the Symmetric Encryption part, which is used for all Data
     * shared within Circles.
     */
    @Test
    void testObjectEncryption() {
        final SecretCWSKey key = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        key.setSalt(new IVSalt(UUID.randomUUID().toString()));

        // Now, we're going to encrypt some data
        final String cleartext = "This is just an example";
        final byte[] toEncrypt = crypto.stringToBytes(cleartext);
        final byte[] encrypted = crypto.encrypt(key, toEncrypt);

        // And decrypt it so we can verify it
        final byte[] decrypted = crypto.decrypt(key, encrypted);
        final String result = crypto.bytesToString(decrypted);

        assertEquals(cleartext, result);
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
    void testMemberEncryption() {
        final CWSKeyPair key = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String cleartext = "This is just an example";
        final byte[] toEncrypt = crypto.stringToBytes(cleartext);
        final byte[] encrypted = crypto.encrypt(key.getPublic(), toEncrypt);

        final byte[] decrypted = crypto.decrypt(key.getPrivate(), encrypted);
        final String result = crypto.bytesToString(decrypted);

        assertEquals(cleartext, result);
    }

    /**
     * Destruction of the Symmetric Keys is important - to ensure that the keys
     * cannot be reused.
     */
    @Test
    void testSymmetricKeyDestruction() {
        final SecretCWSKey key = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        key.setSalt(new IVSalt(UUID.randomUUID().toString()));

        // Now, we're going to encrypt some data
        final String cleartext = "This is just an example";
        final byte[] toEncrypt = crypto.stringToBytes(cleartext);
        final byte[] encrypted = crypto.encrypt(key, toEncrypt);

        // Destroy the key and try to decrypt. Should fail!
        key.destroy();
        assertThrows(NullPointerException.class, () -> crypto.decrypt(key, encrypted));
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
    void testPasswordToKey() {
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

        assertEquals(cleartext, result);
    }

    /**
     * <p>Circles have a Key generated, which is stored encrypted per Trustee,
     * i.e. Member with access to the Circle. The Circle Key is encrypted using
     * the Member's Public Key, and can be decrypted using the Member's Private
     * Key which again is unlocked during the Authentication Process.</p>
     */
    @Test
    void testMemberAccessCircleKey() {
        // Added this stupid assertion, as SonarQube failed to detect the
        // assertion at the end of the test.
        assertFalse(Boolean.parseBoolean("Is SonarQube rule squid:S2699 working correctly ?"));

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

        assertArrayEquals(rawdata, decryptedData);
    }

    @Test
    void testStringToBytesConversion() {
        final Settings mySettings = newSettings();
        final Crypto myCrypto = new Crypto(mySettings);
        final String str = "Alpha Beta æøåßöäÿ";

        final String garbage = "INVALID_ENCODING";
        mySettings.set(StandardSetting.CWS_CHARSET.getKey(), garbage);

        final CWSException cause = assertThrows(CWSException.class, () -> myCrypto.stringToBytes(str));
        assertTrue(cause.getMessage().contains("java.nio.charset.UnsupportedCharsetException: " + garbage));
        assertEquals(ReturnCode.SETTING_ERROR, cause.getReturnCode());
    }

    @Test
    void testBytesToStringConversion() {
        final Settings mySettings = newSettings();
        final Crypto myCrypto = new Crypto(mySettings);
        final String str = "Alpha Beta æøåßöäÿ";
        final byte[] bytes = str.getBytes(mySettings.getCharset());
        final String garbage = "INVALID_ENCODING";
        mySettings.set(StandardSetting.CWS_CHARSET.getKey(), garbage);

        final CWSException cause = assertThrows(CWSException.class, () -> myCrypto.bytesToString(bytes));
        assertEquals(ReturnCode.SETTING_ERROR, cause.getReturnCode());
        assertTrue(cause.getMessage().contains("UnsupportedCharsetException: " + garbage));
    }

    @Test
    void testDestroyingKeys() {
        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final PrivateCWSKey privateKey = keyPair.getPrivate();
        final SecretCWSKey secretKey = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());

        assertFalse(privateKey.isDestroyed());
        assertFalse(secretKey.isDestroyed());

        // First attempt at destroying should also update the flag
        privateKey.destroy();
        secretKey.destroy();
        assertTrue(privateKey.isDestroyed());
        assertTrue(secretKey.isDestroyed());

        // Second attempt at destroying should be ignored
        privateKey.destroy();
        secretKey.destroy();
        assertTrue(privateKey.isDestroyed());
        assertTrue(secretKey.isDestroyed());
    }

    @Test
    void testInvalidSymmetricKeyEncryption() throws NoSuchAlgorithmException {
        final SecretCWSKey key = prepareSecretCwsKey();
        final byte[] data = generateData(524288);

        final CWSException cause = assertThrows(CWSException.class, () -> crypto.encrypt(key, data));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("No installed provider supports this key: javax.crypto.spec.SecretKeySpec", cause.getMessage());
    }

    @Test
    void testInvalidSymmetricKeyDecryption() throws NoSuchAlgorithmException {
        final SecretCWSKey key = prepareSecretCwsKey();
        final byte[] data = generateData(524288);

        final CWSException cause = assertThrows(CWSException.class, () -> crypto.decrypt(key, data));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("No installed provider supports this key: javax.crypto.spec.SecretKeySpec", cause.getMessage());
    }

    @Test
    void testInvalidAsymmetricKeyEncryption() {
        final CWSKeyPair keyPair = generateKeyPair();
        final byte[] data = generateData(524288);

        final CWSException cause = assertThrows(CWSException.class, () -> crypto.encrypt(keyPair.getPublic(), data));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        // Java 8 & Java 11 are providing different messages. to make sure that
        // CWS builds under Java 11, the expected error message must be
        // corrected.
        //  OpenJDK  8: io.javadog.cws.core.jce.PublicCWSKey cannot be cast to io.javadog.cws.core.jce.SecretCWSKey
        //  OpenJDK 11: class io.javadog.cws.core.jce.PublicCWSKey cannot be cast to class io.javadog.cws.core.jce.SecretCWSKey (io.javadog.cws.core.jce.PublicCWSKey and io.javadog.cws.core.jce.SecretCWSKey are in unnamed module of loader 'app')
        //  AdoptOpenJDK 11: io.javadog.cws.core.jce.PublicCWSKey incompatible with io.javadog.cws.core.jce.SecretCWSKey
        assertTrue(cause.getMessage().contains("io.javadog.cws.core.jce.PublicCWSKey"));
    }

    @Test
    void testInvalidAsymmetricKeyDecryption() {
        final CWSKeyPair keyPair = generateKeyPair();
        final byte[] data = generateData(524288);

        final CWSException cause = assertThrows(CWSException.class, () -> crypto.decrypt(keyPair.getPrivate(), data));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        // Java 8 & Java 11 are providing different messages. to make sure that
        // CWS builds under Java 11, the expected error message must be
        // corrected.
        //   OpenJDK  8: io.javadog.cws.core.jce.PrivateCWSKey cannot be cast to io.javadog.cws.core.jce.SecretCWSKey
        //   OpenJDK 11: class io.javadog.cws.core.jce.PrivateCWSKey cannot be cast to class io.javadog.cws.core.jce.SecretCWSKey (io.javadog.cws.core.jce.PrivateCWSKey and io.javadog.cws.core.jce.SecretCWSKey are in unnamed module of loader 'app')
        //   AdoptOpenJDK 11: io.javadog.cws.core.jce.PrivateCWSKey incompatible with io.javadog.cws.core.jce.SecretCWSKey
        assertTrue(cause.getMessage().contains("io.javadog.cws.core.jce.PrivateCWSKey"));
    }

    @Test
    void testInvalidDearmoringPublicKey() {
        final Settings mySettings = newSettings();
        final Crypto myCrypto = new Crypto(mySettings);
        final CWSKeyPair keyPair = myCrypto.generateAsymmetricKey(mySettings.getAsymmetricAlgorithm());
        final String armoredPublicKey = myCrypto.armoringPublicKey(keyPair.getPublic().getKey());

        mySettings.set(StandardSetting.ASYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.AES_CBC_128.name());

        final CWSException cause = assertThrows(CWSException.class, () -> myCrypto.dearmoringPublicKey(armoredPublicKey));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("AES KeyFactory not available", cause.getMessage());
    }

    @Test
    void testInvalidDearmoringPrivateKey() {
        final Settings mySettings = newSettings();
        final Crypto myCrypto = new Crypto(mySettings);
        final CWSKeyPair keyPair = myCrypto.generateAsymmetricKey(mySettings.getAsymmetricAlgorithm());
        final SecretCWSKey secretKey = myCrypto.generateSymmetricKey(mySettings.getSymmetricAlgorithm());
        secretKey.setSalt(new IVSalt(UUID.randomUUID().toString()));

        final String armoredPrivateKey = myCrypto.armoringPrivateKey(secretKey, keyPair.getPrivate().getKey());
        mySettings.set(StandardSetting.ASYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.AES_CBC_128.name());

        final CWSException cause = assertThrows(CWSException.class, () -> myCrypto.dearmoringPrivateKey(secretKey, armoredPrivateKey));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("AES KeyFactory not available", cause.getMessage());
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
