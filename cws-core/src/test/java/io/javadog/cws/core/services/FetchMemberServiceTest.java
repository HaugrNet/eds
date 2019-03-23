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

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.MemberEntity;
import org.junit.Test;

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
public final class FetchMemberServiceTest extends DatabaseSetup {

    /**
     * Testing a Request without any credentials. This should always result in
     * an error from CWS.
     */
    @Test
    public void testEmptyRequest() {
        prepareCause(ReturnCode.VERIFICATION_WARNING, "Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.");

        final FetchMemberService service = new FetchMemberService(settings, entityManager);
        final FetchMemberRequest request = new FetchMemberRequest();
        // Just making sure that the account is missing
        assertThat(request.getAccountName(), is(nullValue()));

        // Should throw a VerificationException, as the request is invalid.
        service.perform(request);
    }

    @Test
    public void testFindNotExistingAccount() {
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "The requested Member cannot be found.");

        final FetchMemberService service = new FetchMemberService(settings, entityManager);

        // Build and send the Request
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setMemberId(UUID.randomUUID().toString());
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    //==========================================================================
    // Testing fetching All members using different Accounts & Settings
    //==========================================================================

    /**
     * This test expects a list of All Members, including the System
     * Administrator, which should be 6 in total. No Circles are fetched,
     * as we're not looking at a specific Member Account.
     */
    @Test
    public void testFindAllMembersAsAdmin() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();

        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        assertThat(request.validate().isEmpty(), is(true));
        runRequestAndVerifyResponse(mySettings, request);
    }

    private void runRequestAndVerifyResponse(final Settings mySettings, final FetchMemberRequest request) {
        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final FetchMemberResponse fetchResponse = service.perform(request);

        // Verify that we have found the correct data
        assertThat(fetchResponse, is(not(nullValue())));
        assertThat(fetchResponse.isOk(), is(true));
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchResponse.getReturnMessage(), is("Ok"));
        assertThat(fetchResponse.getMembers().size(), is(6));
        assertThat(fetchResponse.getCircles().isEmpty(), is(true));
    }

    /**
     * This test expects a list of All Members, excluding the System
     * Administrator, which should be 5 in total. No Circles are fetched,
     * as we're not looking at a specific Member Account.
     */
    @Test
    public void testFindAllMembersAsMember1() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();

        final FetchMemberService memberService = new FetchMemberService(mySettings, entityManager);
        final FetchMemberRequest memberRequest = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        assertThat(memberRequest.validate().isEmpty(), is(true));
        final FetchMemberResponse fetchedMemberResponse = memberService.perform(memberRequest);

        // Verify that we have found the correct data
        assertThat(fetchedMemberResponse, is(not(nullValue())));
        assertThat(fetchedMemberResponse.isOk(), is(true));
        assertThat(fetchedMemberResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchedMemberResponse.getReturnMessage(), is("Ok"));
        assertThat(fetchedMemberResponse.getMembers().size(), is(6));
        assertThat(fetchedMemberResponse.getCircles().isEmpty(), is(true));

        // Check that the member information is present
        final Member member0 = fetchedMemberResponse.getMembers().get(0);
        assertThat(member0.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(member0.getMemberId(), is(ADMIN_ID));
        assertThat(member0.getAdded(), is(not(nullValue())));

        final Member member1 = fetchedMemberResponse.getMembers().get(1);
        assertThat(member1.getAccountName(), is(MEMBER_1));
        assertThat(member1.getMemberId(), is(MEMBER_1_ID));
        assertThat(member1.getAdded(), is(not(nullValue())));

        final Member member5 = fetchedMemberResponse.getMembers().get(5);
        assertThat(member5.getAccountName(), is(MEMBER_5));
        assertThat(member5.getMemberId(), is(MEMBER_5_ID));
        assertThat(member5.getAdded(), is(not(nullValue())));
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
    public void testFindAdminAsAdmin() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();

        final FetchMemberService fetchService = new FetchMemberService(mySettings, entityManager);
        final FetchMemberRequest fetchRequest = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        fetchRequest.setMemberId(ADMIN_ID);
        assertThat(fetchRequest.validate().isEmpty(), is(true));
        final FetchMemberResponse fetchResponse = fetchService.perform(fetchRequest);

        // Verify that we have found the correct data
        assertThat(fetchResponse, is(not(nullValue())));
        assertThat(fetchResponse.isOk(), is(true));
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchResponse.getReturnMessage(), is("Ok"));
        assertThat(fetchResponse.getMembers().size(), is(1));
        assertThat(fetchResponse.getCircles().isEmpty(), is(true));
        assertThat(fetchResponse.getMembers().get(0).getAccountName(), is(Constants.ADMIN_ACCOUNT));
    }

    /**
     * <p>The System Administrator should always be able to find all Accounts,
     * regardless of the Settings.</p>
     */
    @Test
    public void testFindMember1WithShowOtherFalseAsAdmin() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final FetchMemberService memberService = new FetchMemberService(mySettings, entityManager);
        final MemberEntity firstMember = findFirstMember();
        final FetchMemberRequest memberRequest = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        memberRequest.setMemberId(firstMember.getExternalId());
        final FetchMemberResponse memberResponse = memberService.perform(memberRequest);

        // Verify that we have found the correct data
        assertThat(memberResponse, is(not(nullValue())));
        assertThat(memberResponse.isOk(), is(true));
        assertThat(memberResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(memberResponse.getReturnMessage(), is("Ok"));
        assertThat(memberResponse.getMembers().size(), is(1));
        assertThat(memberResponse.getCircles().size(), is(2));
        assertThat(memberResponse.getMembers().get(0).getAccountName(), is(firstMember.getName()));
    }

    /**
     * <p>The System Administrator should always be able to find all Accounts,
     * regardless of the Settings.</p>
     */
    @Test
    public void testFindMember1WithShowOtherTrueAsAdmin() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getMembers().get(0).getAccountName(), is(member.getName()));
    }

    @Test
    public void testFindMember1WithShowOtherTrueAsMember1() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final FetchMemberService memberService = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest memberRequest = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        memberRequest.setMemberId(member.getExternalId());
        final FetchMemberResponse memberResponse = memberService.perform(memberRequest);

        // Verify that we have found the correct data
        assertThat(memberResponse, is(not(nullValue())));
        assertThat(memberResponse.getMembers().size(), is(1));
        assertThat(memberResponse.getCircles().size(), is(2));
        assertThat(memberResponse.getMembers().get(0).getAccountName(), is(member.getName()));
    }

    @Test
    public void testFindMember1WithShowOtherFalseAsMember1() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getMembers().get(0).getAccountName(), is(member.getName()));
    }

    @Test
    public void testFindMember1WithShowOtherTrueAsMember4() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, "member4");
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(2));
    }

    @Test
    public void testFindMember1WithShowOtherFalseAsMember4() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_4);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(1));
    }

    @Test
    public void testFindMember1WithShowOtherTrueAsMember5() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "true");

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_5);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(2));
    }

    @Test
    public void testFindMember1WithShowOtherFalseAsMember5() {
        // Ensure that we have the correct settings for the Service
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "false");

        final FetchMemberService service = new FetchMemberService(mySettings, entityManager);
        final MemberEntity member = findFirstMember();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_5);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(0));
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
