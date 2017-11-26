/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
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
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.AuthorizationException;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.model.DatabaseSetup;
import io.javadog.cws.model.entities.CircleEntity;
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

        final ProcessDataService processDataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addRequest = prepareRequest(ProcessDataRequest.class, MEMBER_5);
        addRequest.setAction(Action.ADD);
        addRequest.setCircleId(createResponse.getCircleId());
        addRequest.setDataName("My Data Object");
        addRequest.setData(generateData(512));
        final ProcessDataResponse processDataResponse = processDataService.perform(addRequest);
        assertThat(processDataResponse.isOk(), is(true));

        final FetchDataService dataService = new FetchDataService(settings, entityManager);
        final FetchDataRequest dataRequest = prepareRequest(FetchDataRequest.class, MEMBER_5);
        dataRequest.setCircleId(createResponse.getCircleId());
        final FetchDataResponse dataResponse = dataService.perform(dataRequest);
        assertThat(dataResponse.isOk(), is(true));
        assertThat(dataResponse.getMetadata().size(), is(1));
        assertThat(dataResponse.getMetadata().get(0).getDataName(), is("My Data Object"));
    }

    @Test
    public void testFetchNotExistingCircle() {
        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(UUID.randomUUID().toString());
        final FetchCircleResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("The requested Circle cannot be found."));
    }

    @Test
    public void testFetchAllCirclesAsAdmin() {
        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(3));
        assertThat(response.getCircles().get(0).getCircleName(), is(CIRCLE_1));
        assertThat(response.getCircles().get(1).getCircleName(), is(CIRCLE_2));
        assertThat(response.getCircles().get(2).getCircleName(), is(CIRCLE_3));
        assertThat(response.getTrustees().isEmpty(), is(true));
    }

    @Test
    public void testFetchAllCirclesAsMember1() {
        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);

        final FetchCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(3));
        assertThat(response.getCircles().get(0).getCircleName(), is(CIRCLE_1));
        assertThat(response.getCircles().get(1).getCircleName(), is(CIRCLE_2));
        assertThat(response.getCircles().get(2).getCircleName(), is(CIRCLE_3));
        assertThat(response.getTrustees().size(), is(0));
    }

    @Test
    public void testFetchCircle1WithShowTrueAsAdmin() {
        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "true");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final CircleEntity circle = findFirstCircle();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getCircleName(), is(CIRCLE_1));
        assertThat(response.getTrustees().size(), is(3));
        assertThat(response.getTrustees().get(0).getMemberId(), is(MEMBER_1_ID));
        assertThat(response.getTrustees().get(1).getMemberId(), is(MEMBER_2_ID));
        assertThat(response.getTrustees().get(2).getMemberId(), is(MEMBER_3_ID));
    }

    @Test
    public void testFetchCircle1WithShowFalseAsAdmin() {
        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "false");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final CircleEntity circle = findFirstCircle();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getCircleName(), is(CIRCLE_1));
        assertThat(response.getTrustees().size(), is(3));
        assertThat(response.getTrustees().get(0).getMemberId(), is(MEMBER_1_ID));
        assertThat(response.getTrustees().get(1).getMemberId(), is(MEMBER_2_ID));
        assertThat(response.getTrustees().get(2).getMemberId(), is(MEMBER_3_ID));
    }

    @Test
    public void testFetchCircle1WithShowTrueAsMember1() {
        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "true");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final CircleEntity circle = findFirstCircle();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getCircleName(), is(CIRCLE_1));
        assertThat(response.getTrustees().size(), is(3));
        assertThat(response.getTrustees().get(0).getMemberId(), is(MEMBER_1_ID));
        assertThat(response.getTrustees().get(1).getMemberId(), is(MEMBER_2_ID));
        assertThat(response.getTrustees().get(2).getMemberId(), is(MEMBER_3_ID));
    }

    @Test
    public void testFetchCircle1WithShowFalseAsMember1() {
        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "false");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final CircleEntity circle = findFirstCircle();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getCircleName(), is(CIRCLE_1));
        assertThat(response.getTrustees().size(), is(3));
        assertThat(response.getTrustees().get(0).getMemberId(), is(MEMBER_1_ID));
        assertThat(response.getTrustees().get(1).getMemberId(), is(MEMBER_2_ID));
        assertThat(response.getTrustees().get(2).getMemberId(), is(MEMBER_3_ID));
    }

    @Test
    public void testFetchCircle1WithShowTrueAsMember5() {
        final CircleEntity circle = findFirstCircle();
        prepareCause(CWSException.class, "No Trustee information found for member 'member5' and circle '" + circle.getExternalId() + "'.");

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "true");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        assertThat(request, is(not(nullValue())));
        request.setCircleId(circle.getExternalId());
        service.perform(request);
    }

    @Test
    public void testFetchCircle1WithShowFalseAsMember5() {
        final CircleEntity circle = findFirstCircle();
        prepareCause(CWSException.class, "No Trustee information found for member 'member5' and circle '" + circle.getExternalId() + "'.");

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "false");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        assertThat(request, is(not(nullValue())));
        request.setCircleId(circle.getExternalId());
        service.perform(request);
    }

    @Test
    public void testCreateCircle() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setMemberId(MEMBER_1_ID);
        request.setCircleName("My Circle");

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircleId(), is(not(nullValue())));

        final FetchCircleService fetchService = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        fetchRequest.setCircleId(response.getCircleId());
        final FetchCircleResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(fetchResponse.getCircles().size(), is(1));
        assertThat(fetchResponse.getCircles().get(0).getCircleName(), is("My Circle"));
    }

    @Test
    public void testCreateCircleAsMember() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.CREATE);
        request.setCircleName("My Circle");
        request.setMemberId(MEMBER_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING));
        assertThat(response.getReturnMessage(), is("Only the System Administrator may create a new Circle."));
    }

    @Test
    public void testCreateCircleWithInvalidCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setMemberId(UUID.randomUUID().toString());
        request.setCircleName("My Circle");

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
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
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
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
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
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
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));

        final FetchCircleService fetchService = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchCircleResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(fetchResponse.getCircles().size(), is(1));
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
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testUpdateExistingCircleAsCircleMember() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process a Circle.");
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_2);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(CIRCLE_1_ID);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
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
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
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
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
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
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("A Circle with the requested name already exists."));
    }

    @Test
    public void testDeleteCircleAsAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testDeleteCircleAsMember() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.DELETE);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING));
        assertThat(response.getReturnMessage(), is("Only the System Administrator may delete a Circle."));
    }

    @Test
    public void testDeleteNotExistingCircle() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setCircleId(UUID.randomUUID().toString());

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("No Circle could be found with the given Id."));
    }

    @Test
    public void testAddingTrusteeAsAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_4_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING));
        assertThat(response.getReturnMessage(), is("The System Administrator cannot add a Member to a Circle."));
    }

    @Test
    public void testAddingTrusteeAsWritingTrustee() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process a Circle.");

        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_2);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_3_ID);
        request.setTrustLevel(TrustLevel.WRITE);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAddingTrusteeAsCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testAddingTrusteeToInvalidCircleAsCircleAdmin() {
        final String circleId = UUID.randomUUID().toString();
        prepareCause(CWSException.class, ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member1' and circle '" + circleId + "'.");

        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAddingInvalidMemberAsTrusteeAsCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("No Member could be found with the given Id."));
    }

    @Test
    public void testAddingExistingTrusteeAsTrustee() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("The Member is already a trustee of the requested Circle."));
    }

    @Test
    public void testAlterTrusteeAsAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ALTER);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.ADMIN);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING));
        assertThat(response.getReturnMessage(), is("Only a Circle Administrator may alter a Trustee."));
    }

    @Test
    public void testAlterTrusteeAsWritingTrustee() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process a Circle.");

        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_2);
        request.setAction(Action.ALTER);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.ADMIN);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAlterTrusteeSetAdminAsCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.ALTER);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.ADMIN);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));

        final FetchCircleService fetchService = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchCircleResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(fetchResponse.getTrustees().get(2).getMemberId(), is(MEMBER_3_ID));
        assertThat(fetchResponse.getTrustees().get(2).getTrustLevel(), is(TrustLevel.READ));
    }

    @Test
    public void testAlterTrusteeToInvalidCircleAsCircleAdmin() {
        final String circleId = UUID.randomUUID().toString();
        prepareCause(CWSException.class, ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member1' and circle '" + circleId + "'.");

        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.ALTER);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAlterInvalidMemberAsTrusteeAsCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.ALTER);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("The requested Trustee could not be found."));
    }

    @Test
    public void testRemoveTrusteeAsAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING));
        assertThat(response.getReturnMessage(), is("Only a Circle Administrator may remove a Trustee."));
    }

    @Test
    public void testRemoveTrusteeAsWritingTrustee() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process a Circle.");

        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_2);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testRemoveTrusteeAsCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));

        final FetchCircleService fetchService = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchCircleResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(fetchResponse.getTrustees().size(), is(2));
    }

    @Test
    public void testRemoveTrusteeToInvalidCircleAsCircleAdmin() {
        final String circleId = UUID.randomUUID().toString();
        prepareCause(CWSException.class, ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member1' and circle '" + circleId + "'.");

        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.REMOVE);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testRemoveInvalidMemberAsTrusteeAsCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());

        final ProcessCircleResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("The requested Trustee could not be found."));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private CircleEntity findFirstCircle() {
        return entityManager.find(CircleEntity.class, 1L);
    }
}
