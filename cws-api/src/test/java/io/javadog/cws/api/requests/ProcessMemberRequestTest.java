/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

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
        authentication.setAccountName(account);
        authentication.setCredentialType(CredentialType.PASSPHRASE);
        authentication.setCredential(credentials);

        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(account);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(credentials);
        request.setAction(Action.PROCESS);
        request.setNewAccountName("new Account");

        assertThat(request.getAccountName(), is(account));
        assertThat(request.getCredentialType(), is(CredentialType.PASSPHRASE));
        assertThat(request.getCredential(), is(credentials));
        assertThat(request.getAction(), is(Action.PROCESS));
        assertThat(request.getNewAccountName(), is("new Account"));

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testEmptyObject() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(3));
        assertThat(errors.get(FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(FIELD_ACTION), is("No action has been provided."));
    }
}
