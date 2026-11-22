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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Base64;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.exceptions.CryptoException;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>Tests for the Member keys (ML-KEM-1024 + P-384), their double wrapped
 * storage, and the hybrid KEM (PLAN.md, Crypto §2 and §3).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class MemberKeysTest extends DatabaseSetup {

    private static final KeyAlgorithm ALGORITHM = KeyAlgorithm.MLKEM1024_P384;

    @Test
    void testPublicKeysRoundTrip() {
        final MemberKeyPair pair = MemberKeyPair.generate(ALGORITHM);
        final String armored = pair.getPublic().armored();
        final MemberKeyPair.Public read = MemberKeyPair.Public.fromArmored(ALGORITHM, armored);

        assertEquals(pair.getPublic().mlkem(), read.mlkem());
        assertEquals(pair.getPublic().ec(), read.ec());
        assertEquals("ML-KEM", read.mlkem().getAlgorithm());
        assertEquals("EC", read.ec().getAlgorithm());
    }

    @Test
    void testUnsupportedAlgorithm() {
        final EDSException cause = assertThrows(EDSException.class, () -> MemberKeyPair.generate(KeyAlgorithm.AES_GCM_256));
        assertEquals("Unsupported Member Key Algorithm AES_GCM_256.", cause.getMessage());
    }

    @Test
    void testInvalidArmoredPublicKeys() {
        final String garbage = Base64.getEncoder().encodeToString(new byte[] { 0, 0, 0, 9, 1, 2 });
        assertThrows(CryptoException.class, () -> MemberKeyPair.Public.fromArmored(ALGORITHM, garbage));
        assertThrows(CryptoException.class, () -> MemberKeyPair.Public.fromArmored(ALGORITHM, "not base64 !"));
    }

    @Test
    void testPrivateKeysRoundTrip() {
        final MemberKeyPair pair = MemberKeyPair.generate(ALGORITHM);

        final MemberKeyPair read = MemberKeyPair.of(pair.getPublic(), pair.privateBytes());

        assertEquals(ALGORITHM, read.getAlgorithm());
        assertEquals(pair.getMlkemPrivate(), read.getMlkemPrivate());
        assertEquals(pair.getEcPrivate(), read.getEcPrivate());
    }

    @Test
    void testInvalidPrivateKeys() {
        final MemberKeyPair pair = MemberKeyPair.generate(ALGORITHM);
        final MemberKeyPair.Public publicKeys = pair.getPublic();
        final byte[] valid = pair.privateBytes();
        final byte[] trailing = Arrays.copyOf(valid, valid.length + 1);
        final byte[] swapped = MemberKeyPair.encode(pair.getEcPrivate().getEncoded(), pair.getMlkemPrivate().getEncoded());

        assertInvalidPrivateKeys("Unexpected trailing bytes.", publicKeys, trailing);
        assertInvalidPrivateKeys("Truncated key encoding.", publicKeys, new byte[] { 0, 0 });
        assertInvalidPrivateKeys("Invalid length in key encoding.", publicKeys, new byte[] { -1, -1, -1, -1 });
        assertInvalidPrivateKeys("Invalid length in key encoding.", publicKeys, new byte[] { 0, 0, 0, 9, 1, 2 });
        // Parts which are no keys of their kind.
        assertThrows(CryptoException.class, () -> MemberKeyPair.of(publicKeys, swapped));
    }

    @Test
    void testPrivateKeysAreDoubleWrapped() {
        final MemberKeyPair pair = MemberKeyPair.generate(ALGORITHM);
        final SecretEDSKey passwordKey = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes("My passphrase"), generateData(16), settings.getPasswordIterations());
        final byte[] innerAad = crypto.stringToBytes("eds/member/key|test");

        final String stored = crypto.protectPrivateKeys(passwordKey, innerAad, "eds/member/key-outer|test", pair);
        final MemberKeyPair opened = crypto.openPrivateKeys(passwordKey, innerAad, "eds/member/key-outer|test", pair.getPublic(), stored);
        assertArrayEquals(pair.privateBytes(), opened.privateBytes());

        // The wrong outer AAD (e.g. another Member) is rejected.
        final MemberKeyPair.Public publicKeys = pair.getPublic();
        assertThrows(EDSException.class, () -> crypto.openPrivateKeys(passwordKey, innerAad, "eds/member/key-outer|other", publicKeys, stored));
    }

    /**
     * A copy of the database without the MasterKey can't be used to test a
     * password guess (PLAN.md, C3): even with the correct passphrase and
     * the salt, the stored private keys can't be opened, as the outer layer
     * is the MasterKey.
     */
    @Test
    void testDatabaseCopyCannotTestPasswordGuesses() {
        final MemberKeyPair pair = MemberKeyPair.generate(ALGORITHM);
        final byte[] salt = generateData(16);
        final SecretEDSKey passwordKey = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes("My passphrase"), salt, settings.getPasswordIterations());
        final byte[] innerAad = crypto.stringToBytes("eds/member/key|test");
        final String stored = crypto.protectPrivateKeys(passwordKey, innerAad, "eds/member/key-outer|test", pair);
        final byte[] fromDatabase = Base64.getDecoder().decode(stored);

        // The attacker has the correct password key, but no MasterKey: the
        // stored value doesn't open with the password key alone.
        assertThrows(EDSException.class, () -> Crypto.open(passwordKey, innerAad, fromDatabase));

        // Nothing opens while EDS is locked.
        MasterKey.getInstance().lock();
        final MemberKeyPair.Public publicKeys = pair.getPublic();
        final EDSException cause = assertThrows(EDSException.class, () -> crypto.openPrivateKeys(passwordKey, innerAad, "eds/member/key-outer|test", publicKeys, stored));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("EDS is locked, the MasterKey is not loaded.", cause.getMessage());
    }

    @Test
    void testWrongPasswordKeyFails() {
        final MemberKeyPair pair = MemberKeyPair.generate(ALGORITHM);
        final byte[] salt = generateData(16);
        final SecretEDSKey right = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes("My passphrase"), salt, settings.getPasswordIterations());
        final SecretEDSKey wrong = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes("My passphrasf"), salt, settings.getPasswordIterations());
        final byte[] innerAad = crypto.stringToBytes("eds/member/key|test");
        final String stored = crypto.protectPrivateKeys(right, innerAad, "outer", pair);
        final MemberKeyPair.Public publicKeys = pair.getPublic();

        assertThrows(EDSException.class, () -> crypto.openPrivateKeys(wrong, innerAad, "outer", publicKeys, stored));
    }

    @Test
    void testHybridKemRoundTrip() {
        final MemberKeyPair pair = MemberKeyPair.generate(ALGORITHM);
        final byte[] aad = crypto.stringToBytes("eds/trustee");
        final byte[] secret = generateData(32);

        final byte[] first = HybridKem.wrap(pair.getPublic(), aad, secret);
        final byte[] second = HybridKem.wrap(pair.getPublic(), aad, secret);
        assertFalse(Arrays.equals(first, second));
        assertEquals(1, first[0]);
        assertArrayEquals(secret, HybridKem.unwrap(pair, aad, first));
        assertArrayEquals(secret, HybridKem.unwrap(pair, aad, second));
    }

    @Test
    void testHybridKemWrongRecipientFails() {
        final MemberKeyPair recipient = MemberKeyPair.generate(ALGORITHM);
        final MemberKeyPair other = MemberKeyPair.generate(ALGORITHM);
        final byte[] aad = crypto.stringToBytes("eds/trustee");
        final byte[] wrapped = HybridKem.wrap(recipient.getPublic(), aad, generateData(32));

        assertThrows(CryptoException.class, () -> HybridKem.unwrap(other, aad, wrapped));
    }

    @Test
    void testHybridKemWrongAadFails() {
        final MemberKeyPair pair = MemberKeyPair.generate(ALGORITHM);
        final byte[] wrapped = HybridKem.wrap(pair.getPublic(), crypto.stringToBytes("circle 1"), generateData(32));
        final byte[] otherAad = crypto.stringToBytes("circle 2");

        assertThrows(EDSException.class, () -> HybridKem.unwrap(pair, otherAad, wrapped));
    }

    /**
     * Changing any part, the ML-KEM ciphertext, the ephemeral ECDH key or
     * the sealed secret, makes the unwrap fail: the combiner binds them all.
     */
    @Test
    void testHybridKemTamperingFails() {
        final MemberKeyPair pair = MemberKeyPair.generate(ALGORITHM);
        final byte[] aad = crypto.stringToBytes("eds/trustee");
        final byte[] wrapped = HybridKem.wrap(pair.getPublic(), aad, generateData(32));

        // Layout: version (1) | len (4) | ct_kem (1568) | len (4) | epk (120) | nonce | ciphertext + tag
        final int ctKemOffset = 1 + 4;
        final int epkOffset = ctKemOffset + 1568 + 4;
        final int sealedOffset = epkOffset + 120;
        assertEquals(sealedOffset + 12 + 32 + 16, wrapped.length);

        for (final int position : new int[] { ctKemOffset + 10, epkOffset + 60, sealedOffset + 3, wrapped.length - 1 }) {
            final byte[] tampered = Arrays.copyOf(wrapped, wrapped.length);
            tampered[position] ^= 0x01;
            assertThrows(EDSException.class, () -> HybridKem.unwrap(pair, aad, tampered), "Tampering at " + position);
        }

        final byte[] wrongVersion = Arrays.copyOf(wrapped, wrapped.length);
        wrongVersion[0] = 2;
        final EDSException cause = assertThrows(EDSException.class, () -> HybridKem.unwrap(pair, aad, wrongVersion));
        assertEquals("Unsupported hybrid KEM format.", cause.getMessage());
        final byte[] truncated = Arrays.copyOf(wrapped, 100);
        assertThrows(EDSException.class, () -> HybridKem.unwrap(pair, aad, truncated));
    }

    @Test
    void testCircleKeyRoundTrip() {
        final MemberKeyPair pair = MemberKeyPair.generate(ALGORITHM);
        final SecretEDSKey circleKey = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        final byte[] aad = crypto.stringToBytes("eds/trustee");

        final String wrapped = Crypto.wrapCircleKey(pair.getPublic(), aad, circleKey);
        final SecretEDSKey unwrapped = Crypto.unwrapCircleKey(KeyAlgorithm.AES_GCM_256, pair, aad, wrapped);
        assertArrayEquals(circleKey.getEncoded(), unwrapped.getEncoded());
        assertTrue(wrapped.length() < 8192);
    }

    @Test
    void testAWrappedCircleKeyWhichIsntBase64IsRefused() {
        final MemberKeyPair pair = MemberKeyPair.generate(ALGORITHM);
        final byte[] aad = crypto.stringToBytes("eds/trustee");

        final EDSException cause = assertThrows(EDSException.class, () -> Crypto.unwrapCircleKey(KeyAlgorithm.AES_GCM_256, pair, aad, "not base64 !"));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
    }

    private static void assertInvalidPrivateKeys(final String reason, final MemberKeyPair.Public publicKeys, final byte[] encoded) {
        final CryptoException cause = assertThrows(CryptoException.class, () -> MemberKeyPair.of(publicKeys, encoded));
        assertEquals("The private keys could not be read: " + reason, cause.getMessage());
    }
}
