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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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
    void testGCM256Encryption() {
        final String clearText = "This is just an example";

        final KeyAlgorithm algorithm = KeyAlgorithm.AES_GCM_256;
        final SecretEDSKey key = Crypto.generateSymmetricKey(algorithm);
        final Nonce nonce = Nonce.random();
        final byte[] aad = crypto.stringToBytes("test/aad");

        final byte[] clearTextBytes = crypto.stringToBytes(clearText);
        final byte[] encryptedBytes = Crypto.seal(key, nonce, aad, clearTextBytes);
        final byte[] decryptedBytes = Crypto.open(key, nonce, aad, encryptedBytes);
        final String decrypted = crypto.bytesToString(decryptedBytes);

        assertEquals(clearText, decrypted);
    }

    @Test
    void testSealingTwiceGivesDifferentResults() {
        final SecretEDSKey key = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        final byte[] aad = crypto.stringToBytes("test/aad");
        final byte[] plain = crypto.stringToBytes("The same value");

        final byte[] first = Crypto.seal(key, aad, plain);
        final byte[] second = Crypto.seal(key, aad, plain);
        assertFalse(Arrays.equals(first, second));
        assertArrayEquals(plain, Crypto.open(key, aad, first));
        assertArrayEquals(plain, Crypto.open(key, aad, second));
    }

    @Test
    void testOpenWithWrongAadFails() {
        final SecretEDSKey key = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        final byte[] sealed = Crypto.seal(key, crypto.stringToBytes("member|1"), crypto.stringToBytes("secret"));
        final byte[] otherAad = crypto.stringToBytes("member|2");

        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.open(key, otherAad, sealed));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
    }

    @Test
    void testOpenWithTamperedCiphertextFails() {
        final SecretEDSKey key = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        final byte[] aad = crypto.stringToBytes("test/aad");
        final byte[] sealed = Crypto.seal(key, aad, crypto.stringToBytes("secret"));
        sealed[sealed.length - 1] ^= 1;

        assertThrows(EDSException.class, () -> Crypto.open(key, aad, sealed));
    }

    @Test
    void testOpenTooShortValueFails() {
        final SecretEDSKey key = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        final byte[] aad = crypto.stringToBytes("test/aad");

        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.open(key, aad, new byte[5]));
        assertEquals("The sealed value is too short.", cause.getMessage());
        final EDSException missing = assertThrows(EDSException.class, () -> Crypto.open(key, aad, null));
        assertEquals("The sealed value is too short.", missing.getMessage());
    }

    @Test
    void testOpenWithMasterKeyRefusesWhatIsntBase64() {
        final EDSException cause = assertThrows(EDSException.class, () -> crypto.openWithMasterKey("eds/test", "not base64 !"));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
    }

    @Test
    void testNonceLength() {
        assertEquals(12, Nonce.random().getBytes().length);
        assertThrows(EDSException.class, () -> Nonce.of(new byte[16]));
        assertThrows(EDSException.class, () -> Nonce.of(null));
    }

    /**
     * Every MasterKey encryption uses a random nonce (fixes the fixed-IV
     * nonce reuse found 2026-10-22), so two encryptions of the same value
     * differ.
     */
    @Test
    void testMasterKeyEncryptionsDiffer() {
        final byte[] value = crypto.stringToBytes("member salt");

        final String first = crypto.sealWithMasterKey("eds/test", value);
        final String second = crypto.sealWithMasterKey("eds/test", value);
        assertNotEquals(first, second);
        assertArrayEquals(value, crypto.openWithMasterKey("eds/test", first));
        assertArrayEquals(value, crypto.openWithMasterKey("eds/test", second));
    }

    @Test
    void testMasterKeyEncryptionIsBoundToAadAndVersion() {
        final byte[] value = crypto.stringToBytes("member salt");
        final String sealed = crypto.sealWithMasterKey("eds/test|1", value);

        assertThrows(EDSException.class, () -> crypto.openWithMasterKey("eds/test|2", sealed));

        // The same MasterKey, but another version, can't open it either.
        MasterKey.getInstance().install(MasterKey.derive(crypto.stringToBytes(TEST_MASTERKEY_SECRET)), 2);
        assertThrows(EDSException.class, () -> crypto.openWithMasterKey("eds/test|1", sealed));
    }

    @Test
    void testMasterKeyEncryptionDuringRotationCanBeOpened() throws InterruptedException {
        // Values are sealed while the MasterKey is replaced again and again,
        // as in a rotation under load. Each value must be sealed with the
        // key and the version of one and the same MasterKey, else it can
        // never be opened again.
        final MasterKey masterKey = MasterKey.getInstance();
        final MasterKey.Material first = MasterKey.derive(crypto.stringToBytes(TEST_MASTERKEY_SECRET));
        final MasterKey.Material second = MasterKey.derive(crypto.stringToBytes(NEW_MASTERKEY_SECRET));
        final byte[] value = crypto.stringToBytes("member salt");
        final Queue<String> sealed = new ConcurrentLinkedQueue<>();
        final AtomicBoolean running = new AtomicBoolean(true);

        final Thread[] sealers = new Thread[4];
        for (int i = 0; i < sealers.length; i++) {
            sealers[i] = Thread.ofPlatform().start(() -> {
                while (running.get()) {
                    sealed.add(crypto.sealWithMasterKey("eds/test", value));
                }
            });
        }
        boolean toSecond = true;
        while (sealed.size() < 50_000) {
            if (toSecond) {
                masterKey.install(second, 2);
            } else {
                masterKey.install(first, 1);
            }
            toSecond = !toSecond;
        }
        running.set(false);
        for (final Thread sealer : sealers) {
            sealer.join();
        }

        assertFalse(sealed.isEmpty());
        int unreadable = 0;
        for (final String one : sealed) {
            if (!opens(masterKey, first, 1, one, value) && !opens(masterKey, second, 2, one, value)) {
                unreadable++;
            }
        }
        assertEquals(0, unreadable, "of " + sealed.size() + " values");
    }

    private boolean opens(final MasterKey masterKey, final MasterKey.Material material, final int version, final String sealed, final byte[] value) {
        masterKey.install(material, version);
        try {
            return Arrays.equals(value, crypto.openWithMasterKey("eds/test", sealed));
        } catch (EDSException _) {
            return false;
        }
    }

    @Test
    void testMasterKeyEncryptionWithOtherMasterKeyFails() {
        final String sealed = crypto.sealWithMasterKey("eds/test", crypto.stringToBytes("member salt"));

        MasterKey.getInstance().install(MasterKey.derive(crypto.stringToBytes(NEW_MASTERKEY_SECRET)), 1);
        assertThrows(EDSException.class, () -> crypto.openWithMasterKey("eds/test", sealed));
    }

    @Test
    void testGeneratingPasswordKeyWithInvalidAlgorithm() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.PBE_ALGORITHM.getKey(), "MLKEM1024_P384");
        final Crypto myCrypto = new Crypto(mySettings);
        final byte[] salt = generateData(16);
        final byte[] bytes = crypto.stringToBytes("my secret");
        final int iterations = settings.getPasswordIterations();

        final EDSException cause = assertThrows(EDSException.class, () -> myCrypto.generatePasswordKey(KeyAlgorithm.AES_GCM_256, bytes, salt, iterations));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("AES_256/GCM/NoPadding SecretKeyFactory not available", cause.getMessage());
    }

    @Test
    void testPasswordWithWeirdCharacters() {
        final byte[] secret = new byte[256];
        for (int i = 0; i < 256; i++) {
            secret[i] = (byte) i;
        }
        final KeyAlgorithm algorithm = KeyAlgorithm.PBE_GCM_256;
        final byte[] salt = generateData(16);
        final SecretEDSKey key = crypto.generatePasswordKey(algorithm, secret, salt, settings.getPasswordIterations());
        assertNotNull(key);
    }

    @Test
    void testGeneratingSymmetricKeyWithInvalidAlgorithm() {
        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.generateSymmetricKey(KeyAlgorithm.MLKEM1024_P384));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("no such algorithm: ML-KEM-1024+P-384 for provider SunJCE", cause.getMessage());
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
    void testHmacWithMasterKey() {
        final byte[] value = "invitation token".getBytes(settings.getCharset());
        final byte[] other = "another token".getBytes(settings.getCharset());

        final String hmac = crypto.hmacWithMasterKey("eds/test", value);
        assertEquals(hmac, crypto.hmacWithMasterKey("eds/test", value));
        assertNotEquals(hmac, crypto.hmacWithMasterKey("eds/test", other));
        // The label separates the purposes.
        assertNotEquals(hmac, crypto.hmacWithMasterKey("eds/other", value));
        // HMAC-SHA512 gives 64 bytes.
        assertEquals(64, Base64.getDecoder().decode(hmac).length);
    }

    /**
     * There are two types of Cryptography applied in EDS. This test will
     * demonstrate the Symmetric Encryption part, which is used for all Data
     * shared within Circles.
     */
    @Test
    void testObjectEncryption() {
        final SecretEDSKey key = Crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        final byte[] aad = crypto.stringToBytes("eds/data");

        // Now, we're going to encrypt some data
        final String clearText = "This is just an example";
        final byte[] toEncrypt = crypto.stringToBytes(clearText);
        final byte[] encrypted = Crypto.seal(key, aad, toEncrypt);

        // And decrypt it so we can verify it
        final byte[] decrypted = Crypto.open(key, aad, encrypted);
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
        final byte[] salt = crypto.stringToBytes("MemberSpecificSalt");
        final SecretEDSKey key = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes(password), salt, settings.getPasswordIterations());
        final byte[] aad = crypto.stringToBytes("test/aad");

        // Now, we're going to encrypt some data
        final String clearText = "This is just an example";
        final byte[] encrypted = Crypto.seal(key, aad, crypto.stringToBytes(clearText));

        // And decrypt it so we can verify it
        final byte[] decrypted = Crypto.open(key, aad, encrypted);
        final String result = crypto.bytesToString(decrypted);

        assertEquals(clearText, result);
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

        final Nonce nonce = Nonce.random();
        final byte[] aad = crypto.stringToBytes("test/aad");

        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.seal(key, nonce, aad, data));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("Cannot prepare Cipher for this Algorithm Type ASYMMETRIC.", cause.getMessage());
    }

    @Test
    void testInvalidSymmetricKeyDecryption() throws NoSuchAlgorithmException {
        final SecretEDSKey key = prepareSecretEDSKey();
        final byte[] data = generateData(524288);

        final Nonce nonce = Nonce.random();
        final byte[] aad = crypto.stringToBytes("test/aad");

        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.open(key, nonce, aad, data));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("Cannot prepare Cipher for this Algorithm Type ASYMMETRIC.", cause.getMessage());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private SecretEDSKey prepareSecretEDSKey() throws NoSuchAlgorithmException {
        final KeyAlgorithm algorithm = settings.getSymmetricAlgorithm();
        final KeyGenerator generator = KeyGenerator.getInstance(algorithm.getName());
        generator.init(algorithm.getLength());

        final SecretKey secretKey = generator.generateKey();
        return new SecretEDSKey(settings.getAsymmetricAlgorithm(), secretKey);
    }
}
