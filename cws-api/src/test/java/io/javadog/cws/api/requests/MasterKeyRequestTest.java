/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.TestUtilities.convert;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import java.io.File;
import java.util.Map;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MasterKeyRequestTest {

    @Test
    public void testClass() {
        final String secret = "secret";
        final String url = "https://my.super.secret.url/to/a/master/key";

        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setSecret(convert(secret));
        request.setUrl(url);

        assertThat(convert(request.getSecret()), is(secret));
        assertThat(request.getUrl(), is(url));
    }

    @Test
    public void testClassflow() {
        final String secret = "secret";
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setSecret(convert(secret));

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
        assertThat(convert(request.getSecret()), is("secret"));
    }

    @Test
    public void testEmptyClass() {
        final MasterKeyRequest request = new MasterKeyRequest();

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Session (Credential) is missing."));
        assertThat(errors.get(Constants.FIELD_SECRET), is("Either the secret or the URL must be given to alter the MasterKey."));
        assertThat(errors.get(Constants.FIELD_URL), is("Either the secret or the URL must be given to alter the MasterKey."));
    }

    @Test
    public void testSettingBothSecretAndUrl() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setSecret(convert("secret"));
        request.setUrl("url");

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_SECRET), is("Either the secret or the URL must be given to alter the MasterKey."));
        assertThat(errors.get(Constants.FIELD_URL), is("Either the secret or the URL must be given to alter the MasterKey."));
    }

    @Test
    public void testInvalidSecret() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setSecret(convert(""));

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_SECRET), is("The secret for the MasterKey is missing."));
    }

    @Test
    public void testValidHttpUrl() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setUrl("https://javadog.io/");

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testValidFileUrl() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setUrl("file://" + File.separator + System.getProperty("java.io.tmpdir") + File.separator + "file_name.dat");

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testEmptyProtocolUrl() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setUrl("");

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_URL), is("The URL field is invalid - no protocol: "));
    }

    @Test
    public void testInvalidProtocolUrl() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setUrl("protocol:///path/to/file");

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_URL), is("The URL field is invalid - unknown protocol: protocol"));
    }

    @Test
    public void testMissingProtocolUrl() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setUrl("/path/to/file");

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_URL), is("The URL field is invalid - no protocol: /path/to/file"));
    }

    @Test
    public void testInvalidPathUrl() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setUrl("https://weird.domain.name/not\tAllowed\nPath");

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_URL), containsString("The URL field is invalid - Illegal character in path"));
    }
}
