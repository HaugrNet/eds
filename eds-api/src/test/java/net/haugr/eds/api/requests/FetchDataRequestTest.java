/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.api.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.TestUtilities;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class FetchDataRequestTest {

    @Test
    void testClassFlow() {
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

    @Test
    void testFetchCriteria() {
        final FetchDataRequest request = new FetchDataRequest();
        request.setAccountName("My Account");
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(TestUtilities.convert("My Password"));

        final Map<String, String> errors1 = request.validate();
        assertEquals(1, errors1.size());
        assertEquals("Either a Circle Id, Data Id, or Data Name must be provided.", errors1.get(Constants.FIELD_IDS));

        request.setDataName("My Data");
        final Map<String, String> errors2 = request.validate();
        assertEquals(0, errors2.size());

        request.setDataId(UUID.randomUUID().toString());
        final Map<String, String> errors3 = request.validate();
        assertEquals(0, errors3.size());
    }
}
