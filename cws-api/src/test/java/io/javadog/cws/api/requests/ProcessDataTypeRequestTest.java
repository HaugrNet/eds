/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.TestUtilities;
import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import org.junit.Test;

import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessDataTypeRequestTest {

    @Test
    public void testClassflow() {
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

        assertThat(errors.isEmpty(), is(true));
        assertThat(request.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(TestUtilities.convert(request.getCredential()), is(Constants.ADMIN_ACCOUNT));
        assertThat(request.getAction(), is(Action.PROCESS));
        assertThat(request.getTypeName(), is(typeName));
        assertThat(request.getType(), is(type));
    }

    @Test
    public void testEmptyClass() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAction(null);
        final Map<String, String> errors = request.validate();

        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_ACTION), is("No action has been provided."));
    }

    @Test
    public void testInvalidAction() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_ACTION), is("Not supported Action has been provided."));
    }

    @Test
    public void testActionProcess() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setTypeName("The TypeName");
        request.setType("The Type");
        request.setAction(Action.PROCESS);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionProcessFail() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.PROCESS);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_TYPENAME), is("The name of the DataType is missing or invalid."));
        assertThat(errors.get(Constants.FIELD_TYPE), is("The type of the DataType is missing or invalid."));
    }

    @Test
    public void testActionDelete() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setTypeName("The TypeName to Delete");
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionDeleteFail() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setTypeName("");
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_TYPENAME), is("The name of the DataType is missing or invalid."));
    }
}
