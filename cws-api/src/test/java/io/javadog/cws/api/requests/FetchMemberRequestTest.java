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
public final class FetchMemberRequestTest {

    @Test
    public void testClassflow() {
        final String memberId = UUID.randomUUID().toString();

        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setMemberId(memberId);
        final Map<String, String> errors = request.validate();

        assertThat(errors.isEmpty(), is(true));
        assertThat(request.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(TestUtilities.convert(request.getCredential()), is(Constants.ADMIN_ACCOUNT));
        assertThat(request.getMemberId(), is(memberId));
    }

    @Test
    public void testEmptyClass() {
        final FetchMemberRequest request = new FetchMemberRequest();
        final Map<String, String> errors = request.validate();

        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Session (Credential) is missing."));
    }

    @Test
    public void testClassWithInvalidValues() {
        final FetchMemberRequest request = new FetchMemberRequest();
        request.setMemberId("Invalid Id");

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Session (Credential) is missing."));
        assertThat(errors.get(Constants.FIELD_MEMBER_ID), is("The Member Id is invalid."));
    }
}
