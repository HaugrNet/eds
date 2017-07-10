package io.javadog.cws.api.requests;

import static io.javadog.cws.api.ReflectiveTesting.reflectiveCorrection;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.dtos.DataType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessDataTypeRequestTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testClass() {
        final DataType dataType = new DataType();
        dataType.setName("Data Type Name");
        dataType.setType("Data Type Type");

        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        request.setDataType(dataType);

        assertThat(request.getDataType(), is(dataType));

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testSetNullAction() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The value for 'action' may not be null.");

        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAction(null);
    }

    @Test
    public void testSetInvalidAction() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The value for 'action' is not allowed.");

        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAction(Action.REKEY);
    }

    @Test
    public void testSetNullDataType() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The value for 'dataType' may not be null.");

        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setDataType(null);
    }

    @Test
    public void testDefaultValidation() {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(4));
        assertThat(errors.get("account"), is("Account is missing, null or invalid."));
        assertThat(errors.get("credentialType"), is("CredentialType is missing, null or invalid."));
        assertThat(errors.get("credential"), is("Credential is missing, null or invalid."));
        assertThat(errors.get("dataType"), is("Value is missing, null or invalid."));
    }

    @Test
    public void testValidateInvalidAction() throws NoSuchFieldException, IllegalAccessException {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        reflectiveCorrection(request, "action", Action.REKEY);
        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(5));
        assertThat(errors.get("account"), is("Account is missing, null or invalid."));
        assertThat(errors.get("credentialType"), is("CredentialType is missing, null or invalid."));
        assertThat(errors.get("credential"), is("Credential is missing, null or invalid."));
        assertThat(errors.get("dataType"), is("Value is missing, null or invalid."));
        assertThat(errors.get("action"), is("Invalid Action has been provided."));
    }
}
