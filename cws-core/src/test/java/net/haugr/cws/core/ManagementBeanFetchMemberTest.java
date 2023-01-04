/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.cws.api.common.Action;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.dtos.Member;
import net.haugr.cws.api.requests.FetchMemberRequest;
import net.haugr.cws.api.requests.ProcessMemberRequest;
import net.haugr.cws.api.responses.FetchMemberResponse;
import net.haugr.cws.api.responses.ProcessMemberResponse;
import net.haugr.cws.core.enums.StandardSetting;
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.model.entities.MemberEntity;
import net.haugr.cws.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;
import java.util.UUID;

/**
 * <p>Fetching members is similar to fetching Circles, in that it is possible to
 * retrieve a list of Members in the CWS, and information about a specific
 * Member Account. However, there's also a property values which influence who
 * may see what:</p>
 * <ul>
 *   <li><b>cws.show.other.member.information</b><br>
 *       <i>If two Member Accounts are connected, then they should also be able
 *       to retrieve information about each other, for all shared information.
 *       However, for non-shared information, it may not be desirable to allow
 *       this. Hence, this Property controls it. If set, then a Member can only
 *       see information about Members with shared information, but nothing
 *       else. The value does not apply to the listing of Accounts, as this is
 *       required if a Circle Administrator wishes to add a new Member to a
 *       Circle.</i></li>
 * </ul>
 *
 * <p>The tests in this Test class reflects the above limitations.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class ManagementBeanFetchMemberTest extends DatabaseSetup {

    /**
     * Testing a Request without any credentials. This should always result in
     * an error from CWS.
     */
    @Test
    void testEmptyRequest() {
        final ManagementBean bean = prepareManagementBean();
        final FetchMemberRequest request = new FetchMemberRequest();
        // Just making sure that the account is missing
        assertNull(request.getAccountName());

        // Should throw a VerificationException, as the request is invalid.
        final FetchMemberResponse response = bean.fetchMembers(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.", response.getReturnMessage());
    }

    @Test
    void testFindNotExistingAccount() {
        final ManagementBean bean = prepareManagementBean();

        // Build and send the Request
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setMemberId(UUID.randomUUID().toString());

        final FetchMemberResponse response = bean.fetchMembers(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requested Member cannot be found.", response.getReturnMessage());
    }

    /**
     * This test is in response to the Bug report #55. Where a deleted member
     * cannot be found, but instead of the expected error, a complete list of
     * all members is returned.
     */
    @Test
    void testFindNotExistingAccount2() {
        final ManagementBean bean = prepareManagementBean();

        // Step 1: Add a new Member
        final ProcessMemberRequest addRequest = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        addRequest.setAction(Action.CREATE);
        addRequest.setNewAccountName("new Name");
        addRequest.setNewCredential(addRequest.getNewAccountName().getBytes(settings.getCharset()));
        final ProcessMemberResponse addResponse = bean.processMember(addRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addResponse.getReturnCode());
        assertNotNull(addResponse.getMemberId());

        // Step 2: Verify that we can retrieve the Member Account
        final FetchMemberRequest fetchRequest1 = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest1.setMemberId(addResponse.getMemberId());
        final FetchMemberResponse fetchResponse1 = bean.fetchMembers(fetchRequest1);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse1.getReturnCode());
        assertEquals(1, fetchResponse1.getMembers().size());
        assertEquals(addResponse.getMemberId(), fetchResponse1.getMembers().get(0).getMemberId());

        // Step 3: Delete the newly created Member Account
        final ProcessMemberRequest deleteRequest = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        deleteRequest.setMemberId(addResponse.getMemberId());
        deleteRequest.setAction(Action.DELETE);
        final ProcessMemberResponse deleteResponse = bean.processMember(deleteRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), deleteResponse.getReturnCode());

        // Step 3: Attempt to fetch the deleted Member Account, should result
        //         in an Exception, as no such member exists.
        final FetchMemberRequest fetchRequest2 = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest2.setMemberId(addResponse.getMemberId());
        final FetchMemberResponse fetchResponse2 = bean.fetchMembers(fetchRequest2);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), fetchResponse2.getReturnCode());
        assertEquals("The requested Member cannot be found.", fetchResponse2.getReturnMessage());
    }

    // =========================================================================
    // Testing fetching All members using different Accounts & Settings
    // =========================================================================

    /**
     * This test expects a list of All Members, including the System
     * Administrator, which should be 6 in total. No Circles are fetched,
     * as we're not looking at a specific Member Account.
     */
    @Test
    void testFindAllMembersAsAdmin() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();

        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        assertTrue(request.validate().isEmpty());
        runRequestAndVerifyResponse(mySettings, request);
    }

    // TODO merge if only used once...
    private void runRequestAndVerifyResponse(final Settings mySettings, final FetchMemberRequest request) {
        final ManagementBean bean = prepareManagementBean(mySettings);
        final FetchMemberResponse fetchResponse = bean.fetchMembers(request);

        // Verify that we have found the correct data
        assertNotNull(fetchResponse);
        assertTrue(fetchResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals("Ok", fetchResponse.getReturnMessage());
        assertEquals(6, fetchResponse.getMembers().size());
        assertTrue(fetchResponse.getCircles().isEmpty());
    }

    /**
     * This test expects a list of All Members, excluding the System
     * Administrator, which should be 5 in total. No Circles are fetched,
     * as we're not looking at a specific Member Account.
     */
    @Test
    void testFindAllMembersAsMember1() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();

        final ManagementBean bean = prepareManagementBean(mySettings);
        final FetchMemberRequest memberRequest = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        assertTrue(memberRequest.validate().isEmpty());
        final FetchMemberResponse fetchedMemberResponse = bean.fetchMembers(memberRequest);

        // Verify that we have found the correct data
        assertNotNull(fetchedMemberResponse);
        assertTrue(fetchedMemberResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchedMemberResponse.getReturnCode());
        assertEquals("Ok", fetchedMemberResponse.getReturnMessage());
        assertEquals(6, fetchedMemberResponse.getMembers().size());
        assertTrue(fetchedMemberResponse.getCircles().isEmpty());

        // Check that the member information is present
        final Member member0 = fetchedMemberResponse.getMembers().get(0);
        assertEquals(Constants.ADMIN_ACCOUNT, member0.getAccountName());
        assertEquals(ADMIN_ID, member0.getMemberId());
        assertNotNull(member0.getAdded());

        final Member member1 = fetchedMemberResponse.getMembers().get(1);
        assertEquals(MEMBER_1, member1.getAccountName());
        assertEquals(MEMBER_1_ID, member1.getMemberId());
        assertNotNull(member1.getAdded());

        final Member member5 = fetchedMemberResponse.getMembers().get(5);
        assertEquals(MEMBER_5, member5.getAccountName());
        assertEquals(MEMBER_5_ID, member5.getMemberId());
        assertNotNull(member5.getAdded());
    }

    //==========================================================================
    // Testing fetching Specific Account using different Accounts & Settings
    //==========================================================================

    /**
     * <p>The System Administrator should always be able to find all Accounts,
     * regardless of the Settings. The System Administrator is not and cannot
     * be part of any Circles.</p>
     */
    @Test
    void testFindAdminAsAdmin() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();

        final ManagementBean bean = prepareManagementBean(mySettings);
        final FetchMemberRequest fetchRequest = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest.setMemberId(ADMIN_ID);
        assertTrue(fetchRequest.validate().isEmpty());
        final FetchMemberResponse fetchResponse = bean.fetchMembers(fetchRequest);

        // Verify that we have found the correct data
        assertNotNull(fetchResponse);
        assertTrue(fetchResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals("Ok", fetchResponse.getReturnMessage());
        assertEquals(1, fetchResponse.getMembers().size());
        assertTrue(fetchResponse.getCircles().isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, fetchResponse.getMembers().get(0).getAccountName());
    }

    /**
     * <p>The System Administrator should always be able to find all Accounts,
     * regardless of the Settings.</p>
     */
    @Test
    void testFindMember1WithShowOtherFalseAsAdmin() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final ManagementBean bean = prepareManagementBean(mySettings);
        final MemberEntity firstMember = findFirstMember();
        final FetchMemberRequest memberRequest = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        memberRequest.setMemberId(firstMember.getExternalId());
        final FetchMemberResponse memberResponse = bean.fetchMembers(memberRequest);

        // Verify that we have found the correct data
        assertNotNull(memberResponse);
        assertTrue(memberResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), memberResponse.getReturnCode());
        assertEquals("Ok", memberResponse.getReturnMessage());
        assertEquals(1, memberResponse.getMembers().size());
        assertEquals(2, memberResponse.getCircles().size());
        assertEquals(firstMember.getName(), memberResponse.getMembers().get(0).getAccountName());
    }

    /**
     * <p>The System Administrator should always be able to find all Accounts,
     * regardless of the Settings.</p>
     */
    @Test
    void testFindMember1WithShowOtherTrueAsAdmin() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final ManagementBean bean = prepareManagementBean(mySettings);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = bean.fetchMembers(request);

        // Verify that we have found the correct data
        assertNotNull(response);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(1, response.getMembers().size());
        assertEquals(2, response.getCircles().size());
        assertEquals(member.getName(), response.getMembers().get(0).getAccountName());
    }

    @Test
    void testFindMember1WithShowOtherTrueAsMember1() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final ManagementBean bean = prepareManagementBean(mySettings);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest memberRequest = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        memberRequest.setMemberId(member.getExternalId());
        final FetchMemberResponse memberResponse = bean.fetchMembers(memberRequest);

        // Verify that we have found the correct data
        assertNotNull(memberResponse);
        assertEquals(1, memberResponse.getMembers().size());
        assertEquals(2, memberResponse.getCircles().size());
        assertEquals(member.getName(), memberResponse.getMembers().get(0).getAccountName());
    }

    @Test
    void testFindMember1WithShowOtherFalseAsMember1() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final ManagementBean bean = prepareManagementBean(mySettings);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = bean.fetchMembers(request);

        // Verify that we have found the correct data
        assertNotNull(response);
        assertEquals(1, response.getMembers().size());
        assertEquals(2, response.getCircles().size());
        assertEquals(member.getName(), response.getMembers().get(0).getAccountName());
    }

    @Test
    void testFindMember1WithShowOtherTrueAsMember4() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final ManagementBean bean = prepareManagementBean(mySettings);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_4);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = bean.fetchMembers(request);

        // Verify that we have found the correct data
        assertNotNull(response);
        assertEquals(1, response.getMembers().size());
        // Member 1 is Administrator for 2 Circles, Circle1 & Circle2
        // Member 4 is Administrator for 2 Circles, Circle2 & Circle3
        // As SHOW_TRUSTEES is enabled, we're only getting all Circles
        // for Member 1, i.e. 2 Circles
        assertEquals(2, response.getCircles().size());
    }

    @Test
    void testFindMember1WithShowOtherFalseAsMember4() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final ManagementBean bean = prepareManagementBean(mySettings);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_4);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = bean.fetchMembers(request);

        // Verify that we have found the correct data
        assertEquals(1, response.getMembers().size());
        assertEquals(MEMBER_1_ID, response.getMembers().get(0).getMemberId());
        // Member 1 is Administrator for 2 Circles, Circle1 & Circle2
        // Member 4 is Administrator for 2 Circles, Circle2 & Circle3
        // As SHOW_TRUSTEES is disabled, we're only getting shared
        // Circle information, meaning we only get Circle2 here.
        assertEquals(1, response.getCircles().size());
    }

    @Test
    void testFindMember1WithShowOtherTrueAsMember5() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final ManagementBean bean = prepareManagementBean(mySettings);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_5);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = bean.fetchMembers(request);

        // Verify that we have found the correct data
        assertNotNull(response);
        assertEquals(1, response.getMembers().size());
        assertEquals(2, response.getCircles().size());
    }

    @Test
    void testFindMember1WithShowOtherFalseAsMember5() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final ManagementBean bean = prepareManagementBean(mySettings);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_5);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = bean.fetchMembers(request);

        // Verify that we have found the correct data
        assertNotNull(response);
        assertEquals(1, response.getMembers().size());
        assertTrue(response.getCircles().isEmpty());
    }

    /**
     * Finds and returns the first Member, i.e. 'member1'.
     *
     * @return Member1, which is only a member of the first Circle
     */
    private MemberEntity findFirstMember() {
        return entityManager.find(MemberEntity.class, 2L);
    }
}
