/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.TestUtilities;
import io.javadog.cws.api.common.Constants;
import org.junit.Test;

import java.util.Date;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SignRequestTest {

    @Test
    public void testClassflow() {
        final byte[] data = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5 };
        final Date expires = new Date();

        final SignRequest request = new SignRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setData(data);
        request.setExpires(expires);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
        assertThat(request.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(request.getCredential(), is(TestUtilities.convert(Constants.ADMIN_ACCOUNT)));
        assertThat(request.getData(), is(data));
        assertThat(request.getExpires(), is(expires));
    }

    @Test
    public void testEmptyClass() {
        final SignRequest request = new SignRequest();
        final Map<String, String> errors = request.validate();

        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_DATA), is("The Data Object to create a Signature is missing."));
    }

    @Test
    public void testClassWithInvalidValues() {
        final SignRequest request = new SignRequest();
        request.setData(null);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_DATA), is("The Data Object to create a Signature is missing."));
    }
}
