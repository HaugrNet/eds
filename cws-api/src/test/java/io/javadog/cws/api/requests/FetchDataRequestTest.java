/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Constants.FIELD_ACCOUNT_NAME;
import static io.javadog.cws.api.common.Constants.FIELD_CIRCLE_ID;
import static io.javadog.cws.api.common.Constants.FIELD_CREDENTIAL;
import static io.javadog.cws.api.common.Constants.FIELD_DATA_ID;
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
public final class FetchDataRequestTest {

    @Test
    public void testClass() {
        final String objectId = UUID.randomUUID().toString();

        final FetchDataRequest request = new FetchDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setDataId(objectId);

        assertThat(request.getDataId(), is(objectId));
        assertThat(request.validate(), is(not(nullValue())));
        assertThat(request.validate().size(), is(0));
    }

    @Test
    public void testClassWithoutDataId() {
        final FetchDataRequest request = new FetchDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        final Map<String, String> errors = request.validate();

        assertThat(request.getDataId(), is(nullValue()));
        assertThat(errors, is(not(nullValue())));
        assertThat(errors.size(), is(1));
        assertThat(errors.get("circleId"), is("Either a Circle or Data Id must be provided."));
    }

    @Test
    public void testClassWithForcedDataId() throws NoSuchFieldException, IllegalAccessException {
        final String dataId = Constants.ADMIN_ACCOUNT;

        final FetchDataRequest request = new FetchDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT);

        final Field field = request.getClass().getDeclaredField("dataId");
        field.setAccessible(true);
        field.set(request, dataId);

        final Map<String, String> errors = request.validate();

        assertThat(request.getDataId(), is(dataId));
        assertThat(errors, is(not(nullValue())));
        assertThat(errors.size(), is(1));
        assertThat(errors.get(FIELD_DATA_ID), is("The Data Id is invalid."));
    }

    @Test
    public void testEmptyClass() {
        final FetchDataRequest request = new FetchDataRequest();
        final Map<String, String> errors = request.validate();

        assertThat(request.getDataId(), is(nullValue()));
        assertThat(errors, is(not(nullValue())));
        assertThat(errors.size(), is(3));
        assertThat(errors.get(FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(FIELD_CIRCLE_ID), is("Either a Circle or Data Id must be provided."));
    }
}
