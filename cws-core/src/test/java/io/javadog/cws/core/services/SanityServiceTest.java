/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.exceptions.AuthorizationException;
import io.javadog.cws.core.exceptions.CWSException;
import org.junit.Test;

import java.util.Date;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SanityServiceTest extends DatabaseSetup {

    @Test
    public void testRequestAsSystemAdministrator() {
        prepareInvalidData();
        final SanityRequest request = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);
        final SanityService service = new SanityService(settings, entityManager);
        final SanityResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));
        assertThat(response.getSanities().size(), is(6));
    }

    @Test
    public void testRequestAsSystemAdministratorForCircle() {
        prepareInvalidData();
        final SanityRequest request = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(CIRCLE_3_ID);
        request.setSince(new Date(10000L));
        final SanityService service = new SanityService(settings, entityManager);
        final SanityResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));
        assertThat(response.getSanities().size(), is(1));
    }

    @Test
    public void testRequestAsCircleAdministratorForCircle() {
        prepareInvalidData();
        final SanityRequest request = prepareRequest(SanityRequest.class, MEMBER_1);
        final SanityService service = new SanityService(settings, entityManager);
        final SanityResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));
        assertThat(response.getSanities().size(), is(4));
    }

    @Test
    public void testRequestAsCircleAdministratorForOtherCircle() {
        prepareCause(CWSException.class, ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member '" + MEMBER_1 + "' and circle '" + CIRCLE_3_ID + "'.");
        final SanityRequest request = prepareRequest(SanityRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_3_ID);
        final SanityService service = new SanityService(settings, entityManager);
        final SanityResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));
        assertThat(response.getSanities().isEmpty(), is(true));
    }

    @Test
    public void testRequestAsMember() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process last Sanity Check.");

        final SanityService service = new SanityService(settings, entityManager);
        final SanityRequest request = prepareRequest(SanityRequest.class, MEMBER_5);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testWithFailedChecksums() {
        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest dataRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 524288);

        final ProcessDataResponse response = dataService.perform(dataRequest);
        assertThat(response.isOk(), is(true));
        falsifyChecksum(response, new Date());

        // Now to the actual test - reading the data with invalid checksum
        final FetchDataService readDataService = new FetchDataService(settings, entityManager);
        final FetchDataRequest readDattaRequest = prepareReadRequest(MEMBER_1, CIRCLE_1_ID, response.getDataId());
        final FetchDataResponse readDataResponse = readDataService.perform(readDattaRequest);
        assertThat(readDataResponse.getReturnCode(), is(ReturnCode.INTEGRITY_ERROR));
        assertThat(readDataResponse.getReturnMessage(), is("The Encrypted Data Checksum is invalid, the data appears to have been corrupted."));

        // Now for the actual testing...
        final SanityRequest sanityRequest = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);
        final SanityService sanityService = new SanityService(settings, entityManager);
        final SanityResponse sanityResponse = sanityService.perform(sanityRequest);
        assertThat(sanityResponse.isOk(), is(true));
        assertThat(sanityResponse.getSanities().size(), is(1));
        assertThat(sanityResponse.getSanities().get(0).getDataId(), is(response.getDataId()));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private void prepareInvalidData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "Data1", 524288)), new Date(10L));
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "Data2", 1048576)), new Date());
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_1, CIRCLE_2_ID, "Data3", 524288)), new Date(10L));
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_1, CIRCLE_2_ID, "Data4", 1048576)), new Date());
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_4, CIRCLE_3_ID, "Data5", 524288)), new Date(10L));
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_4, CIRCLE_3_ID, "Data6", 1048576)), new Date());
    }

    private static ProcessDataRequest prepareAddRequest(final String account, final String circleId, final String dataName, final int bytes) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setDataName(dataName);
        request.setTypeName(Constants.DATA_TYPENAME);
        request.setData(generateData(bytes));

        return request;
    }

    private static FetchDataRequest prepareReadRequest(final String account, final String circleId, final String dataId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, account);
        request.setCircleId(circleId);
        request.setDataId(dataId);

        return request;
    }
}
