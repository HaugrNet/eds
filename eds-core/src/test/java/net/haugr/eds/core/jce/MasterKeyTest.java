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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.exceptions.CryptoException;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class MasterKeyTest extends DatabaseSetup {

    private static final String URL_CONTENT = "This is my test data";

    @Test
    void testReadMasterKeySecretFromUrl() {
        final byte[] bytes = MasterKey.readMasterKeySecretFromUrl(prepareDataUrl());
        assertArrayEquals(URL_CONTENT.getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    void testDerivationIsDeterministicAndSeparated() {
        final byte[] secret = TEST_MASTERKEY_SECRET.getBytes(StandardCharsets.UTF_8);
        final MasterKey.Material first = MasterKey.derive(secret);
        final MasterKey.Material second = MasterKey.derive(secret);
        final MasterKey.Material other = MasterKey.derive(NEW_MASTERKEY_SECRET.getBytes(StandardCharsets.UTF_8));

        assertEquals(first.kcv(), second.kcv());
        assertArrayEquals(first.wrapKey().getEncoded(), second.wrapKey().getEncoded());
        assertArrayEquals(first.hmacKey(), second.hmacKey());
        assertNotEquals(first.kcv(), other.kcv());

        // The subkeys are independent: the raw secret, the wrap key and the
        // HMAC key all differ.
        assertEquals(32, first.wrapKey().getEncoded().length);
        assertEquals(64, first.hmacKey().length);
        assertFalse(java.util.Arrays.equals(first.wrapKey().getEncoded(), java.util.Arrays.copyOf(first.hmacKey(), 32)));
        assertFalse(java.util.Arrays.equals(secret, first.wrapKey().getEncoded()));
    }

    // The Material is given as the expected value on purpose: only so does
    // the assertion call Material.equals(null), which is part of what is
    // checked; assertNotNull, or null first, would leave equals out.
    @SuppressWarnings({ "java:S2701", "java:S3415" })
    @Test
    void testMaterialIsEqualByItsKeysAndNeverShowsThem() {
        final byte[] secret = TEST_MASTERKEY_SECRET.getBytes(StandardCharsets.UTF_8);
        final MasterKey.Material first = MasterKey.derive(secret);
        final MasterKey.Material second = MasterKey.derive(secret);
        final MasterKey.Material other = MasterKey.derive(NEW_MASTERKEY_SECRET.getBytes(StandardCharsets.UTF_8));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
        assertNotEquals(first, null);
        assertEquals("Material[kcv=" + first.kcv() + ']', first.toString());
        final String hmac = Base64.getEncoder().encodeToString(first.hmacKey());
        assertFalse(first.toString().contains(hmac));
    }

    @Test
    void testEmptySecretIsRejected() {
        assertThrows(CryptoException.class, () -> MasterKey.derive(new byte[0]));
        assertThrows(CryptoException.class, () -> MasterKey.derive(null));
    }

    @Test
    void testLockedMasterKey() {
        final MasterKey masterKey = MasterKey.getInstance();
        masterKey.lock();
        assertFalse(masterKey.isUnlocked());

        final EDSException cause = assertThrows(EDSException.class, masterKey::getKcv);
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("EDS is locked, the MasterKey is not loaded.", cause.getMessage());

        installTestMasterKey();
        assertTrue(masterKey.isUnlocked());
        assertEquals(1, masterKey.getVersion());
    }

    private static String prepareDataUrl() {
        try {
            final Path tempFile = Files.createTempFile("Master", "key");
            Files.write(tempFile, URL_CONTENT.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            final URI uri = tempFile.toUri();

            return uri.toString();
        } catch (IOException e) {
            throw new EDSException(ReturnCode.ERROR, e);
        }
    }
}
