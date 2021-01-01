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
package io.javadog.cws.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.SanityStatus;
import io.javadog.cws.core.exceptions.CWSException;
import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class SanityServiceTest extends DatabaseSetup {

    @Test
    void testRequestAsSystemAdministrator() {
        prepareInvalidData();
        final SanityRequest request = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);
        final SanityService service = new SanityService(settings, entityManager);
        final SanityResponse response = service.perform(request);
        assertTrue(response.isOk());
        assertEquals(6, response.getSanities().size());
    }

    @Test
    void testRequestAsSystemAdministratorForCircle() {
        prepareInvalidData();
        final SanityRequest request = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(CIRCLE_3_ID);
        request.setSince(new Date(10000L));
        final SanityService service = new SanityService(settings, entityManager);
        final SanityResponse response = service.perform(request);
        assertTrue(response.isOk());
        assertEquals(1, response.getSanities().size());
    }

    @Test
    void testRequestAsCircleAdministratorForCircle() {
        prepareInvalidData();
        final SanityRequest request = prepareRequest(SanityRequest.class, MEMBER_1);
        final SanityService service = new SanityService(settings, entityManager);
        final SanityResponse response = service.perform(request);
        assertTrue(response.isOk());
        assertEquals(4, response.getSanities().size());
    }

    @Test
    void testRequestAsCircleAdministratorForOtherCircle() {
        final SanityService service = new SanityService(settings, entityManager);
        final SanityRequest request = prepareRequest(SanityRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_3_ID);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No Trustee information found for member '" + MEMBER_1 + "' and circle '" + CIRCLE_3_ID + "'.", cause.getMessage());
    }

    @Test
    void testRequestAsMember() {
        final SanityService service = new SanityService(settings, entityManager);
        final SanityRequest request = prepareRequest(SanityRequest.class, MEMBER_5);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process last Sanity Check.", cause.getMessage());
    }

    @Test
    void testWithFailedChecksums() {
        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest dataRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 524288);

        final ProcessDataResponse response = dataService.perform(dataRequest);
        assertTrue(response.isOk());
        falsifyChecksum(response, new Date(), SanityStatus.FAILED);

        // Now to the actual test - reading the data with invalid checksum
        final FetchDataService readDataService = new FetchDataService(settings, entityManager);
        final FetchDataRequest readDataRequest = prepareReadRequest(response.getDataId());
        final FetchDataResponse readDataResponse = readDataService.perform(readDataRequest);
        assertEquals(ReturnCode.INTEGRITY_ERROR.getCode(), readDataResponse.getReturnCode());
        assertEquals("The Encrypted Data Checksum is invalid, the data appears to have been corrupted.", readDataResponse.getReturnMessage());

        // Now for the actual testing...
        final SanityRequest sanityRequest = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);
        final SanityService sanityService = new SanityService(settings, entityManager);
        final SanityResponse sanityResponse = sanityService.perform(sanityRequest);
        assertTrue(sanityResponse.isOk());
        assertEquals(1, sanityResponse.getSanities().size());
        assertEquals(response.getDataId(), sanityResponse.getSanities().get(0).getDataId());
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private void prepareInvalidData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data1", 524288)), new Date(10L), SanityStatus.FAILED);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data2", 1048576)), new Date(), SanityStatus.FAILED);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data3", 524288)), new Date(10L), SanityStatus.FAILED);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data4", 1048576)), new Date(), SanityStatus.FAILED);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data5", 524288)), new Date(10L), SanityStatus.FAILED);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data6", 1048576)), new Date(), SanityStatus.FAILED);
    }

    private static FetchDataRequest prepareReadRequest(final String dataId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);
        request.setDataId(dataId);

        return request;
    }
}
