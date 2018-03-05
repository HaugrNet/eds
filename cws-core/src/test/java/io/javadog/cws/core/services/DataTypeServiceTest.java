/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

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
import io.javadog.cws.core.exceptions.AuthorizationException;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.exceptions.VerificationException;
import org.junit.Test;

/**
 * <p>Common test class for the Process & Fetch DataType Services.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class DataTypeServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyFetchRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credential, Error: The Credential is missing." +
                        "\nKey: accountName, Error: AccountName is missing, null or invalid.");

        final FetchDataTypeService service = new FetchDataTypeService(settings, entityManager);
        final FetchDataTypeRequest request = new FetchDataTypeRequest();
        assertThat(request.getAccountName(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testEmptyProcessRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credential, Error: The Credential is missing." +
                        "\nKey: accountName, Error: AccountName is missing, null or invalid." +
                        "\nKey: typeName, Error: The name of the DataType is missing or invalid." +
                        "\nKey: type, Error: The type of the DataType is missing or invalid.");

        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        assertThat(request.getAccountName(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testAdminFetchRequest() {
        final FetchDataTypeService service = new FetchDataTypeService(settings, entityManager);
        final FetchDataTypeRequest request = prepareRequest(FetchDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchDataTypeResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getDataTypes().size(), is(2));
        assertThat(response.getDataTypes().get(0).getTypeName(), is(Constants.DATA_TYPENAME));
        assertThat(response.getDataTypes().get(0).getType(), is("Data Object"));
        assertThat(response.getDataTypes().get(1).getTypeName(), is(Constants.FOLDER_TYPENAME));
        assertThat(response.getDataTypes().get(1).getType(), is("Folder"));
    }

    @Test
    public void testInvokeWithoutAnything() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: typeName, Error: The name of the DataType is missing or invalid." +
                        "\nKey: type, Error: The type of the DataType is missing or invalid.");

        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        assertThat(request.getTypeName(), is(nullValue()));
        assertThat(request.getType(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testCircleAdminsAreAuthorized() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, MEMBER_1);
        request.setType("MyDataType");
        request.setTypeName("The Data Type");
        assertThat(request.getTypeName(), is(not(nullValue())));
        assertThat(request.getType(), is(not(nullValue())));

        final ProcessDataTypeResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testNotAuthorizedRequest() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING,
                "The requesting Account is not permitted to Process Data Type.");

        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, MEMBER_5);
        request.setType("MyDataType");
        request.setTypeName("The Data Type");
        assertThat(request.getTypeName(), is(not(nullValue())));
        assertThat(request.getType(), is(not(nullValue())));

        service.perform(request);
    }

    @Test
    public void testUpdateRestrictedDataTypeFolder() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING,
                "It is not permitted to update the DataType '" + Constants.FOLDER_TYPENAME + "'.");

        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setTypeName(Constants.FOLDER_TYPENAME);
        request.setType("alternative folder");
        assertThat(request.getTypeName(), is(not(nullValue())));
        assertThat(request.getType(), is(not(nullValue())));

        service.perform(request);
    }

    @Test
    public void testUpdateRestrictedDataTypeData() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING,
                "It is not permitted to update the DataType '" + Constants.DATA_TYPENAME + "'.");

        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setTypeName(Constants.DATA_TYPENAME);
        request.setType("alternative data");
        assertThat(request.getTypeName(), is(not(nullValue())));
        assertThat(request.getType(), is(not(nullValue())));

        final ProcessDataTypeResponse response = service.perform(request);
        assertThat(response.getReturnMessage(), is(""));
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

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));

        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.DELETE);
        final ProcessDataTypeResponse deletedResponse = service.perform(request);
        assertThat(deletedResponse, is(not(nullValue())));
        assertThat(deletedResponse.isOk(), is(true));
        assertThat(deletedResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(deletedResponse.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testCreateAndDeleteUsedDataType() {
        prepareCause(CWSException.class, ReturnCode.ILLEGAL_ACTION, "The Data Type 'text' cannot be deleted, as it is being actively used.");

        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);

        final ProcessDataTypeRequest addRequest = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        addRequest.setAction(Action.PROCESS);
        addRequest.setTypeName("text");
        addRequest.setType("text/plain");
        final ProcessDataTypeResponse addResponse = service.perform(addRequest);
        assertThat(addResponse.isOk(), is(true));

        final ProcessDataRequest dataRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        dataRequest.setAction(Action.ADD);
        dataRequest.setCircleId(CIRCLE_1_ID);
        dataRequest.setTypeName("text");
        dataRequest.setDataName("file.txt");
        final ProcessDataResponse dataResponse = dataService.perform(dataRequest);
        assertThat(dataResponse.isOk(), is(true));

        final ProcessDataTypeRequest deleteRequest = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        deleteRequest.setAction(Action.DELETE);
        deleteRequest.setTypeName("text");
        service.perform(deleteRequest);
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

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("No records were found with the name '" + theDataTypeName + "'."));
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
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getDataType().getTypeName(), is(theDataTypeName));
        assertThat(response.getDataType().getType(), is(newDataTypeType));

        final String updatedDataTypeType = "updatedType";
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setTypeName(theDataTypeName);
        request.setType(updatedDataTypeType);
        final ProcessDataTypeResponse updateResponse = service.perform(request);
        assertThat(updateResponse.isOk(), is(true));
        assertThat(updateResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(updateResponse.getReturnMessage(), is("Ok"));
        assertThat(updateResponse.getDataType().getTypeName(), is(theDataTypeName));
        assertThat(updateResponse.getDataType().getType(), is(updatedDataTypeType));
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
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getDataType().getTypeName(), is(aDataTypeName));
        assertThat(response.getDataType().getType(), is(aDataTypeType));

        createRequest.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        final ProcessDataTypeResponse updateResponse = dataTypeService.perform(createRequest);
        assertThat(updateResponse.isOk(), is(true));
        assertThat(updateResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(updateResponse.getReturnMessage(), is("Ok"));
        assertThat(updateResponse.getDataType().getTypeName(), is(aDataTypeName));
        assertThat(updateResponse.getDataType().getType(), is(aDataTypeType));
    }
}
