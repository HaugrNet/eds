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
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import org.junit.Test;

import java.util.UUID;

/**
 * <p>Common test class for the Process & Fetch Trustee Services.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class TrusteeServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyFetchRequest() {
        prepareCause(ReturnCode.VERIFICATION_WARNING, "Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = new FetchTrusteeRequest();
        // Just making sure that the account is missing
        assertThat(request.getAccountName(), is(nullValue()));

        // Should throw a VerificationException, as the request is invalid.
        service.perform(request);
    }

    @Test
    public void testEmptyProcessRequest() {
        prepareCause(ReturnCode.VERIFICATION_WARNING, "Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing." +
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
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "The requested Circle cannot be found.");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(UUID.randomUUID().toString());
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
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
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member5' and circle '" + CIRCLE_1_ID + "'.");

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
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member5' and circle '" + CIRCLE_1_ID + "'.");

        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_5);
        assertThat(request, is(not(nullValue())));
        request.setCircleId(CIRCLE_1_ID);

        service.perform(request);
    }

    @Test
    public void testCreatingAndAddingTrusteeAsSystemAdmin() {
        // Step 1, create a new Circle as System Administrator
        final ProcessCircleService circleService = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest circleRequest = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        circleRequest.setAction(Action.CREATE);
        circleRequest.setCircleName("Admin Circle");
        final ProcessCircleResponse circleResponse = circleService.perform(circleRequest);
        assertThat(circleResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(circleResponse.getReturnMessage(), is("Ok"));
        assertThat(circleResponse.getCircleId(), is(not(nullValue())));
        final String circleId = circleResponse.getCircleId();

        // Step 2, add a new trustee to the newly created circle
        final ProcessTrusteeService trusteeService = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest trusteeRequest = prepareRequest(ProcessTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        trusteeRequest.setAction(Action.ADD);
        trusteeRequest.setCircleId(circleId);
        trusteeRequest.setMemberId(MEMBER_2_ID);
        trusteeRequest.setTrustLevel(TrustLevel.WRITE);
        final ProcessTrusteeResponse trusteeResponse = trusteeService.perform(trusteeRequest);
        assertThat(trusteeResponse.getReturnMessage(), is("Ok"));
        assertThat(trusteeResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        // Step 3, verify that the Circle has 2 members
        final FetchTrusteeService fetchService = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest.setCircleId(circleId);
        final FetchTrusteeResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchResponse.getReturnMessage(), is("Ok"));
        assertThat(fetchResponse.getTrustees(), is(not(nullValue())));
        assertThat(fetchResponse.getTrustees().size(), is(2));
        assertThat(fetchResponse.getTrustees().get(0).getMemberId(), is(ADMIN_ID));
        assertThat(fetchResponse.getTrustees().get(0).getTrustLevel(), is(TrustLevel.ADMIN));
        assertThat(fetchResponse.getTrustees().get(1).getMemberId(), is(MEMBER_2_ID));
        assertThat(fetchResponse.getTrustees().get(1).getTrustLevel(), is(TrustLevel.WRITE));
    }

    @Test
    public void testAddingTrusteeAsWritingTrustee() {
        prepareCause(ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process a Trustee");

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
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member1' and circle '" + circleId + "'.");

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
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "No Member could be found with the given Id.");

        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAddingExistingTrusteeAsTrustee() {
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "The Member is already a trustee of the requested Circle.");

        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.WRITE);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    /**
     * This test is testing a border case scenario, where a System Administrator
     * is attempting to perform an illegal action on a Circle of Trust. The
     * System Administrator is a member of the Circle, and is thus not allowed
     * to perform the given action. Yet, as System Administrator, the path for
     * verification of permissions is traversing a slightly different one than
     * standard members. Hence, the rejection with the strange error, hinting
     * that the Administrator is not a member if the Circle.
     */
    @Test
    public void testAddingNewTrusteeAsCircleMemberAndSystemAdministrator() {
        prepareCause(ReturnCode.ILLEGAL_ACTION, "It is not possible to add a member to a circle, without membership.");

        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(ADMIN_ID);
        request.setTrustLevel(TrustLevel.READ);

        final ProcessTrusteeResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));

        final FetchTrusteeService fetchService = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchResponse.getReturnMessage(), is("Ok"));
        assertThat(fetchResponse.getTrustees(), is(not(nullValue())));
        assertThat(fetchResponse.getTrustees().size(), is(4));
        assertThat(fetchResponse.getTrustees().get(0).getMemberId(), is(ADMIN_ID));

        final ProcessTrusteeRequest adminRequest = prepareRequest(ProcessTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        adminRequest.setAction(Action.ADD);
        adminRequest.setCircleId(CIRCLE_1_ID);
        adminRequest.setMemberId(MEMBER_5_ID);
        adminRequest.setTrustLevel(TrustLevel.READ);
        service.perform(adminRequest);
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
        prepareCause(ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process a Trustee");
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
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member1' and circle '" + circleId + "'.");

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
        prepareCause(ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process a Trustee");

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
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member 'member1' and circle '" + circleId + "'.");

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
