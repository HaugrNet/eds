/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
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
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.common.exceptions.AuthorizationException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.model.DatabaseSetup;
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
                        "\nKey: credentialType, Error: CredentialType is missing, null or invalid." +
                        "\nKey: credential, Error: Credential is missing, null or invalid." +
                        "\nKey: account, Error: Account is missing, null or invalid.");

        final FetchDataTypeService service = new FetchDataTypeService(settings, entityManager);
        final FetchDataTypeRequest request = new FetchDataTypeRequest();
        assertThat(request.getAccount(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testEmptyProcessRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credentialType, Error: CredentialType is missing, null or invalid." +
                        "\nKey: credential, Error: Credential is missing, null or invalid." +
                        "\nKey: dataType, Error: Value is missing, null or invalid." +
                        "\nKey: account, Error: Account is missing, null or invalid.");

        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        assertThat(request.getAccount(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testAdminFetchRequest() {
        final FetchDataTypeService service = new FetchDataTypeService(settings, entityManager);
        final FetchDataTypeRequest request = prepareRequest(FetchDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchDataTypeResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getTypes().size(), is(2));
        assertThat(response.getTypes().get(0).getName(), is(Constants.DATA_TYPENAME));
        assertThat(response.getTypes().get(0).getType(), is("Data Object"));
        assertThat(response.getTypes().get(1).getName(), is(Constants.FOLDER_TYPENAME));
        assertThat(response.getTypes().get(1).getType(), is("Folder"));
    }

    @Test
    public void testInvokeWithoutAnything() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:\nKey: dataType, Error: Value is missing, null or invalid.");

        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        assertThat(request.getName(), is(nullValue()));
        assertThat(request.getType(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testNotAuthorizedRequest() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING,
                "Cannot complete this request, as it is only allowed for the System Administrator.");

        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, MEMBER_1);
        request.setType("MyDataType");
        request.setName("The Data Type");
        assertThat(request.getName(), is(not(nullValue())));
        assertThat(request.getType(), is(not(nullValue())));

        service.perform(request);
    }

    @Test
    public void testUpdateRestrictedDataTypeFolder() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING,
                "It is not permitted to update the DataType '" + Constants.FOLDER_TYPENAME + "'.");

        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setType(Constants.FOLDER_TYPENAME);
        request.setName("alternative folder");
        assertThat(request.getName(), is(not(nullValue())));
        assertThat(request.getType(), is(not(nullValue())));

        service.perform(request);
    }

    @Test
    public void testUpdateRestrictedDataTypeData() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING,
                "It is not permitted to update the DataType '" + Constants.DATA_TYPENAME + "'.");

        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setType(Constants.DATA_TYPENAME);
        request.setName("alternative data");
        assertThat(request.getName(), is(not(nullValue())));
        assertThat(request.getType(), is(not(nullValue())));

        service.perform(request);
    }

    @Test
    public void testCreateAndDeleteDataType() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        final String theDataTypeName = "dataTypeToDelete";
        final String newDataTypeType = "The Type information";
        request.setName(theDataTypeName);
        request.setType(newDataTypeType);
        final ProcessDataTypeResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));

        request.setAction(Action.DELETE);
        final ProcessDataTypeResponse deletedResponse = service.perform(request);
        assertThat(deletedResponse, is(not(nullValue())));
        assertThat(deletedResponse.isOk(), is(true));
        assertThat(deletedResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(deletedResponse.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testDeleteUnknownDataType() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        final String theDataTypeName = "unknownDataType";
        final String newDataTypeType = "The Type information";
        request.setName(theDataTypeName);
        request.setType(newDataTypeType);
        final ProcessDataTypeResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("No records were found with the name '" + theDataTypeName + "'."));
    }

    @Test
    public void testCreateAndUpdateDataType() {
        final ProcessDataTypeService service = new ProcessDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        final String theDataTypeName = "newName";
        final String newDataTypeType = "newType";
        request.setName(theDataTypeName);
        request.setType(newDataTypeType);

        final ProcessDataTypeResponse response = service.perform(request);
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getDataType().getName(), is(theDataTypeName));
        assertThat(response.getDataType().getType(), is(newDataTypeType));

        final String updatedDataTypeType = "updatedType";
        request.setName(theDataTypeName);
        request.setType(updatedDataTypeType);
        final ProcessDataTypeResponse updateResponse = service.perform(request);
        assertThat(updateResponse, is(not(nullValue())));
        assertThat(updateResponse.isOk(), is(true));
        assertThat(updateResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(updateResponse.getReturnMessage(), is("Ok"));
        assertThat(updateResponse.getDataType().getName(), is(theDataTypeName));
        assertThat(updateResponse.getDataType().getType(), is(updatedDataTypeType));
    }
}
