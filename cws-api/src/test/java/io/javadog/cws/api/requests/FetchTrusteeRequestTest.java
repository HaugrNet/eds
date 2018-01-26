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
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchTrusteeRequestTest {

    @Test
    public void testClassflow() {
        final String circleId = UUID.randomUUID().toString();

        final FetchTrusteeRequest request = new FetchTrusteeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(circleId);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
        assertThat(request.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(TestUtilities.convert(request.getCredential()), is(Constants.ADMIN_ACCOUNT));
        assertThat(request.getCircleId(), is(circleId));
    }

    @Test
    public void testEmptyClass() {
        final FetchTrusteeRequest request = new FetchTrusteeRequest();
        final Map<String, String> errors = request.validate();

        assertThat(errors.isEmpty(), is(false));
        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_CIRCLE_ID), is("The Circle Id is missing or invalid."));
    }

    @Test
    public void testClassWithInvalidValues() {
        final String circleId = "Invalid Circle Id";

        final FetchTrusteeRequest request = new FetchTrusteeRequest();
        request.setCircleId(circleId);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(false));
        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_CIRCLE_ID), is("The Circle Id is missing or invalid."));
    }
}
