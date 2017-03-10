package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchMemberRequestTest {

    @Test
    public void testClass() {
        final String memberId = UUID.randomUUID().toString();

        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        request.setMemberId(memberId);

        assertThat(request.getMemberId(), is(memberId));
        assertThat(request.validate(), is(not(nullValue())));
        assertThat(request.validate().size(), is(0));
    }

    @Test
    public void testClassWithoutMemberId() {
        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());

        assertThat(request.getMemberId(), is(nullValue()));
        assertThat(request.validate(), is(not(nullValue())));
        assertThat(request.validate().size(), is(0));
    }

    @Test
    public void testClassWithForcedMemberId() throws NoSuchFieldException, IllegalAccessException {
        final String memberId = Constants.ADMIN_ACCOUNT;

        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());

        final Field field = request.getClass().getDeclaredField("memberId");
        field.setAccessible(true);
        field.set(request, memberId);

        final Map<String, String> errors = request.validate();

        assertThat(request.getMemberId(), is(memberId));
        assertThat(errors, is(not(nullValue())));
        assertThat(errors.size(), is(1));
        assertThat(errors.get("memberId"), is("The Member Id is invalid."));
    }

    @Test
    public void testEmptyClass() {
        final FetchMemberRequest request = new FetchMemberRequest();
        final Map<String, String> errors = request.validate();

        assertThat(request.getMemberId(), is(nullValue()));
        assertThat(errors, is(not(nullValue())));
        assertThat(errors.size(), is(3));
        assertThat(errors.get("credentialType"), is("CredentialType is missing, null or invalid."));
        assertThat(errors.get("credential"), is("Credential is missing, null or invalid."));
        assertThat(errors.get("account"), is("Account is missing, null or invalid."));
    }
}
