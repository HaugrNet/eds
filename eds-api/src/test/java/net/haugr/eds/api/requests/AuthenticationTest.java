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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.TestUtilities;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class AuthenticationTest {

    private static final byte[] CREDENTIAL = {};

    @Test
    void testClassFlow() {
        final String name = "Authentication Name";
        final String credentials = "Member Passphrase";
        final CredentialType type = CredentialType.SIGNATURE;

        final Authentication authentication = new Authentication();
        assertNotEquals(name, authentication.getAccountName());
        assertNotEquals(type, authentication.getCredentialType());
        assertNotEquals(credentials, TestUtilities.convert(authentication.getCredential()));

        authentication.setAccountName(name);
        authentication.setCredentialType(type);
        authentication.setCredential(TestUtilities.convert(credentials));
        assertEquals(name, authentication.getAccountName());
        assertEquals(type, authentication.getCredentialType());
        assertEquals(credentials, TestUtilities.convert(authentication.getCredential()));

        final Map<String, String> errors = authentication.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testEmptyClass() {
        final Authentication authentication = new Authentication();
        authentication.setAccountName("");
        authentication.setCredentialType(null);

        final Map<String, String> errors = authentication.validate();
        assertEquals(2, errors.size());
        assertEquals("AccountName is missing, null or invalid.", errors.get(Constants.FIELD_ACCOUNT_NAME));
        assertEquals("The Credential is missing.", errors.get(Constants.FIELD_CREDENTIAL));
    }

    @Test
    void testSessionEmptyCredential() {
        final Authentication authentication = new Authentication();
        authentication.setCredentialType(CredentialType.SESSION);

        final Map<String, String> errors = authentication.validate();
        assertEquals(1, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
    }

    @Test
    void testEmptyClassEmptyCredential() {
        final Authentication authentication = new Authentication();
        authentication.setAccountName("");
        authentication.setCredential(CREDENTIAL);
        authentication.setCredentialType(null);

        final Map<String, String> errors = authentication.validate();
        assertEquals(2, errors.size());
        assertEquals("AccountName is missing, null or invalid.", errors.get(Constants.FIELD_ACCOUNT_NAME));
        assertEquals("The Credential is missing.", errors.get(Constants.FIELD_CREDENTIAL));
    }

    @Test
    void testMissingCredentialTypePassphrase() {
        final Authentication authentication = new Authentication();
        authentication.setAccountName("AccountName");
        final Map<String, String> errors = authentication.validate();

        assertEquals(1, errors.size());
        assertEquals("The Credential is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals(CredentialType.PASSPHRASE, authentication.getCredentialType());
    }

    @Test
    void testMissingCredentialTypeSession() {
        final Authentication authentication = new Authentication();
        final Map<String, String> errors = authentication.validate();

        assertEquals(1, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals(CredentialType.SESSION, authentication.getCredentialType());
    }
}
