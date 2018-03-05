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
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.dtos.Trustee;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.AuthorizationException;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.exceptions.VerificationException;
import org.junit.Test;

import java.util.UUID;

/**
 * <p>Common test class for the Process & Fetch Trustee Services.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class TrusteeServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyFetchRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credential, Error: The Credential is missing." +
                        "\nKey: accountName, Error: AccountName is missing, null or invalid.");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = new FetchTrusteeRequest();
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

        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        // Just making sure that the account is missing
        assertThat(request.getAccountName(), is(nullValue()));

        // Should throw a VerificationException, as the request is invalid.
        service.perform(request);
    }

    @Test
    public void testFetchNotExistingCircle() {
        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(UUID.randomUUID().toString());
        final FetchTrusteeResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("The requested Circle cannot be found."));
    }

    @Test
    public void testFetchCircle1WithShowTrueAsAdmin() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = service.perform(request);

        assertThat(response.isOk(), is(true));
        detailedTrusteeAssertion(response, MEMBER_1_ID, MEMBER_2_ID, MEMBER_3_ID);
    }

    @Test
    public void testFetchCircle1WithShowFalseAsAdmin() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));
        detailedTrusteeAssertion(response, MEMBER_1_ID, MEMBER_2_ID, MEMBER_3_ID);
    }

    @Test
    public void testFetchCircle1WithShowTrueAsMember1() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = service.perform(request);

        assertThat(response.isOk(), is(true));
        detailedTrusteeAssertion(response, MEMBER_1_ID, MEMBER_2_ID, MEMBER_3_ID);
    }

    @Test
    public void testFetchCircle1WithShowFalseAsMember1() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getTrustees().size(), is(3));

        // Check the member records
        final Trustee trustee1 = response.getTrustees().get(0);
        assertThat(trustee1.getCircleId(), is(CIRCLE_1_ID));
        assertThat(trustee1.getMemberId(), is(MEMBER_1_ID));
        assertThat(trustee1.getTrustLevel(), is(TrustLevel.ADMIN));
        assertThat(trustee1.getChanged().before(trustee1.getAdded()), is(false));

        final Trustee trustee2 = response.getTrustees().get(1);
        assertThat(trustee2.getCircleId(), is(CIRCLE_1_ID));
        assertThat(trustee2.getMemberId(), is(MEMBER_2_ID));
        assertThat(trustee2.getTrustLevel(), is(TrustLevel.WRITE));
        assertThat(trustee2.getChanged().before(trustee2.getAdded()), is(false));

        final Trustee trustee3 = response.getTrustees().get(2);
        assertThat(trustee3.getCircleId(), is(CIRCLE_1_ID));
        assertThat(trustee3.getMemberId(), is(MEMBER_3_ID));
        assertThat(trustee3.getTrustLevel(), is(TrustLevel.READ));
        assertThat(trustee3.getChanged().before(trustee3.getAdded()), is(false));
    }

    @Test
    public void testFetchCircle1WithShowTrueAsMember5() {
        prepareCause(CWSException.class, "No Trustee information found for member 'member5' and circle '" + CIRCLE_1_ID + "'.");

        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_5);
        assertThat(request, is(not(nullValue())));
        request.setCircleId(CIRCLE_1_ID);
        service.perform(request);
    }

    @Test
    public void testFetchCircle1WithShowFalseAsMember5() {
        prepareCause(CWSException.class, "No Trustee information found for member 'member5' and circle '" + CIRCLE_1_ID + "'.");

        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_5);
        assertThat(request, is(not(nullValue())));
        request.setCircleId(CIRCLE_1_ID);
        service.perform(request);
    }

    @Test
    public void testAddingTrusteeAsAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_4_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("The System Administrator cannot add a Member to a Circle."));
    }

    @Test
    public void testAddingTrusteeAsWritingTrustee() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process a Trustee");
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_2);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_3_ID);
        request.setTrustLevel(TrustLevel.WRITE);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAddingTrusteeAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testAddingTrusteeToInvalidCircleAsCircleAdmin() {
        final String circleId = UUID.randomUUID().toString();
        prepareCause(CWSException.class, ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member1' and circle '" + circleId + "'.");

        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAddingInvalidMemberAsTrusteeAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("No Member could be found with the given Id."));
    }

    @Test
    public void testAddingExistingTrusteeAsTrustee() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("The Member is already a trustee of the requested Circle."));
    }

    @Test
    public void testAlterTrusteeAsAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ALTER);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.ADMIN);

        final ProcessTrusteeResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("Only a Circle Administrator may alter a Trustee."));
    }

    @Test
    public void testAlterTrusteeAsWritingTrustee() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process a Trustee");
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_2);
        request.setAction(Action.ALTER);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.ADMIN);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAlterTrusteeSetAdminAsCircleAdmin() {
        final ProcessTrusteeService circleService = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest circleRequest = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        circleRequest.setAction(Action.ALTER);
        circleRequest.setCircleId(CIRCLE_1_ID);
        circleRequest.setMemberId(MEMBER_2_ID);
        circleRequest.setTrustLevel(TrustLevel.ADMIN);

        final ProcessTrusteeResponse circleResponse = circleService.perform(circleRequest);
        assertThat(circleResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(circleResponse.getReturnMessage(), is("Ok"));

        final FetchTrusteeService fetchService = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchResponse.getTrustees().get(2).getMemberId(), is(MEMBER_3_ID));
        assertThat(fetchResponse.getTrustees().get(2).getTrustLevel(), is(TrustLevel.READ));
    }

    @Test
    public void testAlterTrusteeToInvalidCircleAsCircleAdmin() {
        final String circleId = UUID.randomUUID().toString();
        prepareCause(CWSException.class, ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member1' and circle '" + circleId + "'.");

        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ALTER);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAlterInvalidMemberAsTrusteeAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ALTER);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("The requested Trustee could not be found."));
    }

    @Test
    public void testRemoveTrusteeAsAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);

        final ProcessTrusteeResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("Only a Circle Administrator may remove a Trustee."));
    }

    @Test
    public void testRemoveTrusteeAsWritingTrustee() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process a Trustee");
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_2);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testRemoveTrusteeAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);

        final ProcessTrusteeResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));

        final FetchTrusteeService fetchService = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchResponse.getTrustees().size(), is(2));
    }

    @Test
    public void testRemoveTrusteeToInvalidCircleAsCircleAdmin() {
        final String circleId = UUID.randomUUID().toString();
        prepareCause(CWSException.class, ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member1' and circle '" + circleId + "'.");

        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.REMOVE);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testRemoveInvalidMemberAsTrusteeAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());

        final ProcessTrusteeResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("The requested Trustee could not be found."));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private static void detailedTrusteeAssertion(final FetchTrusteeResponse response, final String... memberIds) {
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));

        if ((memberIds != null) && (memberIds.length > 0)) {
            assertThat(response.getTrustees().size(), is(memberIds.length));
            for (int i = 0; i < memberIds.length; i++) {
                assertThat(response.getTrustees().get(i).getMemberId(), is(memberIds[i]));
            }
        } else {
            assertThat(response.getTrustees().isEmpty(), is(true));
        }
    }
}
