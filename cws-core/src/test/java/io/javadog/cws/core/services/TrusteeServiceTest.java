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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.dtos.Trustee;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * <p>Common test class for the Process & Fetch Trustee Services.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class TrusteeServiceTest extends DatabaseSetup {

    @Test
    void testEmptyFetchRequest() {
        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = new FetchTrusteeRequest();

        // Should throw a VerificationException, as the request is invalid.
        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing." +
                "\nKey: circleId, Error: The Circle Id is missing or invalid.", cause.getMessage());
    }

    @Test
    void testEmptyProcessRequest() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        // Just making sure that the account is missing
        assertNull(request.getAccountName());

        // Should throw a VerificationException, as the request is invalid.
        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing." +
                "\nKey: action, Error: No action has been provided.", cause.getMessage());
    }

    @Test
    void testFetchNotExistingCircle() {
        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(UUID.randomUUID().toString());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The requested Circle cannot be found.", cause.getMessage());
    }

    @Test
    void testFetchCircle1WithShowTrueAsAdmin() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = service.perform(request);

        assertTrue(response.isOk());
        detailedTrusteeAssertion(response, MEMBER_1_ID, MEMBER_2_ID, MEMBER_3_ID);
    }

    @Test
    void testFetchCircle1WithShowFalseAsAdmin() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = service.perform(request);
        assertTrue(response.isOk());
        detailedTrusteeAssertion(response, MEMBER_1_ID, MEMBER_2_ID, MEMBER_3_ID);
    }

    @Test
    void testFetchCircle1WithShowTrueAsMember1() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = service.perform(request);

        assertTrue(response.isOk());
        detailedTrusteeAssertion(response, MEMBER_1_ID, MEMBER_2_ID, MEMBER_3_ID);
    }

    @Test
    void testFetchCircle1WithShowFalseAsMember1() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = service.perform(request);

        assertNotNull(response);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(3, response.getTrustees().size());

        // Check the member records
        final Trustee trustee1 = response.getTrustees().get(0);
        assertEquals(CIRCLE_1_ID, trustee1.getCircleId());
        assertEquals(MEMBER_1_ID, trustee1.getMemberId());
        assertEquals(TrustLevel.ADMIN, trustee1.getTrustLevel());
        assertFalse(trustee1.getChanged().before(trustee1.getAdded()));

        final Trustee trustee2 = response.getTrustees().get(1);
        assertEquals(CIRCLE_1_ID, trustee2.getCircleId());
        assertEquals(MEMBER_2_ID, trustee2.getMemberId());
        assertEquals(TrustLevel.WRITE, trustee2.getTrustLevel());
        assertFalse(trustee2.getChanged().before(trustee2.getAdded()));

        final Trustee trustee3 = response.getTrustees().get(2);
        assertEquals(CIRCLE_1_ID, trustee3.getCircleId());
        assertEquals(MEMBER_3_ID, trustee3.getMemberId());
        assertEquals(TrustLevel.READ, trustee3.getTrustLevel());
        assertFalse(trustee3.getChanged().before(trustee3.getAdded()));
    }

    @Test
    void testFetchCircle1WithShowTrueAsMember5() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_5);
        assertNotNull(request);
        request.setCircleId(CIRCLE_1_ID);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No Trustee information found for member 'member5' and circle '" + CIRCLE_1_ID + "'.", cause.getMessage());
    }

    @Test
    void testFetchCircle1WithShowFalseAsMember5() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final FetchTrusteeService service = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_5);
        request.setCircleId(CIRCLE_1_ID);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No Trustee information found for member 'member5' and circle '" + CIRCLE_1_ID + "'.", cause.getMessage());
    }

    @Test
    void testCreatingAndAddingTrusteeAsSystemAdmin() {
        // Step 1, create a new Circle as System Administrator
        final ProcessCircleService circleService = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest circleRequest = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        circleRequest.setAction(Action.CREATE);
        circleRequest.setCircleName("Admin Circle");
        final ProcessCircleResponse circleResponse = circleService.perform(circleRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), circleResponse.getReturnCode());
        assertEquals("Ok", circleResponse.getReturnMessage());
        assertNotNull(circleResponse.getCircleId());
        final String circleId = circleResponse.getCircleId();

        // Step 2, add a new trustee to the newly created circle
        final ProcessTrusteeService trusteeService = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest trusteeRequest = prepareRequest(ProcessTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        trusteeRequest.setAction(Action.ADD);
        trusteeRequest.setCircleId(circleId);
        trusteeRequest.setMemberId(MEMBER_2_ID);
        trusteeRequest.setTrustLevel(TrustLevel.WRITE);
        final ProcessTrusteeResponse trusteeResponse = trusteeService.perform(trusteeRequest);
        assertEquals("Ok", trusteeResponse.getReturnMessage());
        assertEquals(ReturnCode.SUCCESS.getCode(), trusteeResponse.getReturnCode());

        // Step 3, verify that the Circle has 2 members
        final FetchTrusteeService fetchService = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest.setCircleId(circleId);
        final FetchTrusteeResponse fetchResponse = fetchService.perform(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals("Ok", fetchResponse.getReturnMessage());
        assertNotNull(fetchResponse.getTrustees());
        assertEquals(2, fetchResponse.getTrustees().size());
        assertEquals(ADMIN_ID, fetchResponse.getTrustees().get(0).getMemberId());
        assertEquals(TrustLevel.ADMIN, fetchResponse.getTrustees().get(0).getTrustLevel());
        assertEquals(MEMBER_2_ID, fetchResponse.getTrustees().get(1).getMemberId());
        assertEquals(TrustLevel.WRITE, fetchResponse.getTrustees().get(1).getTrustLevel());
    }

    @Test
    void testAddingTrusteeAsWritingTrustee() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_2);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_3_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process a Trustee", cause.getMessage());
    }

    @Test
    void testAddingTrusteeAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testAddingTrusteeToInvalidCircleAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        final String circleId = UUID.randomUUID().toString();
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No Trustee information found for member 'member1' and circle '" + circleId + "'.", cause.getMessage());
    }

    @Test
    void testAddingInvalidMemberAsTrusteeAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No Member could be found with the given Id.", cause.getMessage());
    }

    @Test
    void testAddingExistingTrusteeAsTrustee() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The Member is already a trustee of the requested Circle.", cause.getMessage());
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
    void testAddingNewTrusteeAsCircleMemberAndSystemAdministrator() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(ADMIN_ID);
        request.setTrustLevel(TrustLevel.READ);

        final ProcessTrusteeResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());

        final FetchTrusteeService fetchService = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse fetchResponse = fetchService.perform(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals("Ok", fetchResponse.getReturnMessage());
        assertNotNull(fetchResponse.getTrustees());
        assertEquals(4, fetchResponse.getTrustees().size());
        assertEquals(ADMIN_ID, fetchResponse.getTrustees().get(0).getMemberId());

        final ProcessTrusteeRequest adminRequest = prepareRequest(ProcessTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        adminRequest.setAction(Action.ADD);
        adminRequest.setCircleId(CIRCLE_1_ID);
        adminRequest.setMemberId(MEMBER_5_ID);
        adminRequest.setTrustLevel(TrustLevel.READ);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(adminRequest));
        assertEquals(ReturnCode.ILLEGAL_ACTION, cause.getReturnCode());
        assertEquals("It is not possible to add a member to a circle, without membership.", cause.getMessage());
    }

    @Test
    void testAlterTrusteeAsWritingTrustee() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_2);
        request.setAction(Action.ALTER);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.ADMIN);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process a Trustee", cause.getMessage());
    }

    @Test
    void testAlterTrusteeSetAdminAsCircleAdmin() {
        final ProcessTrusteeService circleService = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest circleRequest = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        circleRequest.setAction(Action.ALTER);
        circleRequest.setCircleId(CIRCLE_1_ID);
        circleRequest.setMemberId(MEMBER_2_ID);
        circleRequest.setTrustLevel(TrustLevel.ADMIN);

        final ProcessTrusteeResponse circleResponse = circleService.perform(circleRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), circleResponse.getReturnCode());
        assertEquals("Ok", circleResponse.getReturnMessage());

        final FetchTrusteeService fetchService = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse fetchResponse = fetchService.perform(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(MEMBER_3_ID, fetchResponse.getTrustees().get(2).getMemberId());
        assertEquals(TrustLevel.READ, fetchResponse.getTrustees().get(2).getTrustLevel());
    }

    @Test
    void testAlterTrusteeToInvalidCircleAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        final String circleId = UUID.randomUUID().toString();
        request.setAction(Action.ALTER);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No Trustee information found for member 'member1' and circle '" + circleId + "'.", cause.getMessage());
    }

    @Test
    void testAlterInvalidMemberAsTrusteeAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ALTER);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The requested Trustee could not be found.", cause.getMessage());
    }

    @Test
    void testRemoveTrusteeAsWritingTrustee() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_2);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process a Trustee", cause.getMessage());
    }

    @Test
    void testRemoveTrusteeAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);

        final ProcessTrusteeResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());

        final FetchTrusteeService fetchService = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse fetchResponse = fetchService.perform(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(2, fetchResponse.getTrustees().size());
    }

    @Test
    void testRemoveTrusteeToInvalidCircleAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        final String circleId = UUID.randomUUID().toString();
        request.setAction(Action.REMOVE);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No Trustee information found for member 'member1' and circle '" + circleId + "'.", cause.getMessage());
    }

    @Test
    void testRemoveInvalidMemberAsTrusteeAsCircleAdmin() {
        final ProcessTrusteeService service = new ProcessTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The requested Trustee could not be found.", cause.getMessage());
    }

    @Test
    void testCreateCircleAddTrusteeAndRemoveSelf() {
        final ProcessCircleService circleService = new ProcessCircleService(settings, entityManager);
        final ProcessTrusteeService trusteeService = new ProcessTrusteeService(settings, entityManager);

        // Step 1; Create new Circle of Trust
        final ProcessCircleRequest createRequest = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        createRequest.setAction(Action.CREATE);
        createRequest.setCircleName("Awesome Circle");
        final ProcessCircleResponse createResponse = perform(circleService, createRequest);
        final String circleId = createResponse.getCircleId();

        // Step 2; Add another user to the Circle
        final ProcessTrusteeRequest addRequest = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        addRequest.setAction(Action.ADD);
        addRequest.setCircleId(circleId);
        addRequest.setTrustLevel(TrustLevel.WRITE);
        addRequest.setMemberId(MEMBER_2_ID);
        perform(trusteeService, addRequest);

        // Step 3; Remove self
        final ProcessTrusteeRequest removeRequest = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        removeRequest.setAction(Action.REMOVE);
        removeRequest.setCircleId(circleId);
        removeRequest.setMemberId(MEMBER_1_ID);
        perform(trusteeService, removeRequest);

        // Step 4; As Admin, make the remaining user Circle Admin
        final ProcessTrusteeRequest alterRequest = prepareRequest(ProcessTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        alterRequest.setAction(Action.ALTER);
        alterRequest.setCircleId(circleId);
        alterRequest.setMemberId(MEMBER_2_ID);
        alterRequest.setTrustLevel(TrustLevel.ADMIN);
        perform(trusteeService, alterRequest);

        // Step 5; As member 2, I'm fetching the list of Trustees for the circle
        final FetchTrusteeService fetchService = new FetchTrusteeService(settings, entityManager);
        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, MEMBER_2);
        fetchRequest.setCircleId(circleId);
        final FetchTrusteeResponse fetchResponse = perform(fetchService, fetchRequest);
        final List<Trustee> trustees = fetchResponse.getTrustees();
        assertEquals(1, trustees.size());
        assertEquals(TrustLevel.ADMIN, trustees.get(0).getTrustLevel());
        assertEquals(MEMBER_2_ID, trustees.get(0).getMemberId());
        assertEquals(circleId, trustees.get(0).getCircleId());
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private <I extends Authentication, O extends CwsResponse> O perform(final Serviceable<?, O, I> service, final I request) {
        final O response = service.perform(request);
        assertNotNull(response);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());

        return response;
    }

    private static void detailedTrusteeAssertion(final FetchTrusteeResponse response, final String... memberIds) {
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());

        if ((memberIds != null) && (memberIds.length > 0)) {
            assertEquals(memberIds.length, response.getTrustees().size());
            for (int i = 0; i < memberIds.length; i++) {
                assertEquals(memberIds[i], response.getTrustees().get(i).getMemberId());
            }
        } else {
            assertTrue(response.getTrustees().isEmpty());
        }
    }
}
