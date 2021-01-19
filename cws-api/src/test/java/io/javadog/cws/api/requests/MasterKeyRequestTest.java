/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.Constants;
import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class MasterKeyRequestTest {

    @Test
    void testClass() {
        final String secret = "secret";
        final String url = "https://my.super.secret.url/to/a/master/key";

        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setSecret(convert(secret));
        request.setUrl(url);

        assertEquals(secret, convert(request.getSecret()));
        assertEquals(url, request.getUrl());
    }

    @Test
    void testClassFlow() {
        final String secret = "secret";
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setSecret(convert(secret));

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
        assertEquals("secret", convert(request.getSecret()));
    }

    @Test
    void testEmptyClass() {
        final MasterKeyRequest request = new MasterKeyRequest();

        final Map<String, String> errors = request.validate();
        assertEquals(3, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("Either the secret or the URL must be given to alter the MasterKey.", errors.get(Constants.FIELD_SECRET));
        assertEquals("Either the secret or the URL must be given to alter the MasterKey.", errors.get(Constants.FIELD_URL));
    }

    @Test
    void testSettingBothSecretAndUrl() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setSecret(convert("secret"));
        request.setUrl("url");

        final Map<String, String> errors = request.validate();
        assertEquals(2, errors.size());
        assertEquals("Either the secret or the URL must be given to alter the MasterKey.", errors.get(Constants.FIELD_SECRET));
        assertEquals("Either the secret or the URL must be given to alter the MasterKey.", errors.get(Constants.FIELD_URL));
    }

    @Test
    void testInvalidSecret() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setSecret(convert(""));

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("The secret for the MasterKey is missing.", errors.get(Constants.FIELD_SECRET));
    }

    @Test
    void testValidHttpUrl() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setUrl("https://javadog.io/");

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidFileUrl() {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setUrl("file://" + File.separator + System.getProperty("java.io.tmpdir") + File.separator + "file_name.dat");

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @ValueSource(strings = {
            "",
            "protocol:///path/to/file",
            "/path/to/file",
            "https://weird.domain.name/not\tAllowed\nPath"
    })
    @ParameterizedTest
    void testProtocolUrl(final String url) {
        final MasterKeyRequest request = new MasterKeyRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(convert(Constants.ADMIN_ACCOUNT));
        request.setUrl(url);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertTrue(errors.get(Constants.FIELD_URL).startsWith("The URL field is invalid"));
    }
}
