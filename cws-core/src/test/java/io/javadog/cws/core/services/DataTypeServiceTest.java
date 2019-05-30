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
package io.javadog.cws.core.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.exceptions.CWSException;
import org.junit.Test;

/**
 * <p>Common test class for the Process & Fetch DataType Services.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class DataTypeServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyFetchRequest() {
        final FetchDataTypeService service = new FetchDataTypeService(settings, entityManager);
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.", cause.getMessage());
    }

    @Test
    public void testEmptyProcessRequest() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        assertNull(request.getAccountName());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing." +
                "\nKey: typeName, Error: The name of the DataType is missing or invalid." +
                "\nKey: type, Error: The type of the DataType is missing or invalid.", cause.getMessage());
    }

    @Test
    public void testAdminFetchRequest() {
        final FetchDataTypeService service = new FetchDataTypeService(settings, entityManager);
        final FetchDataTypeRequest request = prepareRequest(FetchDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchDataTypeResponse response = service.perform(request);

        assertNotNull(response);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(2, response.getDataTypes().size());
        assertEquals(Constants.DATA_TYPENAME, response.getDataTypes().get(0).getTypeName());
        assertEquals("Data Object", response.getDataTypes().get(0).getType());
        assertEquals(Constants.FOLDER_TYPENAME, response.getDataTypes().get(1).getTypeName());
        assertEquals("Folder", response.getDataTypes().get(1).getType());
    }

    @Test
    public void testInvokeWithoutAnything() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        assertNull(request.getTypeName());
        assertNull(request.getType());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: typeName, Error: The name of the DataType is missing or invalid." +
                "\nKey: type, Error: The type of the DataType is missing or invalid.", cause.getMessage());
    }

    @Test
    public void testCircleAdminsAreAuthorized() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, MEMBER_1);
        request.setType("MyDataType");
        request.setTypeName("The Data Type");
        assertNotNull(request.getTypeName());
        assertNotNull(request.getType());

        final ProcessDataTypeResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    public void testNotAuthorizedRequest() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, MEMBER_5);
        request.setType("MyDataType");
        request.setTypeName("The Data Type");
        assertNotNull(request.getTypeName());
        assertNotNull(request.getType());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process Data Type.", cause.getMessage());
    }

    @Test
    public void testUpdateRestrictedDataTypeFolder() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setTypeName(Constants.FOLDER_TYPENAME);
        request.setType("alternative folder");
        assertNotNull(request.getTypeName());
        assertNotNull(request.getType());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("It is not permitted to update the DataType '" + Constants.FOLDER_TYPENAME + "'.", cause.getMessage());
    }

    @Test
    public void testUpdateRestrictedDataTypeData() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setTypeName(Constants.DATA_TYPENAME);
        request.setType("alternative data");
        assertNotNull(request.getTypeName());
        assertNotNull(request.getType());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("It is not permitted to update the DataType '" + Constants.DATA_TYPENAME + "'.", cause.getMessage());
    }

    @Test
    public void testCreateAndDeleteDataType() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        final String theDataTypeName = "dataTypeToDelete";
        final String newDataTypeType = "The Type information";
        request.setTypeName(theDataTypeName);
        request.setType(newDataTypeType);
        final ProcessDataTypeResponse response = service.perform(request);

        assertNotNull(response);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());

        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.DELETE);
        final ProcessDataTypeResponse deletedResponse = service.perform(request);
        assertNotNull(deletedResponse);
        assertTrue(deletedResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), deletedResponse.getReturnCode());
        assertEquals("Ok", deletedResponse.getReturnMessage());
    }

    @Test
    public void testCreateAndDeleteUsedDataType() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);

        final ProcessDataTypeRequest addRequest = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        addRequest.setAction(Action.PROCESS);
        addRequest.setTypeName("text");
        addRequest.setType("text/plain");
        final ProcessDataTypeResponse addResponse = service.perform(addRequest);
        assertTrue(addResponse.isOk());

        final ProcessDataRequest dataRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        dataRequest.setAction(Action.ADD);
        dataRequest.setCircleId(CIRCLE_1_ID);
        dataRequest.setTypeName("text");
        dataRequest.setDataName("file.txt");
        final ProcessDataResponse dataResponse = dataService.perform(dataRequest);
        assertTrue(dataResponse.isOk());

        final ProcessDataTypeRequest deleteRequest = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        deleteRequest.setAction(Action.DELETE);
        deleteRequest.setTypeName("text");

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(deleteRequest));
        assertEquals(ReturnCode.ILLEGAL_ACTION, cause.getReturnCode());
        assertEquals("The Data Type 'text' cannot be deleted, as it is being actively used.", cause.getMessage());
    }

    @Test
    public void testDeleteUnknownDataType() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        final String theDataTypeName = "unknownDataType";
        final String newDataTypeType = "The Type information";
        request.setTypeName(theDataTypeName);
        request.setType(newDataTypeType);
        final ProcessDataTypeResponse response = service.perform(request);

        assertNotNull(response);
        assertFalse(response.isOk());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No records were found with the name '" + theDataTypeName + "'.", response.getReturnMessage());
    }

    @Test
    public void testCreateAndUpdateDataType() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        final String theDataTypeName = "newName";
        final String newDataTypeType = "newType";
        request.setTypeName(theDataTypeName);
        request.setType(newDataTypeType);

        final ProcessDataTypeResponse response = service.perform(request);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(theDataTypeName, response.getDataType().getTypeName());
        assertEquals(newDataTypeType, response.getDataType().getType());

        final String updatedDataTypeType = "updatedType";
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setTypeName(theDataTypeName);
        request.setType(updatedDataTypeType);
        final ProcessDataTypeResponse updateResponse = service.perform(request);
        assertTrue(updateResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());
        assertEquals("Ok", updateResponse.getReturnMessage());
        assertEquals(theDataTypeName, updateResponse.getDataType().getTypeName());
        assertEquals(updatedDataTypeType, updateResponse.getDataType().getType());
    }

    @Test
    public void testCreateAndRepeatDataType() {
        final ProcessDataTypeService dataTypeService = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest createRequest = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        createRequest.setAction(Action.PROCESS);
        final String aDataTypeName = "newName";
        final String aDataTypeType = "newType";
        createRequest.setTypeName(aDataTypeName);
        createRequest.setType(aDataTypeType);

        final ProcessDataTypeResponse response = dataTypeService.perform(createRequest);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(aDataTypeName, response.getDataType().getTypeName());
        assertEquals(aDataTypeType, response.getDataType().getType());

        createRequest.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        final ProcessDataTypeResponse updateResponse = dataTypeService.perform(createRequest);
        assertTrue(updateResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());
        assertEquals("Ok", updateResponse.getReturnMessage());
        assertEquals(aDataTypeName, updateResponse.getDataType().getTypeName());
        assertEquals(aDataTypeType, updateResponse.getDataType().getType());
    }
}
