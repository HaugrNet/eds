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
package io.javadog.cws.api.requests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.TestUtilities;
import io.javadog.cws.api.common.Constants;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class VerifyRequestTest {

    @Test
    public void testClassflow() {
        final String signature = "Signature";
        final byte[] data = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5 };

        final VerifyRequest request = new VerifyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setSignature(signature);
        request.setData(data);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, request.getAccountName());
        assertEquals(Constants.ADMIN_ACCOUNT, TestUtilities.convert(request.getCredential()));
        assertEquals(signature, request.getSignature());
        assertArrayEquals(data, request.getData());
    }

    @Test
    public void testEmptyClass() {
        final VerifyRequest request = new VerifyRequest();
        final Map<String, String> errors = request.validate();

        assertEquals(3, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("The Signature is missing.", errors.get(Constants.FIELD_SIGNATURE));
        assertEquals("The Data Object to check the Signature against is missing.", errors.get(Constants.FIELD_DATA));
    }

    @Test
    public void testClassWithInvalidValues() {
        final VerifyRequest request = new VerifyRequest();
        request.setSignature("");
        request.setData(null);

        final Map<String, String> errors = request.validate();
        assertEquals(3, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("The Signature is missing.", errors.get(Constants.FIELD_SIGNATURE));
        assertEquals("The Data Object to check the Signature against is missing.", errors.get(Constants.FIELD_DATA));
    }
}
