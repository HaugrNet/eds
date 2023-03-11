/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.dtos.Trustee;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.responses.FetchTrusteeResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.enums.StandardSetting;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * <p>Common test class for the Process & Fetch Trustee Services.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class ManagementBeanTrusteeTest extends DatabaseSetup {

    @Test
    void testEmptyFetchRequest() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = new FetchTrusteeRequest();

        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.", response.getReturnMessage());
    }

    @Test
    void testEmptyProcessRequest() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        // Just making sure that the account is missing
        assertNull(request.getAccountName());

        // Should throw a VerificationException, as the request is invalid.
        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing." +
                "\nKey: action, Error: No action has been provided.", response.getReturnMessage());
    }

    @Test
    void testFetchNotExistingCircle() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(UUID.randomUUID().toString());

        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requested Circle cannot be found.", response.getReturnMessage());
    }

    @Test
    void testFetchCircle1WithShowTrueAsAdmin() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);

        assertTrue(response.isOk());
        detailedTrusteeAssertion(response, MEMBER_1_ID, MEMBER_2_ID, MEMBER_3_ID);
    }

    @Test
    void testFetchTrusteesAsMemberWithValidCircleId() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);

        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testFetchTrusteesAsMemberWithNonMemberCircleId() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_3_ID);

        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Trustee information found for member 'member1' and circle '" + CIRCLE_3_ID + "'.", response.getReturnMessage());
    }

    @Test
    void testFetchTrusteesAsMemberWithInvalidCircleId() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        final String circleId = UUID.randomUUID().toString();
        request.setCircleId(circleId);

        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requested Circle cannot be found.", response.getReturnMessage());
    }

    @Test
    void testFetchTrusteesAsAdminWithValidCircleId() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);

        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testFetchTrusteesAsAdminWithInvalidCircleId() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        final String circleId = UUID.randomUUID().toString();
        request.setCircleId(circleId);

        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requested Circle cannot be found.", response.getReturnMessage());
    }

    @Test
    void testFetchTrusteesAsMemberWithValidCircleIdAndMemberId() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);

        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testFetchTrusteesAsNonTrusteeWithCircleIdAndMemberId() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_3_ID);
        request.setMemberId(MEMBER_5_ID);

        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Trustee information found for member 'member1' and circle '" + CIRCLE_3_ID + "'.", response.getReturnMessage());
    }

    @Test
    void testFetchTrusteesAsAdminWithValidCircleIdAndMemberId() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_5_ID);

        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Unable to find any relation between given Circle & Member Id's.", response.getReturnMessage());
    }

    @Test
    void testFetchTrusteesAsAdminWithNonExistingMemberId() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setMemberId(UUID.randomUUID().toString());

        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Unable to find any Trustee information for the given Member Id.", response.getReturnMessage());
    }

    @Test
    void testFetchTrusteesAsMemberWithNonAdminPermissions() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setMemberId(MEMBER_2_ID);

        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Requesting Member is not authorized to inquire about other Member's.", response.getReturnMessage());
    }

    @Test
    void testFetchTrusteesAsSelf() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setMemberId(MEMBER_1_ID);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);

        assertEquals("Ok", response.getReturnMessage());
        assertEquals(2, response.getTrustees().size());
        assertEquals(CIRCLE_1_ID, response.getTrustees().get(0).getCircleId());
        assertEquals(CIRCLE_1, response.getTrustees().get(0).getCircleName());
        assertEquals(MEMBER_1_ID, response.getTrustees().get(0).getMemberId());
        assertEquals(MEMBER_1, response.getTrustees().get(0).getAccountName());
        assertEquals(TrustLevel.ADMIN, response.getTrustees().get(0).getTrustLevel());
        assertEquals(CIRCLE_2_ID, response.getTrustees().get(1).getCircleId());
        assertEquals(CIRCLE_2, response.getTrustees().get(1).getCircleName());
        assertEquals(MEMBER_1_ID, response.getTrustees().get(1).getMemberId());
        assertEquals(MEMBER_1, response.getTrustees().get(1).getAccountName());
        assertEquals(TrustLevel.ADMIN, response.getTrustees().get(1).getTrustLevel());
    }

    @Test
    void testFetchTrusteesWithoutParameters() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_5);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);

        assertEquals("Ok", response.getReturnMessage());
        assertEquals(1, response.getTrustees().size());
        assertEquals(CIRCLE_3_ID, response.getTrustees().get(0).getCircleId());
        assertEquals(CIRCLE_3, response.getTrustees().get(0).getCircleName());
        assertEquals(MEMBER_5_ID, response.getTrustees().get(0).getMemberId());
        assertEquals(MEMBER_5, response.getTrustees().get(0).getAccountName());
        assertEquals(TrustLevel.READ, response.getTrustees().get(0).getTrustLevel());
    }

    @Test
    void testFetchTrusteesAsAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setMemberId(MEMBER_1_ID);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);

        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testFetchCircle1WithShowFalseAsAdmin() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertTrue(response.isOk());
        detailedTrusteeAssertion(response, MEMBER_1_ID, MEMBER_2_ID, MEMBER_3_ID);
    }

    @Test
    void testFetchCircle1WithShowTrueAsMember1() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);

        assertTrue(response.isOk());
        detailedTrusteeAssertion(response, MEMBER_1_ID, MEMBER_2_ID, MEMBER_3_ID);
    }

    @Test
    void testFetchCircle1WithShowFalseAsMember1() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);

        assertNotNull(response);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(3, response.getTrustees().size());

        // Check the member records
        final Trustee trustee1 = response.getTrustees().get(0);
        assertEquals(CIRCLE_1_ID, trustee1.getCircleId());
        assertEquals(MEMBER_1_ID, trustee1.getMemberId());
        assertEquals(TrustLevel.ADMIN, trustee1.getTrustLevel());
        assertFalse(trustee1.getChanged().isBefore(trustee1.getAdded()));

        final Trustee trustee2 = response.getTrustees().get(1);
        assertEquals(CIRCLE_1_ID, trustee2.getCircleId());
        assertEquals(MEMBER_2_ID, trustee2.getMemberId());
        assertEquals(TrustLevel.WRITE, trustee2.getTrustLevel());
        assertFalse(trustee2.getChanged().isBefore(trustee2.getAdded()));

        final Trustee trustee3 = response.getTrustees().get(2);
        assertEquals(CIRCLE_1_ID, trustee3.getCircleId());
        assertEquals(MEMBER_3_ID, trustee3.getMemberId());
        assertEquals(TrustLevel.READ, trustee3.getTrustLevel());
        assertFalse(trustee3.getChanged().isBefore(trustee3.getAdded()));
    }

    @Test
    void testFetchCircle1WithShowTrueAsMember5() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_5);
        assertNotNull(request);
        request.setCircleId(CIRCLE_1_ID);

        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Trustee information found for member 'member5' and circle '" + CIRCLE_1_ID + "'.", response.getReturnMessage());
    }

    @Test
    void testFetchCircle1WithShowFalseAsMember5() {
        // Ensure that we have the correct settings for the Service
        settings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final ManagementBean bean = prepareManagementBean();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_5);
        request.setCircleId(CIRCLE_1_ID);

        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Trustee information found for member 'member5' and circle '" + CIRCLE_1_ID + "'.", response.getReturnMessage());
    }

    @Test
    void testCreatingAndAddingTrusteeAsSystemAdmin() {
        final String circleName = "Admin Circle";
        // Step 1, create a new Circle as System Administrator
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest circleRequest = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        circleRequest.setAction(Action.CREATE);
        circleRequest.setCircleName(circleName);
        final ProcessCircleResponse circleResponse = bean.processCircle(circleRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), circleResponse.getReturnCode());
        assertEquals("The Circle '" + circleName + "' was successfully created.", circleResponse.getReturnMessage());
        assertNotNull(circleResponse.getCircleId());
        final String circleId = circleResponse.getCircleId();

        // Step 2, add a new trustee to the newly created circle
        final ProcessTrusteeRequest trusteeRequest = prepareRequest(ProcessTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        trusteeRequest.setAction(Action.ADD);
        trusteeRequest.setCircleId(circleId);
        trusteeRequest.setMemberId(MEMBER_2_ID);
        trusteeRequest.setTrustLevel(TrustLevel.WRITE);
        final ProcessTrusteeResponse trusteeResponse = bean.processTrustee(trusteeRequest);
        assertEquals("The Member '" + MEMBER_2 + "' was successfully added as trustee to '" + circleName + "'.", trusteeResponse.getReturnMessage());
        assertEquals(ReturnCode.SUCCESS.getCode(), trusteeResponse.getReturnCode());

        // Step 3, verify that the Circle has 2 members
        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest.setCircleId(circleId);
        final FetchTrusteeResponse fetchResponse = bean.fetchTrustees(fetchRequest);
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
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_2);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_3_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process a Trustee", response.getReturnMessage());
    }

    @Test
    void testAddingTrusteeAsCircleAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + MEMBER_5 + "' was successfully added as trustee to '" + CIRCLE_1 + "'.", response.getReturnMessage());
    }

    @Test
    void testAddingTrusteeToInvalidCircleAsCircleAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        final String circleId = UUID.randomUUID().toString();
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Trustee information found for member 'member1' and circle '" + circleId + "'.", response.getReturnMessage());
    }

    @Test
    void testAddingInvalidMemberAsTrusteeAsCircleAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Member could be found with the given Id.", response.getReturnMessage());
    }

    @Test
    void testAddingExistingTrusteeAsTrustee() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The Member is already a trustee of the requested Circle.", response.getReturnMessage());
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
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(ADMIN_ID);
        request.setTrustLevel(TrustLevel.READ);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + Constants.ADMIN_ACCOUNT + "' was successfully added as trustee to '" + CIRCLE_1 + "'.", response.getReturnMessage());

        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse fetchResponse = bean.fetchTrustees(fetchRequest);
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

        final ProcessTrusteeResponse adminResponse = bean.processTrustee(adminRequest);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), adminResponse.getReturnCode());
        assertEquals("It is not possible to add a member to a circle, without membership.", adminResponse.getReturnMessage());
    }

    @Test
    void testAlterTrusteeAsWritingTrustee() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_2);
        request.setAction(Action.ALTER);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);
        request.setTrustLevel(TrustLevel.ADMIN);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process a Trustee", response.getReturnMessage());
    }

    @Test
    void testAlterTrusteeSetAdminAsCircleAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest circleRequest = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        circleRequest.setAction(Action.ALTER);
        circleRequest.setCircleId(CIRCLE_1_ID);
        circleRequest.setMemberId(MEMBER_2_ID);
        circleRequest.setTrustLevel(TrustLevel.ADMIN);

        final ProcessTrusteeResponse circleResponse = bean.processTrustee(circleRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), circleResponse.getReturnCode());
        assertEquals("The Trustee '" + MEMBER_2 + "' has successfully been given the trust level '" + TrustLevel.ADMIN + "' in the Circle '" + CIRCLE_1 + "'.", circleResponse.getReturnMessage());

        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse fetchResponse = bean.fetchTrustees(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(MEMBER_3_ID, fetchResponse.getTrustees().get(2).getMemberId());
        assertEquals(TrustLevel.READ, fetchResponse.getTrustees().get(2).getTrustLevel());
    }

    @Test
    void testAlterTrusteeToInvalidCircleAsCircleAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        final String circleId = UUID.randomUUID().toString();
        request.setAction(Action.ALTER);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Trustee information found for member 'member1' and circle '" + circleId + "'.", response.getReturnMessage());
    }

    @Test
    void testAlterInvalidMemberAsTrusteeAsCircleAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ALTER);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requested Trustee could not be found.", response.getReturnMessage());
    }

    @Test
    void testRemoveTrusteeAsWritingTrustee() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_2);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process a Trustee", response.getReturnMessage());
    }

    @Test
    void testRemoveTrusteeAsCircleAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(MEMBER_2_ID);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Trustee '" + MEMBER_2 + "' was successfully removed from the Circle 'circle1'.", response.getReturnMessage());

        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse fetchResponse = bean.fetchTrustees(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(2, fetchResponse.getTrustees().size());
    }

    @Test
    void testRemoveTrusteeToInvalidCircleAsCircleAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        final String circleId = UUID.randomUUID().toString();
        request.setAction(Action.REMOVE);
        request.setCircleId(circleId);
        request.setMemberId(MEMBER_5_ID);

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Trustee information found for member 'member1' and circle '" + circleId + "'.", response.getReturnMessage());
    }

    @Test
    void testRemoveInvalidMemberAsTrusteeAsCircleAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(UUID.randomUUID().toString());

        final ProcessTrusteeResponse response = bean.processTrustee(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requested Trustee could not be found.", response.getReturnMessage());
    }

    @Test
    void testCreateCircleAddTrusteeAndRemoveSelf() {
        final ManagementBean bean = prepareManagementBean();

        // Step 1; Create new Circle of Trust
        final ProcessCircleRequest createRequest = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        createRequest.setAction(Action.CREATE);
        createRequest.setCircleName("Awesome Circle");
        final ProcessCircleResponse createResponse = bean.processCircle(createRequest);
        final String circleId = createResponse.getCircleId();

        // Step 2; Add another user to the Circle
        final ProcessTrusteeRequest addRequest = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        addRequest.setAction(Action.ADD);
        addRequest.setCircleId(circleId);
        addRequest.setTrustLevel(TrustLevel.WRITE);
        addRequest.setMemberId(MEMBER_2_ID);
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processTrustee(addRequest).getReturnCode());

        // Step 3; Remove self
        final ProcessTrusteeRequest removeRequest = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        removeRequest.setAction(Action.REMOVE);
        removeRequest.setCircleId(circleId);
        removeRequest.setMemberId(MEMBER_1_ID);
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processTrustee(removeRequest).getReturnCode());

        // Step 4; As Admin, make the remaining user Circle Admin
        final ProcessTrusteeRequest alterRequest = prepareRequest(ProcessTrusteeRequest.class, Constants.ADMIN_ACCOUNT);
        alterRequest.setAction(Action.ALTER);
        alterRequest.setCircleId(circleId);
        alterRequest.setMemberId(MEMBER_2_ID);
        alterRequest.setTrustLevel(TrustLevel.ADMIN);
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processTrustee(alterRequest).getReturnCode());

        // Step 5; As member 2, I'm fetching the list of Trustees for the circle
        final FetchTrusteeRequest fetchRequest = prepareRequest(FetchTrusteeRequest.class, MEMBER_2);
        fetchRequest.setCircleId(circleId);
        final FetchTrusteeResponse fetchResponse = bean.fetchTrustees(fetchRequest);
        final List<Trustee> trustees = fetchResponse.getTrustees();
        assertEquals(1, trustees.size());
        assertEquals(TrustLevel.ADMIN, trustees.get(0).getTrustLevel());
        assertEquals(MEMBER_2_ID, trustees.get(0).getMemberId());
        assertEquals(circleId, trustees.get(0).getCircleId());
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

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
