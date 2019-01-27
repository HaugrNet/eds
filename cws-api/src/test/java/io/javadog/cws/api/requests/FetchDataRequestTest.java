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
import static org.hamcrest.CoreMatchers.nullValue;
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
public final class FetchDataRequestTest {

    @Test
    public void testClassflow() {
        final String circleId = UUID.randomUUID().toString();
        final String dataId = UUID.randomUUID().toString();

        final FetchDataRequest request = new FetchDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(circleId);
        request.setDataId(dataId);
        request.setPageNumber(43);
        request.setPageSize(56);

        assertThat(request.validate().isEmpty(), is(true));
        assertThat(request.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(TestUtilities.convert(request.getCredential()), is(Constants.ADMIN_ACCOUNT));
        assertThat(request.getCircleId(), is(circleId));
        assertThat(request.getDataId(), is(dataId));
        assertThat(request.getPageNumber(), is(43));
        assertThat(request.getPageSize(), is(56));
    }

    @Test
    public void testEmptyClass() {
        final FetchDataRequest request = new FetchDataRequest();
        final Map<String, String> errors = request.validate();

        assertThat(request.getDataId(), is(nullValue()));
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Session (Credential) is missing."));
        assertThat(errors.get(Constants.FIELD_IDS), is("Either a Circle or Data Id must be provided."));
    }

    @Test
    public void testClassWithInvalidValues() {
        final FetchDataRequest request = new FetchDataRequest();
        request.setCircleId("Invalid Circle Id");
        request.setDataId("Invalid Data Id");
        request.setPageNumber(-1);
        request.setPageSize(Constants.MAX_PAGE_SIZE + 1);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(5));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Session (Credential) is missing."));
        assertThat(errors.get(Constants.FIELD_CIRCLE_ID), is("The Circle Id is invalid."));
        assertThat(errors.get(Constants.FIELD_DATA_ID), is("The Data Id is invalid."));
        assertThat(errors.get(Constants.FIELD_PAGE_NUMBER), is("The Page Number must be a positive number, starting with 1."));
        assertThat(errors.get(Constants.FIELD_PAGE_SIZE), is("The Page Size must be a positive number, starting with 1."));
    }
}
