/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

import net.haugr.eds.core.setup.DatabaseSetup;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.1
 */
class IVSaltTest extends DatabaseSetup {

    @Test
    void testSaltAsUUID() {
        final String uuid = UUID.randomUUID().toString();
        final IVSalt salt = new IVSalt(uuid);

        assertEquals(uuid, salt.getArmored());
        assertEquals(16, salt.getBytes().length);
    }

    @Test
    void testDefaultSalt() {
        final IVSalt salt = new IVSalt();

        assertEquals(24, salt.getArmored().length());
        assertEquals(16, salt.getBytes().length);
    }

    @Test
    void testSecureRandom16Bytes() {
        final byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        final String armored = Base64.getEncoder().encodeToString(random);

        final IVSalt salt = new IVSalt(armored);
        final byte[] bytes = salt.getBytes();

        assertEquals(armored, salt.getArmored());
        assertArrayEquals(random, bytes);
    }

    /**
     * <p>The problem with the IVSalt changes between EDS 1.0 &amp; 1.1, is
     * that in EDS 1.0, the Salt was generated using a UUID, which was not
     * stored using Base64 encoding, since it was persisted raw. Since the code
     * is checking for an exception, this code is testing converting a crafted
     * invalid string.</p>
     */
    @Test
    void testCraftedStringAsSalt() {
        final String armored = "==ABCDEFG123456789%&/()?";

        final IVSalt salt = new IVSalt(armored);
        final byte[] bytes = salt.getBytes();

        assertEquals(armored, salt.getArmored());
        assertEquals(new String(bytes, settings.getCharset()), armored.substring(0, 16));
    }
}
