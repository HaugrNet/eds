/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.CredentialType;
import org.junit.Test;

import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class AuthenticationTest {

    @Test
    public void testClass() {
        final String name = "Authentication Name";
        final String credentials = "Member Passphrase";
        final CredentialType type = CredentialType.SIGNATURE;

        final Authentication authentication = new Authentication();
        assertThat(authentication.getAccountName(), is(not(name)));
        assertThat(authentication.getCredentialType(), is(not(type)));
        assertThat(authentication.getCredential(), is(not(credentials)));

        authentication.setAccountName(name);
        authentication.setCredentialType(type);
        authentication.setCredential(credentials);
        assertThat(authentication.getAccountName(), is(name));
        assertThat(authentication.getCredentialType(), is(type));
        assertThat(authentication.getCredential(), is(credentials));

        final Map<String, String> errors = authentication.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testValidationOfEmptyObject() {
        final Authentication authentication = new Authentication();
        authentication.setCredentialType(null);
        final Map<String, String> errors = authentication.validate();
        assertThat(errors.size(), is(2));
    }
}
