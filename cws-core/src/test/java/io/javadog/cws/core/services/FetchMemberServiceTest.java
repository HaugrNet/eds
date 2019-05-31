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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.MemberEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;

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
final class FetchMemberServiceTest extends DatabaseSetup {

    /**
     * Testing a Request without any credentials. This should always result in
     * an error from CWS.
     */
    @Test
    void testEmptyRequest() {
        final FetchMemberService service = new FetchMemberService(settings, entityManager);
        final FetchMemberRequest request = new FetchMemberRequest();
        // Just making sure that the account is missing
        assertNull(request.getAccountName());

        // Should throw a VerificationException, as the request is invalid.
        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.", cause.getMessage());
    }

    @Test
    void testFindNotExistingAccount() {
        final FetchMemberService service = new FetchMemberService(settings, entityManager);

        // Build and send the Request
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setMemberId(UUID.randomUUID().toString());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The requested Member cannot be found.", cause.getMessage());
    }

    /**
     * This test is in response to the Bug report #55. Where a deleted member
     * cannot be found, but instead of the expected error, a complete list of
     * all members is returned.
     */
    @Test
    void testFindNotExistingAccount2() {
        // The 2 Service Classes required
        final ProcessMemberService processService = new ProcessMemberService(settings, entityManager);
        final FetchMemberService fetchService = new FetchMemberService(settings, entityManager);

        // Step 1: Add a new Member
        final ProcessMemberRequest addRequest = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        addRequest.setAction(Action.CREATE);
        addRequest.setNewAccountName("new Name");
        addRequest.setNewCredential(addRequest.getNewAccountName().getBytes(settings.getCharset()));
        final ProcessMemberResponse addResponse = processService.perform(addRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addResponse.getReturnCode());
        assertNotNull(addResponse.getMemberId());

        // Step 2: Verify that we can retrieve the Member Account
        final FetchMemberRequest fetchRequest1 = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest1.setMemberId(addResponse.getMemberId());
        final FetchMemberResponse fetchResponse1 = fetchService.perform(fetchRequest1);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse1.getReturnCode());
        assertEquals(1, fetchResponse1.getMembers().size());
        assertEquals(addResponse.getMemberId(), fetchResponse1.getMembers().get(0).getMemberId());

        // Step 3: Delete the newly created Member Account
        final ProcessMemberRequest deleteRequest = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        deleteRequest.setMemberId(addResponse.getMemberId());
        deleteRequest.setAction(Action.DELETE);
        final ProcessMemberResponse deleteResponse = processService.perform(deleteRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), deleteResponse.getReturnCode());

        // Step 3: Attempt to fetch the deleted Member Account, should result
        //         in an Exception, as no such member exists.
        final FetchMemberRequest fetchRequest2 = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest2.setMemberId(addResponse.getMemberId());
        try {
            fetchService.perform(fetchRequest2);
        } catch (CWSException e) {
            assertEquals(ReturnCode.IDENTIFICATION_WARNING, e.getReturnCode());
            assertEquals("The requested Member cannot be found.", e.getMessage());
        }
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

    private void runRequestAndVerifyResponse(final Settings mySettings, final FetchMemberRequest request) {
        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final FetchMemberResponse fetchResponse = service.perform(request);

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

        final FetchMemberService memberService = new FetchMemberService(mySettings, entityManager);
        final FetchMemberRequest memberRequest = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        assertTrue(memberRequest.validate().isEmpty());
        final FetchMemberResponse fetchedMemberResponse = memberService.perform(memberRequest);

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

        final FetchMemberService fetchService = new FetchMemberService(mySettings, entityManager);
        final FetchMemberRequest fetchRequest = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest.setMemberId(ADMIN_ID);
        assertTrue(fetchRequest.validate().isEmpty());
        final FetchMemberResponse fetchResponse = fetchService.perform(fetchRequest);

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

        final FetchMemberService memberService = new FetchMemberService(mySettings, entityManager);
        final MemberEntity firstMember = findFirstMember();
        final FetchMemberRequest memberRequest = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        memberRequest.setMemberId(firstMember.getExternalId());
        final FetchMemberResponse memberResponse = memberService.perform(memberRequest);

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

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

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

        final FetchMemberService memberService = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest memberRequest = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        memberRequest.setMemberId(member.getExternalId());
        final FetchMemberResponse memberResponse = memberService.perform(memberRequest);

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

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

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

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_4);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertNotNull(response);
        assertEquals(1, response.getMembers().size());
        // Member 1 is Administrator for 2 Circles, Ciecle1 & Circle2
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

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_4);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertEquals(1, response.getMembers().size());
        assertEquals(MEMBER_1_ID, response.getMembers().get(0).getMemberId());
        // Member 1 is Administrator for 2 Circles, Ciecle1 & Circle2
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

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_5);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

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

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_5);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

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
