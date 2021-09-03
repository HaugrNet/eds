/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.cws.api.common.Action;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.requests.FetchDataTypeRequest;
import net.haugr.cws.api.requests.ProcessDataRequest;
import net.haugr.cws.api.requests.ProcessDataTypeRequest;
import net.haugr.cws.api.responses.FetchDataTypeResponse;
import net.haugr.cws.api.responses.ProcessDataResponse;
import net.haugr.cws.api.responses.ProcessDataTypeResponse;
import net.haugr.cws.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>Common test class for the Process & Fetch DataType Services.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class ShareBeanDataTypeTest extends DatabaseSetup {

    @Test
    void testEmptyFetchRequest() {
        final ShareBean bean = prepareShareBean();

        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final FetchDataTypeResponse response = bean.fetchDataTypes(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.", response.getReturnMessage());
    }

    @Test
    void testEmptyProcessRequest() {
        final ShareBean bean = prepareShareBean();

        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        assertNull(request.getAccountName());

        final ProcessDataTypeResponse response = bean.processDataType(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing." +
                "\nKey: typeName, Error: The name of the DataType is missing or invalid." +
                "\nKey: type, Error: The type of the DataType is missing or invalid.", response.getReturnMessage());
    }

    @Test
    void testAdminFetchRequest() {
        final ShareBean bean = prepareShareBean();

        final FetchDataTypeRequest request = prepareRequest(FetchDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchDataTypeResponse response = bean.fetchDataTypes(request);

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
        final ShareBean bean = prepareShareBean();

        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        assertNull(request.getTypeName());
        assertNull(request.getType());

        final ProcessDataTypeResponse response = bean.processDataType(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: typeName, Error: The name of the DataType is missing or invalid." +
                "\nKey: type, Error: The type of the DataType is missing or invalid.", response.getReturnMessage());
    }

    @Test
    void testCircleAdminsAreAuthorized() {
        final ShareBean bean = prepareShareBean();
        final String type = "MyDataType";
        final String name = "The Data Type";

        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, MEMBER_1);
        request.setType(type);
        request.setTypeName(name);
        assertNotNull(request.getTypeName());
        assertNotNull(request.getType());

        final ProcessDataTypeResponse response = bean.processDataType(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Data Type '" + name + "' was successfully processed.", response.getReturnMessage());
    }

    @Test
    void testNotAuthorizedRequest() {
        final ShareBean bean = prepareShareBean();

        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, MEMBER_5);
        request.setType("MyDataType");
        request.setTypeName("The Data Type");
        assertNotNull(request.getTypeName());
        assertNotNull(request.getType());

        final ProcessDataTypeResponse response = bean.processDataType(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process Data Type.", response.getReturnMessage());
    }

    @Test
    void testUpdateRestrictedDataTypeFolder() {
        final ShareBean bean = prepareShareBean();

        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setTypeName(Constants.FOLDER_TYPENAME);
        request.setType("alternative folder");
        assertNotNull(request.getTypeName());
        assertNotNull(request.getType());

        final ProcessDataTypeResponse response = bean.processDataType(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("It is not permitted to update the Data Type '" + Constants.FOLDER_TYPENAME + "'.", response.getReturnMessage());
    }

    @Test
    void testUpdateRestrictedDataTypeData() {
        final ShareBean bean = prepareShareBean();

        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setTypeName(Constants.DATA_TYPENAME);
        request.setType("alternative data");
        assertNotNull(request.getTypeName());
        assertNotNull(request.getType());

        final ProcessDataTypeResponse response = bean.processDataType(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("It is not permitted to update the Data Type '" + Constants.DATA_TYPENAME + "'.", response.getReturnMessage());
    }

    @Test
    void testCreateAndDeleteDataType() {
        final ShareBean bean = prepareShareBean();

        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        final String theDataTypeName = "dataTypeToDelete";
        final String newDataTypeType = "The Type information";
        request.setTypeName(theDataTypeName);
        request.setType(newDataTypeType);
        final ProcessDataTypeResponse response = bean.processDataType(request);

        assertNotNull(response);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Data Type '" + theDataTypeName + "' was successfully processed.", response.getReturnMessage());

        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.DELETE);
        final ProcessDataTypeResponse deletedResponse = bean.processDataType(request);
        assertNotNull(deletedResponse);
        assertTrue(deletedResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), deletedResponse.getReturnCode());
        assertEquals("The Data Type '" + theDataTypeName + "' was successfully deleted.", deletedResponse.getReturnMessage());
    }

    @Test
    void testCreateAndDeleteUsedDataType() {
        final ShareBean bean = prepareShareBean();

        final ProcessDataTypeRequest addRequest = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        addRequest.setAction(Action.PROCESS);
        addRequest.setTypeName("text");
        addRequest.setType("text/plain");
        final ProcessDataTypeResponse addResponse = bean.processDataType(addRequest);
        assertTrue(addResponse.isOk());

        final ProcessDataRequest dataRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        dataRequest.setAction(Action.ADD);
        dataRequest.setCircleId(CIRCLE_1_ID);
        dataRequest.setTypeName("text");
        dataRequest.setDataName("file.txt");
        final ProcessDataResponse dataResponse = bean.processData(dataRequest);
        assertTrue(dataResponse.isOk());

        final ProcessDataTypeRequest deleteRequest = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        deleteRequest.setAction(Action.DELETE);
        deleteRequest.setTypeName("text");

        final ProcessDataTypeResponse response = bean.processDataType(deleteRequest);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("The Data Type 'text' cannot be deleted, as it is being actively used.", response.getReturnMessage());
    }

    @Test
    void testDeleteUnknownDataType() {
        final ShareBean bean = prepareShareBean();

        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        final String theDataTypeName = "unknownDataType";
        final String newDataTypeType = "The Type information";
        request.setTypeName(theDataTypeName);
        request.setType(newDataTypeType);

        final ProcessDataTypeResponse response = bean.processDataType(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No records were found with the name '" + theDataTypeName + "'.", response.getReturnMessage());
    }

    @Test
    void testCreateAndUpdateDataType() {
        final ShareBean bean = prepareShareBean();

        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        final String theDataTypeName = "newName";
        final String newDataTypeType = "newType";
        request.setTypeName(theDataTypeName);
        request.setType(newDataTypeType);

        final ProcessDataTypeResponse response = bean.processDataType(request);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Data Type '" + theDataTypeName + "' was successfully processed.", response.getReturnMessage());
        assertEquals(theDataTypeName, response.getDataType().getTypeName());
        assertEquals(newDataTypeType, response.getDataType().getType());

        final String updatedDataTypeType = "updatedType";
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setTypeName(theDataTypeName);
        request.setType(updatedDataTypeType);

        final ProcessDataTypeResponse updateResponse = bean.processDataType(request);
        assertTrue(updateResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());
        assertEquals("The Data Type '" + theDataTypeName + "' was successfully processed.", updateResponse.getReturnMessage());
        assertEquals(theDataTypeName, updateResponse.getDataType().getTypeName());
        assertEquals(updatedDataTypeType, updateResponse.getDataType().getType());
    }

    @Test
    void testCreateAndRepeatDataType() {
        final ShareBean bean = prepareShareBean();

        final ProcessDataTypeRequest createRequest = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        createRequest.setAction(Action.PROCESS);
        final String aDataTypeName = "newName";
        final String aDataTypeType = "newType";
        createRequest.setTypeName(aDataTypeName);
        createRequest.setType(aDataTypeType);

        final ProcessDataTypeResponse response = bean.processDataType(createRequest);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Data Type '" + aDataTypeName + "' was successfully processed.", response.getReturnMessage());
        assertEquals(aDataTypeName, response.getDataType().getTypeName());
        assertEquals(aDataTypeType, response.getDataType().getType());

        createRequest.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        final ProcessDataTypeResponse updateResponse = bean.processDataType(createRequest);
        assertTrue(updateResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());
        assertEquals("The Data Type '" + aDataTypeName + "' was successfully processed.", updateResponse.getReturnMessage());
        assertEquals(aDataTypeName, updateResponse.getDataType().getTypeName());
        assertEquals(aDataTypeType, updateResponse.getDataType().getType());
    }
}
