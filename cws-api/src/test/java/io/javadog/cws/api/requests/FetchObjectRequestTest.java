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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchObjectRequestTest {

    @Test
    public void testClass() {
        final String objectId = UUID.randomUUID().toString();

        final FetchObjectRequest request = new FetchObjectRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        request.setObjectId(objectId);

        assertThat(request.getObjectId(), is(objectId));
        assertThat(request.validate(), is(not(nullValue())));
        assertThat(request.validate().size(), is(0));
    }

    @Test
    public void testClassWithoutObjectId() {
        final FetchObjectRequest request = new FetchObjectRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());

        assertThat(request.getObjectId(), is(nullValue()));
        assertThat(request.validate(), is(not(nullValue())));
        assertThat(request.validate().size(), is(0));
    }

    @Test
    public void testClassWithForcedObjectId() throws NoSuchFieldException, IllegalAccessException {
        final String objectId = Constants.ADMIN_ACCOUNT;

        final FetchObjectRequest request = new FetchObjectRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());

        final Field field = request.getClass().getDeclaredField("objectId");
        field.setAccessible(true);
        field.set(request, objectId);

        final Map<String, String> errors = request.validate();

        assertThat(request.getObjectId(), is(objectId));
        assertThat(errors, is(not(nullValue())));
        assertThat(errors.size(), is(1));
        assertThat(errors.get("objectId"), is("The Object Id is invalid."));
    }

    @Test
    public void testEmptyClass() {
        final FetchObjectRequest request = new FetchObjectRequest();
        final Map<String, String> errors = request.validate();

        assertThat(request.getObjectId(), is(nullValue()));
        assertThat(errors, is(not(nullValue())));
        assertThat(errors.size(), is(3));
        assertThat(errors.get("credentialType"), is("CredentialType is missing, null or invalid."));
        assertThat(errors.get("credential"), is("Credential is missing, null or invalid."));
        assertThat(errors.get("account"), is("Account is missing, null or invalid."));
    }
}
