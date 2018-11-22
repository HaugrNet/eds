/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.exceptions.VerificationException;
import io.javadog.cws.core.model.Settings;
import org.junit.Test;

import java.util.UUID;

/**
 * <p>Common test class for the Process & Fetch Circle Services.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CircleServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyFetchRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credential, Error: The Credential is missing." +
                        "\nKey: accountName, Error: AccountName is missing, null or invalid.");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = new FetchCircleRequest();
        // Just making sure that the account is missing
        assertThat(request.getAccountName(), is(nullValue()));

        // Should throw a VerificationException, as the request is invalid.
        service.perform(request);
    }

    @Test
    public void testEmptyProcessRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credential, Error: The Credential is missing." +
                        "\nKey: accountName, Error: AccountName is missing, null or invalid." +
                        "\nKey: action, Error: No action has been provided.");

        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = new ProcessCircleRequest();
        // Just making sure that the account is missing
        assertThat(request.getAccountName(), is(nullValue()));

        // Should throw a VerificationException, as the request is invalid.
        service.perform(request);
    }

    @Test
    public void testCreateAndReadCircle() {
        final ProcessCircleService processService = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest createRequest = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        createRequest.setAction(Action.CREATE);
        createRequest.setMemberId(MEMBER_5_ID);
        createRequest.setCircleName("One");

        final ProcessCircleResponse createResponse = processService.perform(createRequest);
        assertThat(createResponse.isOk(), is(true));

        final byte[] bytes = generateData(512);
        final String data = crypto.bytesToString(bytes);
        final ProcessDataService processDataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addRequest = prepareRequest(ProcessDataRequest.class, MEMBER_5);
        addRequest.setAction(Action.ADD);
        addRequest.setCircleId(createResponse.getCircleId());
        addRequest.setDataName("My Data Object");
        addRequest.setData(bytes);
        final ProcessDataResponse processDataResponse = processDataService.perform(addRequest);
        assertThat(processDataResponse.isOk(), is(true));

        // Read root folder for the Circle
        final FetchDataService dataService = new FetchDataService(settings, entityManager);
        final FetchDataRequest dataRootRequest = prepareRequest(FetchDataRequest.class, MEMBER_5);
        dataRootRequest.setCircleId(createResponse.getCircleId());
        final FetchDataResponse dataRootResponse = dataService.perform(dataRootRequest);
        assertThat(dataRootResponse.isOk(), is(true));
        assertThat(dataRootResponse.getMetadata().size(), is(1));
        assertThat(dataRootResponse.getMetadata().get(0).getDataName(), is("My Data Object"));

        // Read the newly created Data Object
        final FetchDataRequest dataFileRequest = prepareRequest(FetchDataRequest.class, MEMBER_5);
        dataFileRequest.setDataId(dataRootResponse.getMetadata().get(0).getDataId());
        final FetchDataResponse dataFileResponse = dataService.perform(dataFileRequest);
        assertThat(dataFileResponse.isOk(), is(true));
        assertThat(dataFileResponse.getMetadata().size(), is(1));
        assertThat(dataFileResponse.getMetadata().get(0).getDataName(), is("My Data Object"));
        assertThat(crypto.bytesToString(dataFileResponse.getData()), is(data));
    }

    @Test
    public void testFetchAllCirclesAsAdminWithShowCirclesTrue() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "true");
        final FetchCircleService service = new FetchCircleService(mySettings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchCircleResponse response = service.perform(request);

        assertThat(response.isOk(), is(true));
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2, CIRCLE_3);
    }

    @Test
    public void testFetchAllCirclesAsAdminWithShowCirclesFalse() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "false");
        final FetchCircleService service = new FetchCircleService(mySettings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchCircleResponse response = service.perform(request);

        assertThat(response.isOk(), is(true));
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2, CIRCLE_3);
    }

    @Test
    public void testFetchAllCirclesAsMember1WithShowCirclesTrue() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "true");
        final FetchCircleService service = new FetchCircleService(mySettings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        final FetchCircleResponse response = service.perform(request);

        assertThat(response.isOk(), is(true));
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2, CIRCLE_3);
    }

    @Test
    public void testFetchAllCirclesAsMember1WithShowCirclesFalse() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "false");
        final FetchCircleService service = new FetchCircleService(mySettings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        final FetchCircleResponse response = service.perform(request);

        assertThat(response.isOk(), is(true));
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2);
    }

    @Test
    public void testFetchAllCirclesAsMember5WithShowCirclesFalse() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "false");
        final FetchCircleService service = new FetchCircleService(mySettings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        final FetchCircleResponse response = service.perform(request);

        assertThat(response.isOk(), is(true));
        detailedCircleAssertion(response, CIRCLE_3);
    }

    @Test
    public void testCreateCircle() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setMemberId(MEMBER_1_ID);
        request.setCircleName("a circle");

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircleId(), is(not(nullValue())));

        final FetchCircleService fetchService = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        final FetchCircleResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchResponse.getCircles().size(), is(4));
        // Circles are sorted by name, so our newly created Circle will be the first
        assertThat(fetchResponse.getCircles().get(0).getCircleId(), is(response.getCircleId()));
        assertThat(fetchResponse.getCircles().get(0).getCircleName(), is("a circle"));
    }

    @Test
    public void testCreateCircleAsMember() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.CREATE);
        request.setCircleName("My Circle");
        request.setMemberId(MEMBER_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testCreateCircleWithExternalCircleKey() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest createRequest = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        createRequest.setAction(Action.CREATE);
        createRequest.setCircleName("Extra Encrypted");
        createRequest.setCircleKey(UUID.randomUUID().toString());

        final ProcessCircleResponse createResponse = service.perform(createRequest);
        assertThat(createResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(createResponse.getCircleId(), is(not(nullValue())));

        final ProcessCircleRequest updateRequest = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        updateRequest.setCredential(crypto.stringToBytes(MEMBER_5));
        updateRequest.setAction(Action.UPDATE);
        updateRequest.setCircleId(createResponse.getCircleId());
        updateRequest.setCircleKey(UUID.randomUUID().toString());
        final ProcessCircleResponse updateResponse = service.perform(updateRequest);
        assertThat(updateResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        final FetchCircleService fetchService = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        final FetchCircleResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchResponse.getCircles().size(), is(4));
        // Sorting alphabetically on lowercase names should reveal a correct
        // sorting where the list is as follows:
        //  * circle1
        //  * circle2
        //  * circle3
        //  * Extra Encrypted
        // Note, that it could be considered a bug that the list earlier was
        // sorted with uppercase letters before lowercase letters, thus 'Z' came
        // before 'a'. With the case insensitive indexes, it should be fixed.
        assertThat(fetchResponse.getCircles().get(3).getCircleId(), is(createResponse.getCircleId()));
        assertThat(fetchResponse.getCircles().get(3).getCircleKey(), is(updateRequest.getCircleKey()));
    }

    @Test
    public void testCreateCircleWithInvalidCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setMemberId(UUID.randomUUID().toString());
        request.setCircleName("My Circle");

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("Cannot create a new Circle with a non-existing Circle Administrator."));
    }

    @Test
    public void testCreateCircleWithSystemAdminAsCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setMemberId(ADMIN_ID);
        request.setCircleName("My Circle");

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("It is not allowed for the System Administrator to be part of a Circle."));
    }

    @Test
    public void testCreateCircleWithExistingName() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName(CIRCLE_1);
        request.setMemberId(MEMBER_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("A Circle with the requested name already exists."));
    }

    @Test
    public void testUpdateExistingCircleAsAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));

        final FetchCircleService fetchService = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchCircleResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchResponse.getCircles().size(), is(3));
        assertThat(fetchResponse.getCircles().get(0).getCircleId(), is(CIRCLE_1_ID));
        assertThat(fetchResponse.getCircles().get(0).getCircleName(), is("Circle One"));
    }

    @Test
    public void testUpdateExistingCircleAsCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testUpdateExistingCircleAsCircleMember() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_2);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(CIRCLE_1_ID);
        assertThat(request.validate().isEmpty(), is(true));

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("Only a Circle Administrator may perform this action."));
    }

    @Test
    public void testUpdateCircleAsNonMember() {
        prepareCause(CWSException.class, ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member5' and circle '" + CIRCLE_1_ID + "'.");
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(CIRCLE_1_ID);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testUpdateNonExistingCircle() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(UUID.randomUUID().toString());

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("No Circle could be found with the given Id."));
    }

    @Test
    public void testUpdateExistingCircleWithExistingName() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCircleName(CIRCLE_2);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("A Circle with the requested name already exists."));
    }

    @Test
    public void testUpdateExistingCircleWithOwnName() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCircleName(CIRCLE_1);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testDeleteCircleAsAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testDeleteCircleAsMember() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.DELETE);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("Only the System Administrator may delete a Circle."));
    }

    @Test
    public void testDeleteNotExistingCircle() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setCircleId(UUID.randomUUID().toString());

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("No Circle could be found with the given Id."));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private static void detailedCircleAssertion(final FetchCircleResponse response, final String... circleNames) {
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));

        if ((circleNames != null) && (circleNames.length > 0)) {
            assertThat(response.getCircles().size(), is(circleNames.length));
            for (int i = 0; i < circleNames.length; i++) {
                assertThat(response.getCircles().get(i).getCircleName(), is(circleNames[i]));
            }
        } else {
            assertThat(response.getCircles().isEmpty(), is(true));
        }
    }
}
