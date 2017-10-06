/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.common.exceptions.AuthorizationException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

import java.util.UUID;

/**
 * <p>Common test class for the Process & Fetch Data Services.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class DataServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyProcessRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credentialType, Error: CredentialType is missing, null or invalid." +
                        "\nKey: credential, Error: Credential is missing, null or invalid." +
                        "\nKey: action, Error: Invalid Action provided." +
                        "\nKey: account, Error: Account is missing, null or invalid.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = new ProcessDataRequest();
        assertThat(request.getAccount(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testEmptyFetchRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credentialType, Error: CredentialType is missing, null or invalid." +
                        "\nKey: credential, Error: Credential is missing, null or invalid." +
                        "\nKey: circleId, Error: Either the CircleId or an Object Data Id must be provided." +
                        "\nKey: account, Error: Account is missing, null or invalid.");

        final FetchDataService service = new FetchDataService(settings, entityManager);
        final FetchDataRequest request = new FetchDataRequest();
        assertThat(request.getAccount(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testSavingAndReadingData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final FetchDataService dataService = new FetchDataService(settings, entityManager);

        // 1 MB large Data
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "My Data", 1048576);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));

        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setDataId(saveResponse.getId());
        final FetchDataResponse fetchResponse = dataService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testSaveAndDeleteData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 524288);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(saveResponse.getReturnMessage(), is("Ok"));

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_1, saveResponse.getId());
        final ProcessDataResponse deleteResponse = service.perform(deleteRequest);
        assertThat(deleteResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(deleteResponse.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testSaveAndDeleteDataWithoutPermission() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process Data.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "Known Data", 524288);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(saveResponse.getReturnMessage(), is("Ok"));

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_3, saveResponse.getId());
        service.perform(deleteRequest);
    }

    @Test
    public void testSaveAndDeleteDataWithoutAccess() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "More Data", 524288);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(saveResponse.getReturnMessage(), is("Ok"));

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_4, saveResponse.getId());
        final ProcessDataResponse deleteResponse = service.perform(deleteRequest);
        assertThat(deleteResponse.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(deleteResponse.getReturnMessage(), is("The requested Data Object could not be located."));
    }

    @Test
    public void testDeleteNotExistingData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareDeleteRequest(MEMBER_1, UUID.randomUUID().toString());
        final ProcessDataResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("The requested Data Object could not be located."));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private static ProcessDataRequest prepareAddRequest(final String account, final String circleId, final String dataName, final int bytes) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setName(dataName);
        request.setTypeName("data");
        request.setBytes(generateData(bytes));

        return request;
    }

    private static ProcessDataRequest prepareUpdateRequest(final String account, final String dataId) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.UPDATE);
        request.setId(dataId);

        return request;
    }

    private static ProcessDataRequest prepareDeleteRequest(final String account, final String dataId) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.DELETE);
        request.setId(dataId);

        return request;
    }
}
