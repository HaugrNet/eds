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
import io.javadog.cws.common.exceptions.AuthorizationException;
import io.javadog.cws.model.DatabaseSetup;
import io.javadog.cws.model.entities.DataEntity;
import org.junit.Test;

import javax.persistence.Query;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SanityServiceTest extends DatabaseSetup {

    @Test
    public void testNormalCase() {
        final SanityRequest request = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);
        final SanityService service = new SanityService(settings, entityManager);
        final SanityResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));
        assertThat(response.getSanities().isEmpty(), is(true));
    }

    @Test
    public void testRequestAsMember() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "Cannot complete this request, as it is only allowed for the System Administrator.");

        final SanityService service = new SanityService(settings, entityManager);
        final SanityRequest request = prepareRequest(SanityRequest.class, MEMBER_1);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testWithFailedChecksums() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 524288);

        final ProcessDataResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));

        // Now to the tricky part. We wish to test that the checksum is invalid,
        // and thus resulting in a correct error message. As the checksum is
        // controlled internally by CWS, it cannot be altered (rightfully) via
        // the API, hence we have to modify it directly in the database!
        final Query query = entityManager.createQuery("select d from DataEntity d where d.metadata.externalId = :eid");
        query.setParameter("eid", response.getDataId());
        final DataEntity entity = (DataEntity) query.getSingleResult();
        entity.setChecksum(UUID.randomUUID().toString());
        entityManager.persist(entity);

        // Now to the actual test - reading the data with invalid checksum
        final FetchDataService readService = new FetchDataService(settings, entityManager);
        final FetchDataRequest readRequest = prepareReadRequest(MEMBER_1, CIRCLE_1_ID, response.getDataId());
        final FetchDataResponse readResponse = readService.perform(readRequest);
        assertThat(readResponse.getReturnCode(), is(ReturnCode.INTEGRITY_ERROR));
        assertThat(readResponse.getReturnMessage(), is("The Encrypted Data Checksum is invalid, the data appears to have been corrupted."));

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
