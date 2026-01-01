/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.jce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * Testing the simple Cryptographic Operations.
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class CryptoTest extends DatabaseSetup {

    @Test
    void testGCM128Encryption() {
        final String clearText = "This is just an example";

        final KeyAlgorithm algorithm = KeyAlgorithm.AES_GCM_128;
        final SecretEDSKey key = Crypto.generateSymmetricKey(algorithm);
        final IVSalt ivSalt = new IVSalt();
        key.setSalt(ivSalt);

        final byte[] clearTextBytes = crypto.stringToBytes(clearText);
        final byte[] encryptedBytes = Crypto.encrypt(key, clearTextBytes);
        final byte[] decryptedBytes = Crypto.decrypt(key, encryptedBytes);
        final String decrypted = crypto.bytesToString(decryptedBytes);

        assertEquals(clearText, decrypted);
    }

    @Test
    void testShaEncryption() {
        final KeyAlgorithm algorithm = KeyAlgorithm.SHA_256;
        final PublicEDSKey key = Crypto.generateAsymmetricKey(KeyAlgorithm.RSA_2048).getPublic();
        final PublicEDSKey fakeKey = new PublicEDSKey(algorithm, key.getKey());
        final byte[] toEncrypt = { (byte) 1, (byte) 2, (byte) 3, (byte) 4 };

        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.encrypt(fakeKey, toEncrypt));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("Cannot prepare Cipher for this Algorithm Type SIGNATURE.", cause.getMessage());
    }

    @Test
    void testGeneratingPasswordKeyWithInvalidAlgorithm() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.PBE_ALGORITHM.getKey(), "RSA_2048");
        final Crypto myCrypto = new Crypto(mySettings);
        final String salt = UUID.randomUUID().toString();
        final byte[] bytes = crypto.stringToBytes("my secret");

        final EDSException cause = assertThrows(EDSException.class, () -> myCrypto.generatePasswordKey(KeyAlgorithm.AES_CBC_128, bytes, salt));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("AES/CBC/PKCS5Padding SecretKeyFactory not available", cause.getMessage());
    }

    @Test
    void testPasswordWithWeirdCharacters() {
        final byte[] secret = new byte[256];
        for (int i = 0; i < 256; i++) {
            secret[i] = (byte) i;
        }
        final KeyAlgorithm algorithm = KeyAlgorithm.PBE_CBC_128;
        final String salt = UUID.randomUUID().toString();
        final SecretEDSKey key = crypto.generatePasswordKey(algorithm, secret, salt);
        assertNotNull(key);
    }

    @Test
    void testGeneratingSymmetricKeyWithInvalidAlgorithm() {
        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.generateSymmetricKey(KeyAlgorithm.RSA_2048));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("no such algorithm: RSA for provider SunJCE", cause.getMessage());
    }

    @Test
    void testGeneratingAsymmetricKeyWithInvalidAlgorithm() {
        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.generateAsymmetricKey(KeyAlgorithm.AES_CBC_128));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("AES KeyPairGenerator not available", cause.getMessage());
    }

    @Test
    void testGeneratingChecksumWithInvalidAlgorithm() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.HASH_ALGORITHM.getKey(), "AES_128");
        final Crypto myCrypto = new Crypto(mySettings);
        final byte[] bytes = "Bla bla bla".getBytes(settings.getCharset());

        final EDSException cause = assertThrows(EDSException.class, () -> myCrypto.generateChecksum(bytes));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertTrue(cause.getMessage().contains("HashAlgorithm.AES_128"));
    }

    @Test
    void testSigningWithInvalidAlgorithm() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SIGNATURE_ALGORITHM.getKey(), "AES_CBC_256");
        final Crypto myCrypto = new Crypto(mySettings);
        final EDSKeyPair key = Crypto.generateAsymmetricKey(KeyAlgorithm.RSA_2048);
        final PrivateKey privateKey = key.getPrivate().getKey();
        final byte[] bytes = "bla bla bla".getBytes(mySettings.getCharset());

        final EDSException cause = assertThrows(EDSException.class, () -> myCrypto.sign(privateKey, bytes));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("AES/CBC/PKCS5Padding Signature not available", cause.getMessage());
    }

    @Test
    void testSignature() {
        final EDSKeyPair keyPair = Crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
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
        final EDSKeyPair key = Crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredKey = Crypto.armoringPublicKey(key.getPublic().getKey());
        final PublicKey dearmoredKey = crypto.dearmoringPublicKey(armoredKey);

        assertEquals(key.getPublic().getKey(), dearmoredKey);
    }

    /**
     * The Private Key of a Member is stored encrypted using a Member-provided
     * passphrase which is turned into a Key. The PBE-based Key is generated
     * using both a Member Salt and System Salt.
     */
    @Test
    void testArmoringPrivateKey() {
        final String password = "MySuperSecretPassword";
        final String salt = UUID.randomUUID().toString();
        final SecretEDSKey cryptoKeys = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes(password), salt);
        cryptoKeys.setSalt(new IVSalt(UUID.randomUUID().toString()));
        final EDSKeyPair keyPair = Crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredKey = Crypto.encryptAndArmorPrivateKey(cryptoKeys, keyPair.getPrivate().getKey());
        final PrivateKey dearmoredKey = crypto.dearmoringPrivateKey(cryptoKeys, armoredKey);

        assertEquals(dearmoredKey, keyPair.getPrivate().getKey());
    }

    @Test
    void testArmoringAsymmetricKey() {
        final String secret = "MySuperSecretPassword";
        final String salt = UUID.randomUUID().toString();
        final SecretEDSKey secretKey = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes(secret), salt);
        secretKey.setSalt(new IVSalt(salt));
        final EDSKeyPair pair = Crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String armoredPublicKey = Crypto.armoringPublicKey(pair.getPublic().getKey());
        final String armoredPrivateKey = Crypto.encryptAndArmorPrivateKey(secretKey, pair.getPrivate().getKey());

        final EDSKeyPair dearmoredPair = crypto.extractAsymmetricKey(pair.getAlgorithm(), secretKey, salt, armoredPublicKey, armoredPrivateKey);
        assertEquals(pair.getAlgorithm(), dearmoredPair.getAlgorithm());
        assertEquals(pair.getPublic().getKey(), dearmoredPair.getPublic().getKey());
        assertEquals(pair.getPrivate().getKey(), dearmoredPair.getPrivate().getKey());
        assertEquals(pair.getPublic().getKey().hashCode(), dearmoredPair.getPublic().getKey().hashCode());
    }

    /**
     * There are two types of Cryptography applied in EDS. This test will
     * demonstrate the Symmetric Encryption part, which is used for all Data
     * shared within Circles.
     */
    @Test
    void testObjectEncryption() {
        final SecretEDSKey key = Crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        key.setSalt(new IVSalt(UUID.randomUUID().toString()));

        // Now, we're going to encrypt some data
        final String clearText = "This is just an example";
        final byte[] toEncrypt = crypto.stringToBytes(clearText);
        final byte[] encrypted = Crypto.encrypt(key, toEncrypt);

        // And decrypt it so we can verify it
        final byte[] decrypted = Crypto.decrypt(key, encrypted);
        final String result = crypto.bytesToString(decrypted);

        assertEquals(clearText, result);
    }

    /**
     * For the Group Members, we're storing the Symmetric Key per Member using
     * Asymmetric Encryption - this will allow that Group Members can be changed
     * independently of the Group Data. The Member's Public Key is stored with
     * the Member, encrypted using the Member's Public Key. The Private Key,
     * which may be stored elsewhere or additionally encrypted, can then be used
     * by the Member to access the data.
     */
    @Test
    void testMemberEncryption() {
        final EDSKeyPair key = Crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());

        final String clearText = "This is just an example";
        final byte[] toEncrypt = crypto.stringToBytes(clearText);
        final byte[] encrypted = Crypto.encrypt(key.getPublic(), toEncrypt);

        final byte[] decrypted = Crypto.decrypt(key.getPrivate(), encrypted);
        final String result = crypto.bytesToString(decrypted);

        assertEquals(clearText, result);
    }

    /**
     * <p>Members of a Circle must have both a Public and a Private Key. If they
     * are not providing a Private Key as part of initializing a Session, we
     * need a different way to retrieve it. We can, of course, generate a Key
     * Pair and store with the Member information, but storing the Private key
     * thus can hardly be considered a good idea! So, instead we need to
     * encrypt it and store it so.</p>
     *
     * <p>This means that we must take secret information provided by the
     * Member and convert this into a Key, which we can then use. The standard
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
        final SecretEDSKey key = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes(password), salt);

        // Now, we're going to encrypt some data
        key.setSalt(new IVSalt(UUID.randomUUID().toString()));
        final String clearText = "This is just an example";
        final byte[] encrypted = Crypto.encrypt(key, crypto.stringToBytes(clearText));

        // And decrypt it so we can verify it
        final byte[] decrypted = Crypto.decrypt(key, encrypted);
        final String result = crypto.bytesToString(decrypted);

        assertEquals(clearText, result);
    }

    /**
     * <p>Circles have a Key generated, which is stored encrypted per Trustee.
     * I.e., Member with access to the Circle. The Circle Key is encrypted using
     * the Member's Public Key and can be decrypted using the Member's Private
     * Key, which again is unlocked during the Authentication Process.</p>
     */
    @Test
    void testMemberAccessCircleKey() {
        final String dataSalt = UUID.randomUUID().toString();
        final SecretEDSKey key = Crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        key.setSalt(new IVSalt(dataSalt));
        final byte[] rawData = generateData(1048576);
        final byte[] encryptedData = Crypto.encrypt(key, rawData);

        final EDSKeyPair keyPair = Crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final String armoredCircleKey = Crypto.encryptAndArmorCircleKey(keyPair.getPublic(), key);
        final SecretEDSKey circleKey = Crypto.extractCircleKey(key.getAlgorithm(), keyPair.getPrivate(), armoredCircleKey);
        circleKey.setSalt(new IVSalt(dataSalt));
        final byte[] decryptedData = Crypto.decrypt(circleKey, encryptedData);

        assertArrayEquals(rawData, decryptedData);
    }

    @Test
    void testStringToBytesConversion() {
        final Settings mySettings = newSettings();
        final Crypto myCrypto = new Crypto(mySettings);
        final String str = "Alpha Beta æøåßöäÿ";

        final String garbage = "INVALID_ENCODING";
        mySettings.set(StandardSetting.EDS_CHARSET.getKey(), garbage);

        final EDSException cause = assertThrows(EDSException.class, () -> myCrypto.stringToBytes(str));
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
        mySettings.set(StandardSetting.EDS_CHARSET.getKey(), garbage);

        final EDSException cause = assertThrows(EDSException.class, () -> myCrypto.bytesToString(bytes));
        assertEquals(ReturnCode.SETTING_ERROR, cause.getReturnCode());
        assertTrue(cause.getMessage().contains("UnsupportedCharsetException: " + garbage));
    }

    @Test
    void testInvalidSymmetricKeyEncryption() throws NoSuchAlgorithmException {
        final SecretEDSKey key = prepareSecretEDSKey();
        final byte[] data = generateData(524288);

        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.encrypt(key, data));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("No installed provider supports this key: javax.crypto.spec.SecretKeySpec", cause.getMessage());
    }

    @Test
    void testInvalidSymmetricKeyDecryption() throws NoSuchAlgorithmException {
        final SecretEDSKey key = prepareSecretEDSKey();
        final byte[] data = generateData(524288);

        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.decrypt(key, data));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("No installed provider supports this key: javax.crypto.spec.SecretKeySpec", cause.getMessage());
    }

    @Test
    void testInvalidAsymmetricKeyEncryption() {
        final EDSKeyPair keyPair = generateKeyPair();
        final byte[] data = generateData(524288);
        final PublicEDSKey publicKey = keyPair.getPublic();

        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.encrypt(publicKey, data));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertTrue(cause.getMessage().contains("PublicEDSKey"));
    }

    @Test
    void testInvalidAsymmetricKeyDecryption() {
        final EDSKeyPair keyPair = generateKeyPair();
        final byte[] data = generateData(524288);
        final PrivateEDSKey privateKey = keyPair.getPrivate();

        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.decrypt(privateKey, data));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertTrue(cause.getMessage().contains("PrivateEDSKey"));
    }

    @Test
    void testInvalidDearmoringPublicKey() {
        final Settings mySettings = newSettings();
        final Crypto myCrypto = new Crypto(mySettings);
        final EDSKeyPair keyPair = Crypto.generateAsymmetricKey(mySettings.getAsymmetricAlgorithm());
        final String armoredPublicKey = Crypto.armoringPublicKey(keyPair.getPublic().getKey());

        mySettings.set(StandardSetting.ASYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.AES_CBC_128.name());

        final EDSException cause = assertThrows(EDSException.class, () -> myCrypto.dearmoringPublicKey(armoredPublicKey));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("AES KeyFactory not available", cause.getMessage());
    }

    @Test
    void testInvalidDearmoringPrivateKey() {
        final Settings mySettings = newSettings();
        final Crypto myCrypto = new Crypto(mySettings);
        final EDSKeyPair keyPair = Crypto.generateAsymmetricKey(mySettings.getAsymmetricAlgorithm());
        final SecretEDSKey secretKey = Crypto.generateSymmetricKey(mySettings.getSymmetricAlgorithm());
        secretKey.setSalt(new IVSalt(UUID.randomUUID().toString()));

        final String armoredPrivateKey = Crypto.encryptAndArmorPrivateKey(secretKey, keyPair.getPrivate().getKey());
        mySettings.set(StandardSetting.ASYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.AES_CBC_128.name());

        final EDSException cause = assertThrows(EDSException.class, () -> myCrypto.dearmoringPrivateKey(secretKey, armoredPrivateKey));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("AES KeyFactory not available", cause.getMessage());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private SecretEDSKey prepareSecretEDSKey() throws NoSuchAlgorithmException {
        final KeyAlgorithm algorithm = settings.getSymmetricAlgorithm();
        final KeyGenerator generator = KeyGenerator.getInstance(algorithm.getName());
        generator.init(algorithm.getLength());

        final SecretKey secretKey = generator.generateKey();
        final SecretEDSKey key = new SecretEDSKey(settings.getAsymmetricAlgorithm(), secretKey);
        key.setSalt(new IVSalt(UUID.randomUUID().toString()));

        return key;
    }

    private EDSKeyPair generateKeyPair() {
        final EDSKeyPair generated = Crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final KeyPair keys = new KeyPair(generated.getPublic().getKey(), generated.getPrivate().getKey());

        return new EDSKeyPair(settings.getSymmetricAlgorithm(), keys);
    }
}
