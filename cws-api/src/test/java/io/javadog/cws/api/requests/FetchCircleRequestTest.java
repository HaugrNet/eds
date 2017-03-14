package io.javadog.cws.api.requests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchCircleRequestTest {

    @Rule
    public ExpectedException excepctedException = ExpectedException.none();

    @Test
    public void testClass() {
        final String circleId = UUID.randomUUID().toString();

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        request.setCircleId(circleId);

        assertThat(request.getCircleId(), is(circleId));
        assertThat(request.validate(), is(not(nullValue())));
        assertThat(request.validate().size(), is(0));
    }

    @Test
    public void testClassWithoutCircleId() {
        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());

        assertThat(request.getCircleId(), is(nullValue()));
        assertThat(request.validate(), is(not(nullValue())));
        assertThat(request.validate().size(), is(0));
    }

    @Test
    public void testInvalidCircleId() {
        excepctedException.expect(IllegalArgumentException.class);
        excepctedException.expectMessage("The value for 'circleId' is not matching the required pattern '[\\da-z]{8}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{12}'.");

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setCircleId("invalidCircleId");
    }

    @Test
    public void testClassWithForcedCircleId() throws NoSuchFieldException, IllegalAccessException {
        final String circleId = Constants.ADMIN_ACCOUNT;

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());

        final Field field = request.getClass().getDeclaredField("circleId");
        field.setAccessible(true);
        field.set(request, circleId);

        final Map<String, String> errors = request.validate();

        assertThat(request.getCircleId(), is(circleId));
        assertThat(errors, is(not(nullValue())));
        assertThat(errors.size(), is(1));
        assertThat(errors.get("circleId"), is("The Circle Id is invalid."));
    }

    @Test
    public void testEmptyClass() {
        final FetchCircleRequest request = new FetchCircleRequest();
        final Map<String, String> errors = request.validate();

        assertThat(request.getCircleId(), is(nullValue()));
        assertThat(errors, is(not(nullValue())));
        assertThat(errors.size(), is(3));
        assertThat(errors.get("credentialType"), is("CredentialType is missing, null or invalid."));
        assertThat(errors.get("credential"), is("Credential is missing, null or invalid."));
        assertThat(errors.get("account"), is("Account is missing, null or invalid."));
    }
}
