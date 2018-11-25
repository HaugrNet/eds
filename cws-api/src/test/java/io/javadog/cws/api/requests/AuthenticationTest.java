/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.TestUtilities;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import org.junit.Test;

import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class AuthenticationTest {

    private static final byte[] CREDENTIAL = {};

    @Test
    public void testClassflow() {
        final String name = "Authentication Name";
        final String credentials = "Member Passphrase";
        final CredentialType type = CredentialType.SIGNATURE;

        final Authentication authentication = new Authentication();
        assertThat(authentication.getAccountName(), is(not(name)));
        assertThat(authentication.getCredentialType(), is(not(type)));
        assertThat(authentication.getCredential(), is(not(credentials)));

        authentication.setAccountName(name);
        authentication.setCredentialType(type);
        authentication.setCredential(TestUtilities.convert(credentials));
        assertThat(authentication.getAccountName(), is(name));
        assertThat(authentication.getCredentialType(), is(type));
        assertThat(TestUtilities.convert(authentication.getCredential()), is(credentials));

        final Map<String, String> errors = authentication.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testEmptyClass() {
        final Authentication authentication = new Authentication();
        authentication.setAccountName("");
        authentication.setCredentialType(null);

        final Map<String, String> errors = authentication.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
    }

    @Test
    public void testSessionEmptyCredential() {
        final Authentication authentication = new Authentication();
        authentication.setCredentialType(CredentialType.SESSION);

        final Map<String, String> errors = authentication.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Session (Credential) is missing."));
    }

    @Test
    public void testEmptyClassEmptyCredential() {
        final byte[] credential = CREDENTIAL;
        final Authentication authentication = new Authentication();
        authentication.setAccountName("");
        authentication.setCredential(credential);
        authentication.setCredentialType(null);

        final Map<String, String> errors = authentication.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
    }
}
