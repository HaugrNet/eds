/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.dtos.Authentication;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberRequestTest {

    @Test
    public void testClass() {
        final String account = Constants.ADMIN_ACCOUNT;
        final String credentials = UUID.randomUUID().toString();

        final Authentication authentication = new Authentication();
        authentication.setAccount(account);
        authentication.setCredentialType(CredentialType.PASSPHRASE);
        authentication.setCredential(credentials);

        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccount(account);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(credentials);
        request.setAction(Action.PROCESS);
        request.setAccountName("new Account");

        assertThat(request.getAccount(), is(account));
        assertThat(request.getCredentialType(), is(CredentialType.PASSPHRASE));
        assertThat(request.getCredential(), is(credentials));
        assertThat(request.getAction(), is(Action.PROCESS));
        assertThat(request.getAccountName(), is("new Account"));

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testEmptyObject() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(4));
        assertThat(errors.get("action"), is("No action has been provided."));
        assertThat(errors.get("credentialType"), is("CredentialType is missing, null or invalid."));
        assertThat(errors.get("credential"), is("Credential is missing, null or invalid."));
        assertThat(errors.get("account"), is("Account is missing, null or invalid."));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAction() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAction(Action.NONE);
    }
}
