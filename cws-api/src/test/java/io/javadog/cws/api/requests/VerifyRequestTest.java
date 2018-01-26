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

import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class VerifyRequestTest {

    @Test
    public void testClassflow() {
        final String signature = "Signature";
        final byte[] data = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5 };

        final VerifyRequest request = new VerifyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setSignature(signature);
        request.setData(data);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
        assertThat(request.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(TestUtilities.convert(request.getCredential()), is(Constants.ADMIN_ACCOUNT));
        assertThat(request.getSignature(), is(signature));
        assertThat(request.getData(), is(data));
    }

    @Test
    public void testEmptyClass() {
        final VerifyRequest request = new VerifyRequest();
        final Map<String, String> errors = request.validate();

        assertThat(errors.size(), is(4));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_SIGNATURE), is("The Signature is missing."));
        assertThat(errors.get(Constants.FIELD_DATA), is("The Data Object to check the Signature against is missing."));
    }

    @Test
    public void testClassWithInvalidValues() {
        final VerifyRequest request = new VerifyRequest();
        request.setSignature("");
        request.setData(null);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(4));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_SIGNATURE), is("The Signature is missing."));
        assertThat(errors.get(Constants.FIELD_DATA), is("The Data Object to check the Signature against is missing."));
    }
}
