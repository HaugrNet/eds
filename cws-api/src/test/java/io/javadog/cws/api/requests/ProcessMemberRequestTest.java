package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.dtos.Member;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberRequestTest {

    @Test
    public void testClass() {
        final String admin = "admin";
        final char[] credentials = UUID.randomUUID().toString().toCharArray();

        final Authentication authentication = new Authentication();
        authentication.setName(admin);
        authentication.setCredentialType(CredentialType.PASSPHRASE);
        authentication.setCredentials(credentials);

        final Member member = new Member();
        member.setAuthentication(authentication);

        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setName(admin);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredentials(credentials);
        request.setAction(Action.PROCESS);
        request.setMember(member);

        assertThat(request.getName(), is(admin));
        assertThat(request.getCredentialType(), is(CredentialType.PASSPHRASE));
        assertThat(request.getCredentials(), is(credentials));
        assertThat(request.getAction(), is(Action.PROCESS));
        assertThat(request.getMember(), is(member));

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testEmptyObject() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAction() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAction(Action.NONE);
    }
}
