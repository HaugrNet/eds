/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.Test;

/**
 * <p>Common test class for the Process & Fetch DataType Services.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class DataTypeServiceTest extends DatabaseSetup {

    @Test
    void testEmptyFetchRequest() {
        final FetchDataTypeService service = new FetchDataTypeService(settings, entityManager);
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.", cause.getMessage());
    }

    @Test
    void testEmptyProcessRequest() {
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
    void testAdminFetchRequest() {
        final FetchDataTypeService service = new FetchDataTypeService(settings, entityManager);
        final FetchDataTypeRequest request = prepareRequest(FetchDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchDataTypeResponse response = service.perform(request);

        assertNotNull(response);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(2, response.getDataTypes().size());
        assertEquals(Constants.FOLDER_TYPENAME, response.getDataTypes().get(0).getTypeName());
        assertEquals("Folder", response.getDataTypes().get(0).getType());
        assertEquals(Constants.DATA_TYPENAME, response.getDataTypes().get(1).getTypeName());
        assertEquals("Data Object", response.getDataTypes().get(1).getType());
    }

    @Test
    void testInvokeWithoutAnything() {
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
    void testCircleAdminsAreAuthorized() {
        final String type = "MyDataType";
        final String name = "The Data Type";
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, MEMBER_1);
        request.setType(type);
        request.setTypeName(name);
        assertNotNull(request.getTypeName());
        assertNotNull(request.getType());

        final ProcessDataTypeResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Data Type '" + name + "' was successfully processed.", response.getReturnMessage());
    }

    @Test
    void testNotAuthorizedRequest() {
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
    void testUpdateRestrictedDataTypeFolder() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setTypeName(Constants.FOLDER_TYPENAME);
        request.setType("alternative folder");
        assertNotNull(request.getTypeName());
        assertNotNull(request.getType());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("It is not permitted to update the Data Type '" + Constants.FOLDER_TYPENAME + "'.", cause.getMessage());
    }

    @Test
    void testUpdateRestrictedDataTypeData() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setTypeName(Constants.DATA_TYPENAME);
        request.setType("alternative data");
        assertNotNull(request.getTypeName());
        assertNotNull(request.getType());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("It is not permitted to update the Data Type '" + Constants.DATA_TYPENAME + "'.", cause.getMessage());
    }

    @Test
    void testCreateAndDeleteDataType() {
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
        assertEquals("The Data Type '" + theDataTypeName + "' was successfully processed.", response.getReturnMessage());

        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.DELETE);
        final ProcessDataTypeResponse deletedResponse = service.perform(request);
        assertNotNull(deletedResponse);
        assertTrue(deletedResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), deletedResponse.getReturnCode());
        assertEquals("The Data Type '" + theDataTypeName + "' was successfully deleted.", deletedResponse.getReturnMessage());
    }

    @Test
    void testCreateAndDeleteUsedDataType() {
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
    void testDeleteUnknownDataType() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        final String theDataTypeName = "unknownDataType";
        final String newDataTypeType = "The Type information";
        request.setTypeName(theDataTypeName);
        request.setType(newDataTypeType);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No records were found with the name '" + theDataTypeName + "'.", cause.getMessage());
    }

    @Test
    void testCreateAndUpdateDataType() {
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
        assertEquals("The Data Type '" + theDataTypeName + "' was successfully processed.", response.getReturnMessage());
        assertEquals(theDataTypeName, response.getDataType().getTypeName());
        assertEquals(newDataTypeType, response.getDataType().getType());

        final String updatedDataTypeType = "updatedType";
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setTypeName(theDataTypeName);
        request.setType(updatedDataTypeType);
        final ProcessDataTypeResponse updateResponse = service.perform(request);
        assertTrue(updateResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());
        assertEquals("The Data Type '" + theDataTypeName + "' was successfully processed.", updateResponse.getReturnMessage());
        assertEquals(theDataTypeName, updateResponse.getDataType().getTypeName());
        assertEquals(updatedDataTypeType, updateResponse.getDataType().getType());
    }

    @Test
    void testCreateAndRepeatDataType() {
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
        assertEquals("The Data Type '" + aDataTypeName + "' was successfully processed.", response.getReturnMessage());
        assertEquals(aDataTypeName, response.getDataType().getTypeName());
        assertEquals(aDataTypeType, response.getDataType().getType());

        createRequest.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        final ProcessDataTypeResponse updateResponse = dataTypeService.perform(createRequest);
        assertTrue(updateResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());
        assertEquals("The Data Type '" + aDataTypeName + "' was successfully processed.", updateResponse.getReturnMessage());
        assertEquals(aDataTypeName, updateResponse.getDataType().getTypeName());
        assertEquals(aDataTypeType, updateResponse.getDataType().getType());
    }
}
