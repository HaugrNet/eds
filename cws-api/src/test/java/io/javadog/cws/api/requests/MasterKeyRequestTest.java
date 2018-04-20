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

import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MasterKeyRequestTest {

    @Test
    public void testClassflow() {
        final byte[] secret = "secret".getBytes(Charset.defaultCharset());
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setSecret(secret);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
        assertThat(request.getSecret(), is("secret".getBytes(Charset.defaultCharset())));
    }

    @Test
    public void testEmptyClass() {
        final MasterKeyRequest request = new MasterKeyRequest();

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_SECRET), is("The secret for the MasterKey is missing."));
    }
}
