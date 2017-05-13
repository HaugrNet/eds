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
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.common.exceptions.AuthorizationException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessDataTypeServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors: Key: credentialTypeError: CredentialType is missing, null or invalid.\n" +
                        "Key: credentialError: Credential is missing, null or invalid.\n" +
                        "Key: dataTypeError: Value is missing, null or invalid.\n" +
                        "Key: accountError: Account is missing, null or invalid.");

        final Serviceable<ProcessDataTypeResponse, ProcessDataTypeRequest> service = prepareService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        assertThat(request.getAccount(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testInvokeWithoutAnything() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors: Key: dataTypeError: Value is missing, null or invalid.");

        final Serviceable<ProcessDataTypeResponse, ProcessDataTypeRequest> service = prepareService();
        final ProcessDataTypeRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        assertThat(request.getDataType(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testNotAuthorizedRequest() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING,
                "Cannot complete this request, as it is only allowed for the System Administrator.");

        final Serviceable<ProcessDataTypeResponse, ProcessDataTypeRequest> service = prepareService();
        final ProcessDataTypeRequest request = buildRequestWithCredentials("member1");
        final DataType dataType = buildDataType("MyDataType", "The Data Type");
        request.setDataType(dataType);
        assertThat(request.getDataType(), is(not(nullValue())));

        service.perform(request);
    }

    @Test
    public void testUpdateRestrictedDataType() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING,
                "It is not permitted to update the DataType 'folder'.");

        final Serviceable<ProcessDataTypeResponse, ProcessDataTypeRequest> service = prepareService();
        final ProcessDataTypeRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        final DataType dataType = buildDataType("folder", "alternative folder");
        request.setDataType(dataType);
        assertThat(request.getDataType(), is(not(nullValue())));

        service.perform(request);
    }

    @Test
    public void testCreateAndDeleteDataType() {
        final Serviceable<ProcessDataTypeResponse, ProcessDataTypeRequest> service = prepareService();
        final ProcessDataTypeRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        final String theDataTypeName = "dataTypeToDelete";
        final String newDataTypeType = "The Type information";
        final DataType dataType = buildDataType(theDataTypeName, newDataTypeType);
        request.setDataType(dataType);
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
        final Serviceable<ProcessDataTypeResponse, ProcessDataTypeRequest> service = prepareService();
        final ProcessDataTypeRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        final String theDataTypeName = "unknownDataType";
        final String newDataTypeType = "The Type information";
        final DataType dataType = buildDataType(theDataTypeName, newDataTypeType);
        request.setDataType(dataType);
        final ProcessDataTypeResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("No records were found with the name '" + theDataTypeName + "'."));
    }

    @Test
    public void testCreateAndUpdateDataType() {
        final Serviceable<ProcessDataTypeResponse, ProcessDataTypeRequest> service = prepareService();
        final ProcessDataTypeRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        final String theDataTypeName = "newName";
        final String newDataTypeType = "newType";
        final DataType dataType = buildDataType(theDataTypeName, newDataTypeType);
        request.setDataType(dataType);

        final ProcessDataTypeResponse response = service.perform(request);
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getDataType().getName(), is(theDataTypeName));
        assertThat(response.getDataType().getType(), is(newDataTypeType));

        final String updatedDataTypeType = "updatedType";
        final DataType updatedDataType = buildDataType(theDataTypeName, updatedDataTypeType);
        request.setDataType(updatedDataType);
        final ProcessDataTypeResponse updateResponse = service.perform(request);
        assertThat(updateResponse, is(not(nullValue())));
        assertThat(updateResponse.isOk(), is(true));
        assertThat(updateResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(updateResponse.getReturnMessage(), is("Ok"));
        assertThat(updateResponse.getDataType().getName(), is(theDataTypeName));
        assertThat(updateResponse.getDataType().getType(), is(updatedDataTypeType));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private Serviceable<ProcessDataTypeResponse, ProcessDataTypeRequest> prepareService() {
        return new ProcessDataTypeService(settings, entityManager);
    }

    private static ProcessDataTypeRequest buildRequestWithCredentials(final String account) {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccount(account);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(account.toCharArray());

        return request;
    }

    private static DataType buildDataType(final String name, final String type) {
        final DataType dataType = new DataType();
        dataType.setName(name);
        dataType.setType(type);

        return dataType;
    }
}
