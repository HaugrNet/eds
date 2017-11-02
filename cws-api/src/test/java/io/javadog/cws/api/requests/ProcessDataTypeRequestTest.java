package io.javadog.cws.api.requests;

import static io.javadog.cws.api.ReflectiveTesting.reflectiveCorrection;
import static io.javadog.cws.api.common.Constants.FIELD_ACCOUNT_NAME;
import static io.javadog.cws.api.common.Constants.FIELD_ACTION;
import static io.javadog.cws.api.common.Constants.FIELD_CREDENTIAL;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
    public void testClass() {
        final String name = "DataType Name";
        final String type = "DataType Type";

        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        request.setTypeName(name);
        request.setType(type);

        assertThat(request.getTypeName(), is(name));
        assertThat(request.getType(), is(type));

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testDefaultValidation() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(4));
        assertThat(errors.get(FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(FIELD_CREDENTIAL), is("The Credential is missing."));
    }

    @Test
    public void testValidateInvalidAction() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        reflectiveCorrection(request, "action", Action.REKEY);
        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(3));
        assertThat(errors.get(FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(FIELD_ACTION), is("Invalid Action provided."));
    }
}
