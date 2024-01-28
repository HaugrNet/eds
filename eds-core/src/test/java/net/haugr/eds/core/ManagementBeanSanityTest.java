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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.SanityResponse;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class ManagementBeanSanityTest extends DatabaseSetup {

    @Test
    void testRequestAsSystemAdministrator() {
        prepareInvalidData();
        final SanityRequest request = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);
        final ManagementBean bean = prepareManagementBean();
        final SanityResponse response = bean.sanity(request);
        assertTrue(response.isOk());
        assertEquals(6, response.getSanities().size());
    }

    @Test
    void testRequestAsSystemAdministratorForCircle() {
        prepareInvalidData();
        final SanityRequest request = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(CIRCLE_3_ID);
        request.setSince(Utilities.newDate(10000L));
        final ManagementBean bean = prepareManagementBean();
        final SanityResponse response = bean.sanity(request);
        assertTrue(response.isOk());
        assertEquals(1, response.getSanities().size());
    }

    @Test
    void testRequestAsCircleAdministratorForCircle() {
        prepareInvalidData();
        final SanityRequest request = prepareRequest(SanityRequest.class, MEMBER_1);
        final ManagementBean bean = prepareManagementBean();
        final SanityResponse response = bean.sanity(request);
        assertTrue(response.isOk());
        assertEquals(4, response.getSanities().size());
    }

    @Test
    void testRequestAsCircleAdministratorForOtherCircle() {
        final ManagementBean bean = prepareManagementBean();
        final SanityRequest request = prepareRequest(SanityRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_3_ID);

        final SanityResponse response = bean.sanity(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Trustee information found for member '" + MEMBER_1 + "' and circle '" + CIRCLE_3_ID + "'.", response.getReturnMessage());
    }

    @Test
    void testRequestAsMember() {
        final ManagementBean bean = prepareManagementBean();
        final SanityRequest request = prepareRequest(SanityRequest.class, MEMBER_5);

        final SanityResponse response = bean.sanity(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process last Sanity Check.", response.getReturnMessage());
    }

    @Test
    void testWithFailedChecksums() {
        final ShareBean shareBean = prepareShareBean();
        final ProcessDataRequest dataRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 524288);

        final ProcessDataResponse response = shareBean.processData(dataRequest);
        assertTrue(response.isOk());
        falsifyChecksum(response, Utilities.newDate(), SanityStatus.FAILED);

        // Now to the actual test - reading the data with invalid checksum
        final FetchDataRequest readDataRequest = prepareReadRequest(response.getDataId());
        final FetchDataResponse readDataResponse = shareBean.fetchData(readDataRequest);
        assertEquals(ReturnCode.INTEGRITY_ERROR.getCode(), readDataResponse.getReturnCode());
        assertEquals("The Encrypted Data Checksum is invalid, the data appears to have been corrupted.", readDataResponse.getReturnMessage());

        // Now for the actual testing...
        final SanityRequest sanityRequest = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);
        final ManagementBean bean = prepareManagementBean();
        final SanityResponse sanityResponse = bean.sanity(sanityRequest);
        assertTrue(sanityResponse.isOk());
        assertEquals(1, sanityResponse.getSanities().size());
        assertEquals(response.getDataId(), sanityResponse.getSanities().get(0).getDataId());
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private void prepareInvalidData() {
        final ShareBean bean = prepareShareBean();
        falsifyChecksum(bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data1", MEDIUM_SIZE_BYTES)), Utilities.newDate(10L), SanityStatus.FAILED);
        falsifyChecksum(bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data2", LARGE_SIZE_BYTES)), Utilities.newDate(), SanityStatus.FAILED);
        falsifyChecksum(bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data3", MEDIUM_SIZE_BYTES)), Utilities.newDate(10L), SanityStatus.FAILED);
        falsifyChecksum(bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data4", LARGE_SIZE_BYTES)), Utilities.newDate(), SanityStatus.FAILED);
        falsifyChecksum(bean.processData(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data5", MEDIUM_SIZE_BYTES)), Utilities.newDate(10L), SanityStatus.FAILED);
        falsifyChecksum(bean.processData(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data6", LARGE_SIZE_BYTES)), Utilities.newDate(), SanityStatus.FAILED);
    }

    private static FetchDataRequest prepareReadRequest(final String dataId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);
        request.setDataId(dataId);

        return request;
    }
}
