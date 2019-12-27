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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.TestUtilities;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.2
 */
final class InventoryRequestTest {

    private static final byte[] CREDENTIAL = {};

    @Test
    void testClassflow() {
        final String name = "Authentication Name";
        final String credentials = "Member Passphrase";
        final CredentialType type = CredentialType.SIGNATURE;

        final InventoryRequest inventoryRequest = new InventoryRequest();
        assertNotEquals(name, inventoryRequest.getAccountName());
        assertNotEquals(type, inventoryRequest.getCredentialType());
        assertNotEquals(credentials, inventoryRequest.getCredential());
        assertEquals(1, inventoryRequest.getPageNumber());
        assertEquals(Constants.MAX_PAGE_SIZE, inventoryRequest.getPageSize());

        inventoryRequest.setAccountName(name);
        inventoryRequest.setCredentialType(type);
        inventoryRequest.setCredential(TestUtilities.convert(credentials));
        assertEquals(name, inventoryRequest.getAccountName());
        assertEquals(type, inventoryRequest.getCredentialType());
        assertEquals(credentials, TestUtilities.convert(inventoryRequest.getCredential()));

        final Map<String, String> errors = inventoryRequest.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testEmptyClass() {
        final InventoryRequest inventoryRequest = new InventoryRequest();
        inventoryRequest.setAccountName("");
        inventoryRequest.setCredentialType(null);
        inventoryRequest.setPageNumber(321);
        inventoryRequest.setPageSize(1000);

        final Map<String, String> errors = inventoryRequest.validate();
        assertEquals(3, errors.size());
        assertEquals("AccountName is missing, null or invalid.", errors.get(Constants.FIELD_ACCOUNT_NAME));
        assertEquals("The Credential is missing.", errors.get(Constants.FIELD_CREDENTIAL));
    }
}
