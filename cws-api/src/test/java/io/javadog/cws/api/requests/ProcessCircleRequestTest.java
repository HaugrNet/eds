package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.dtos.Circle;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessCircleRequestTest {

    @Test
    public void testClass() {
        final String account = Constants.ADMIN_ACCOUNT;
        final char[] credentials = UUID.randomUUID().toString().toCharArray();

        final Circle circle = new Circle();
        circle.setName("my Circle");

        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAction(Action.PROCESS);
        request.setCircle(circle);
        request.setAccount(account);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(credentials);
        request.validate();

        assertThat(request.getAction(), is(Action.PROCESS));
        assertThat(request.getCircle(), is(circle));
        assertThat(request.getAccount(), is(account));
        assertThat(request.getCredentialType(), is(CredentialType.PASSPHRASE));
        assertThat(request.getCredential(), is(credentials));
    }

    @Test
    public void testEmptyObject() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAction() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAction(Action.NONE);
    }
}
