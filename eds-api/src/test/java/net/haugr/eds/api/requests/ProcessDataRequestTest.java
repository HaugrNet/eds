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
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class ProcessDataRequestTest {

    @Test
    void testClassFlow() {
        final String dataId = UUID.randomUUID().toString();
        final String circleId = UUID.randomUUID().toString();
        final String targetCircleId = UUID.randomUUID().toString();
        final String dataName = "Data Name";
        final String folderId = UUID.randomUUID().toString();
        final String targetFolderId = UUID.randomUUID().toString();
        final String typeName = "The Type";
        final byte[] data = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5 };

        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.ADD);
        request.setDataId(dataId);
        request.setCircleId(circleId);
        request.setTargetCircleId(targetCircleId);
        request.setDataName(dataName);
        request.setFolderId(folderId);
        request.setTargetFolderId(targetFolderId);
        request.setTypeName(typeName);
        request.setData(data);
        final Map<String, String> errors = request.validate();

        assertTrue(errors.isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, request.getAccountName());
        assertEquals(Constants.ADMIN_ACCOUNT, TestUtilities.convert(request.getCredential()));
        assertEquals(Action.ADD, request.getAction());
        assertEquals(dataId, request.getDataId());
        assertEquals(circleId, request.getCircleId());
        assertEquals(targetCircleId, request.getTargetCircleId());
        assertEquals(dataName, request.getDataName());
        assertEquals(folderId, request.getFolderId());
        assertEquals(targetFolderId, request.getTargetFolderId());
        assertEquals(typeName, request.getTypeName());
        assertArrayEquals(data, request.getData());
    }

    @Test
    void testEmptyClass() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAction(null);
        final Map<String, String> errors = request.validate();

        assertEquals(2, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("No action has been provided.", errors.get(Constants.FIELD_ACTION));
    }

    @Test
    void testInvalidAction() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.PROCESS);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("Not supported Action has been provided.", errors.get(Constants.FIELD_ACTION));
    }

    @Test
    void testActionAdd() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(UUID.randomUUID().toString());
        request.setFolderId(UUID.randomUUID().toString());
        request.setDataName("New Data");
        request.setTypeName("Data Type name");
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionAddFail() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(null);
        request.setFolderId("Invalid folder Id");
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertEquals(3, errors.size());
        assertEquals("The Circle Id is missing or invalid.", errors.get(Constants.FIELD_CIRCLE_ID));
        assertEquals("The Folder Id is invalid.", errors.get(Constants.FIELD_FOLDER_ID));
    }

    @Test
    void testActionUpdate() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(UUID.randomUUID().toString());
        request.setFolderId(UUID.randomUUID().toString());
        request.setDataName("Updated Data Name");
        request.setAction(Action.UPDATE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionUpdateFail() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(null);
        request.setFolderId("Invalid Folder Id");
        request.setDataName("Too long new name for the Data, it is only allowed to be under 75 characters long, and this should hopefully exceed that.");
        request.setAction(Action.UPDATE);

        final Map<String, String> errors = request.validate();
        assertEquals(3, errors.size());
        assertEquals("The Data Id to update is missing or invalid.", errors.get(Constants.FIELD_DATA_ID));
        assertEquals("The Folder Id is invalid.", errors.get(Constants.FIELD_FOLDER_ID));
        assertEquals("The new name of the Data Object is invalid.", errors.get(Constants.FIELD_DATA_NAME));
    }

    @Test
    void testActionCopy() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(UUID.randomUUID().toString());
        request.setTargetCircleId(UUID.randomUUID().toString());
        request.setTargetFolderId(UUID.randomUUID().toString());
        request.setAction(Action.COPY);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionCopyFail() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(null);
        request.setAction(Action.COPY);

        final Map<String, String> errors = request.validate();
        assertEquals(2, errors.size());
        assertEquals("The Data Id to copy is missing or invalid.", errors.get(Constants.FIELD_DATA_ID));
        assertEquals("The target Circle Id is missing or invalid.", errors.get(Constants.FIELD_TARGET_CIRCLE_ID));
    }

    @Test
    void testActionMove() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(UUID.randomUUID().toString());
        request.setTargetCircleId(UUID.randomUUID().toString());
        request.setTargetFolderId(UUID.randomUUID().toString());
        request.setAction(Action.MOVE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionMoveFail() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(null);
        request.setAction(Action.MOVE);

        final Map<String, String> errors = request.validate();
        assertEquals(2, errors.size());
        assertEquals("The Data Id to move is missing or invalid.", errors.get(Constants.FIELD_DATA_ID));
        assertEquals("The target Circle Id is missing or invalid.", errors.get(Constants.FIELD_TARGET_CIRCLE_ID));
    }

    @Test
    void testActionDelete() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(UUID.randomUUID().toString());
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionDeleteFail() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(null);
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("The Data Id to delete is missing or invalid.", errors.get(Constants.FIELD_DATA_ID));
    }
}
