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
public final class FetchCircleRequestTest {

    @Test
    public void testClassflow() {
        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        final Map<String, String> errors = request.validate();

        assertThat(errors.isEmpty(), is(true));
        assertThat(request.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(TestUtilities.convert(request.getCredential()), is(Constants.ADMIN_ACCOUNT));
    }

    @Test
    public void testEmptyClass() {
        final FetchCircleRequest request = new FetchCircleRequest();
        final Map<String, String> errors = request.validate();

        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
    }
}
