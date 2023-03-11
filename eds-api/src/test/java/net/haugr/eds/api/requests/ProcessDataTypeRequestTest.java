/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.TestUtilities;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class ProcessDataTypeRequestTest {

    @Test
    void testClassFlow() {
        final String typeName = "DataType TypeName";
        final String type = "DataType Type";

        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.PROCESS);
        request.setTypeName(typeName);
        request.setType(type);
        final Map<String, String> errors = request.validate();

        assertTrue(errors.isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, request.getAccountName());
        assertEquals(Constants.ADMIN_ACCOUNT, TestUtilities.convert(request.getCredential()));
        assertEquals(Action.PROCESS, request.getAction());
        assertEquals(typeName, request.getTypeName());
        assertEquals(type, request.getType());
    }

    @Test
    void testEmptyClass() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAction(null);
        final Map<String, String> errors = request.validate();

        assertEquals(2, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("No action has been provided.", errors.get(Constants.FIELD_ACTION));
    }

    @Test
    void testInvalidAction() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("Not supported Action has been provided.", errors.get(Constants.FIELD_ACTION));
    }

    @Test
    void testActionProcess() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setTypeName("The TypeName");
        request.setType("The Type");
        request.setAction(Action.PROCESS);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionProcessFail() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.PROCESS);

        final Map<String, String> errors = request.validate();
        assertEquals(2, errors.size());
        assertEquals("The name of the DataType is missing or invalid.", errors.get(Constants.FIELD_TYPENAME));
        assertEquals("The type of the DataType is missing or invalid.", errors.get(Constants.FIELD_TYPE));
    }

    @Test
    void testActionDelete() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setTypeName("The TypeName to Delete");
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionDeleteFail() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setTypeName("");
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("The name of the DataType is missing or invalid.", errors.get(Constants.FIELD_TYPENAME));
    }
}
