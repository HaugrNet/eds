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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.TestUtilities;
import io.javadog.cws.api.common.Constants;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class FetchDataRequestTest {

    @Test
    void testClassflow() {
        final String circleId = UUID.randomUUID().toString();
        final String dataId = UUID.randomUUID().toString();
        final String dataName = UUID.randomUUID().toString();

        final FetchDataRequest request = new FetchDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(circleId);
        request.setDataId(dataId);
        request.setDataName(dataName);
        request.setPageNumber(43);
        request.setPageSize(56);

        assertTrue(request.validate().isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, request.getAccountName());
        assertEquals(Constants.ADMIN_ACCOUNT, TestUtilities.convert(request.getCredential()));
        assertEquals(circleId, request.getCircleId());
        assertEquals(dataId, request.getDataId());
        assertEquals(dataName, request.getDataName());
        assertEquals(43, request.getPageNumber());
        assertEquals(56, request.getPageSize());
    }

    @Test
    void testEmptyClass() {
        final FetchDataRequest request = new FetchDataRequest();
        final Map<String, String> errors = request.validate();

        assertNull(request.getDataId());
        assertEquals(2, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("Either a Circle Id, Data Id, or Data Name must be provided.", errors.get(Constants.FIELD_IDS));
    }

    @Test
    void testClassWithInvalidValues() {
        final FetchDataRequest request = new FetchDataRequest();
        request.setCircleId("Invalid Circle Id");
        request.setDataId("Invalid Data Id");
        request.setPageNumber(-1);
        request.setPageSize(Constants.MAX_PAGE_SIZE + 1);

        final Map<String, String> errors = request.validate();
        assertEquals(5, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("The Circle Id is invalid.", errors.get(Constants.FIELD_CIRCLE_ID));
        assertEquals("The Data Id is invalid.", errors.get(Constants.FIELD_DATA_ID));
        assertEquals("The Page Number must be a positive number, starting with 1.", errors.get(Constants.FIELD_PAGE_NUMBER));
        assertEquals("The Page Size must be a positive number, starting with 1.", errors.get(Constants.FIELD_PAGE_SIZE));
    }
}
