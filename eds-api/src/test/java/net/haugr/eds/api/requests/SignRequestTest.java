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
package net.haugr.eds.api.requests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.TestUtilities;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.Utilities;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class SignRequestTest {

    @Test
    void testClassFlow() {
        final byte[] data = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5 };
        final LocalDateTime expires = Utilities.newDate();

        final SignRequest request = new SignRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setData(data);
        request.setExpires(expires);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, request.getAccountName());
        assertArrayEquals(TestUtilities.convert(Constants.ADMIN_ACCOUNT), request.getCredential());
        assertArrayEquals(data, request.getData());
        assertEquals(expires, request.getExpires());
    }

    @Test
    void testEmptyClass() {
        final SignRequest request = new SignRequest();
        final Map<String, String> errors = request.validate();

        assertEquals(2, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("The Data Object to create a Signature is missing.", errors.get(Constants.FIELD_DATA));
    }

    @Test
    void testClassWithInvalidValues() {
        final SignRequest request = new SignRequest();
        request.setData(null);

        final Map<String, String> errors = request.validate();
        assertEquals(2, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("The Data Object to create a Signature is missing.", errors.get(Constants.FIELD_DATA));
    }
}
