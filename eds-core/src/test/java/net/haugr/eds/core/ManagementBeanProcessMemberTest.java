/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.dtos.Member;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.SessionEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class ManagementBeanProcessMemberTest extends DatabaseSetup {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    void testNullRequest() {
        final ManagementBean bean = prepareManagementBean();

        final ProcessMemberResponse response = bean.processMember(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot Process a NULL Object.", response.getReturnMessage());
    }

    @Test
    void testCreateCircleAsNewMember() {
        final ManagementBean bean = prepareManagementBean();

        final String accountName = "accountName";
        final ProcessMemberRequest memberRequest = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        memberRequest.setNewAccountName(accountName);
        memberRequest.setNewCredential(credentialOf(accountName));
        memberRequest.setAction(Action.CREATE);
        final ProcessMemberResponse memberResponse = bean.processMember(memberRequest);
        assertTrue(memberResponse.isOk());

        final ProcessCircleRequest circleRequest = prepareRequest(ProcessCircleRequest.class, accountName);
        circleRequest.setCircleName("circleName");
        circleRequest.setAction(Action.CREATE);
        final ProcessCircleResponse circleResponse = bean.processCircle(circleRequest);
        assertTrue(circleResponse.isOk());
    }

    @Test
    void testAddingWithoutRole() {
        final String account = "Created Member";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(credentialOf(account));
        final ProcessMemberResponse response = bean.processMember(request);

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + account + "' was successfully added to EDS.", response.getReturnMessage());
    }

    @Test
    void testAddingWithAdminRole() {
        final String account = "Created Member";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(credentialOf(account));
        request.setMemberRole(MemberRole.ADMIN);
        final ProcessMemberResponse response = bean.processMember(request);

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + account + "' was successfully added to EDS.", response.getReturnMessage());
    }

    // =========================================================================
    // Several administrators: the first one rules, invitations make users
    // =========================================================================

    /**
     * An invitation never makes an administrator, also not when the first
     * System Administrator asks for it: a link in the wrong hands must not
     * give more than a user.
     */
    @Test
    void testInvitationAsAdministratorIsRefused() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("would be admin");
        request.setMemberRole(MemberRole.ADMIN);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("An invitation makes a user; an administrator is made by creating the account, or by changing the role afterwards.", response.getReturnMessage());
        assertNull(dao.findMemberByName("would be admin"));
    }

    @Test
    void testInvitationMakesAUser() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invited user");
        request.setMemberRole(MemberRole.STANDARD);

        assertTrue(bean.processMember(request).isOk());
        assertEquals(MemberRole.STANDARD, dao.findMemberByName("invited user").getMemberRole());
    }

    /**
     * Another administrator runs users, but doesn't make, unmake or delete
     * administrators; that is for the first one alone.
     */
    @Test
    void testOtherAdministratorDoesNotRuleAdministrators() {
        final ManagementBean bean = prepareManagementBean();
        final String otherAdmin = createAdministrator(bean);
        final String thirdAdmin = "third admin";
        final ProcessMemberRequest third = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        third.setAction(Action.CREATE);
        third.setNewAccountName(thirdAdmin);
        third.setNewCredential(credentialOf(thirdAdmin));
        third.setMemberRole(MemberRole.ADMIN);
        assertTrue(bean.processMember(third).isOk());
        final String thirdId = dao.findMemberByName(thirdAdmin).getExternalId();

        // A user, yes.
        final ProcessMemberRequest user = prepareRequest(ProcessMemberRequest.class, otherAdmin);
        user.setAction(Action.CREATE);
        user.setNewAccountName("made by the other");
        user.setNewCredential(credentialOf("made by the other"));
        final ProcessMemberResponse made = bean.processMember(user);
        assertEquals(ReturnCode.SUCCESS.getCode(), made.getReturnCode(), made.getReturnMessage());
        assertEquals(MemberRole.STANDARD, dao.findMemberByName("made by the other").getMemberRole());

        // An administrator, no.
        final ProcessMemberRequest admin = prepareRequest(ProcessMemberRequest.class, otherAdmin);
        admin.setAction(Action.CREATE);
        admin.setNewAccountName("another admin");
        admin.setNewCredential(credentialOf("another admin"));
        admin.setMemberRole(MemberRole.ADMIN);
        final ProcessMemberResponse refused = bean.processMember(admin);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), refused.getReturnCode());
        assertEquals("Only the first System Administrator may make, unmake or delete administrators.", refused.getReturnMessage());
        assertNull(dao.findMemberByName("another admin"));

        // Nor a role.
        final ProcessMemberRequest alter = prepareRequest(ProcessMemberRequest.class, otherAdmin);
        alter.setAction(Action.ALTER);
        alter.setMemberId(MEMBER_1_ID);
        alter.setMemberRole(MemberRole.ADMIN);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), bean.processMember(alter).getReturnCode());
        assertEquals(MemberRole.STANDARD, dao.findMemberByName(MEMBER_1).getMemberRole());

        // Nor another administrator deleted; a user may be.
        final ProcessMemberRequest delete = prepareRequest(ProcessMemberRequest.class, otherAdmin);
        delete.setAction(Action.DELETE);
        delete.setMemberId(thirdId);
        final ProcessMemberResponse kept = bean.processMember(delete);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), kept.getReturnCode());
        assertEquals("Only the first System Administrator may make, unmake or delete administrators.", kept.getReturnMessage());
        assertNotNull(dao.findMemberByName(thirdAdmin));
        final ProcessMemberRequest deleteUser = prepareRequest(ProcessMemberRequest.class, otherAdmin);
        deleteUser.setAction(Action.DELETE);
        deleteUser.setMemberId(made.getMemberId());
        assertTrue(bean.processMember(deleteUser).isOk());
    }

    /**
     * The first System Administrator makes and unmakes administrators, and
     * deletes them; the member data tells who it is.
     */
    @Test
    void testFirstAdministratorRulesAdministrators() {
        final ManagementBean bean = prepareManagementBean();
        final String otherAdmin = createAdministrator(bean);
        final String otherId = dao.findMemberByName(otherAdmin).getExternalId();

        final ProcessMemberRequest demote = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        demote.setAction(Action.ALTER);
        demote.setMemberId(otherId);
        demote.setMemberRole(MemberRole.STANDARD);
        assertTrue(bean.processMember(demote).isOk());
        assertEquals(MemberRole.STANDARD, dao.findMemberByName(otherAdmin).getMemberRole());

        final ProcessMemberRequest promote = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        promote.setAction(Action.ALTER);
        promote.setMemberId(otherId);
        promote.setMemberRole(MemberRole.ADMIN);
        assertTrue(bean.processMember(promote).isOk());

        // Who is the first is told to every Member who sees the list.
        final FetchMemberResponse members = bean.fetchMembers(prepareRequest(FetchMemberRequest.class, MEMBER_1));
        assertTrue(members.isOk(), members.getReturnMessage());
        assertEquals(1, members.getMembers().stream().filter(Member::isFirstAdministrator).count());
        assertTrue(members.getMembers().stream().filter(Member::isFirstAdministrator).allMatch(m -> ADMIN_ID.equals(m.getMemberId())));
        assertTrue(members.getMembers().stream().anyMatch(m -> otherId.equals(m.getMemberId()) && !m.isFirstAdministrator() && (m.getMemberRole() == MemberRole.ADMIN)));
        final FetchMemberRequest one = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        one.setMemberId(ADMIN_ID);
        assertTrue(bean.fetchMembers(one).getMembers().getFirst().isFirstAdministrator());

        final ProcessMemberRequest delete = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        delete.setAction(Action.DELETE);
        delete.setMemberId(otherId);
        final ProcessMemberResponse deleted = bean.processMember(delete);
        assertEquals(ReturnCode.SUCCESS.getCode(), deleted.getReturnCode(), deleted.getReturnMessage());
        assertNull(dao.findMemberByName(otherAdmin));
    }

    @Test
    void testAddingWithStandardRole() {
        final String account = "Created Member";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(credentialOf(account));
        request.setMemberRole(MemberRole.STANDARD);
        final ProcessMemberResponse response = bean.processMember(request);

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + account + "' was successfully added to EDS.", response.getReturnMessage());
    }

    @Test
    void testAddingAsMember() {
        final String account = "Member Added Member";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(credentialOf(account));
        request.setMemberRole(MemberRole.STANDARD);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Members are not permitted to create new Accounts.", response.getReturnMessage());
    }

    @Test
    void testAddingWithPublicKey() {
        final String account = "Member with PublicKey";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(credentialOf(account));
        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + account + "' was successfully added to EDS.", response.getReturnMessage());

        final ProcessMemberRequest updateRequest = prepareRequest(ProcessMemberRequest.class, account);
        updateRequest.setAction(Action.UPDATE);
        updateRequest.setPublicKey(UUID.randomUUID().toString());
        final ProcessMemberResponse updateResponse = bean.processMember(updateRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());
        assertEquals("The Member '" + account + "' was successfully updated.", updateResponse.getReturnMessage());

        final FetchMemberRequest fetchRequest = prepareRequest(FetchMemberRequest.class, MEMBER_4);
        fetchRequest.setMemberId(response.getMemberId());
        final FetchMemberResponse fetchResponse = bean.fetchMembers(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals("Ok", fetchResponse.getReturnMessage());
        assertEquals(1, fetchResponse.getMembers().size());
        assertEquals(updateRequest.getPublicKey(), fetchResponse.getMembers().getFirst().getPublicKey());
    }

    @Test
    void testAddingWithExistingAccountName() {
        final String account = MEMBER_4;
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(credentialOf(account));

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("An Account with the requested AccountName already exist.", response.getReturnMessage());
    }

    @Test
    void testAlterAccount() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ALTER);
        request.setMemberId(MEMBER_1_ID);
        request.setMemberRole(MemberRole.ADMIN);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + MEMBER_1 + "' has successfully been given the new role '" + MemberRole.ADMIN + "'.", response.getReturnMessage());
    }

    @Test
    void testAlterUnknownAccount() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ALTER);
        request.setMemberId(UUID.randomUUID().toString());
        request.setMemberRole(MemberRole.ADMIN);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requested Member cannot be found.", response.getReturnMessage());
    }

    @Test
    void testAlterSelf() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ALTER);
        request.setMemberId(ADMIN_ID);
        request.setMemberRole(MemberRole.STANDARD);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("It is not permitted to alter own account.", response.getReturnMessage());
    }

    @Test
    void testAlterAccountAsMember() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_3);
        request.setAction(Action.ALTER);
        request.setMemberId(MEMBER_1_ID);
        request.setMemberRole(MemberRole.ADMIN);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Only Administrators may update the Role of a member.", response.getReturnMessage());
    }

    @Test
    void testProcessingSelf() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_5);
        request.setAction(Action.UPDATE);
        request.setMemberId(MEMBER_5_ID);
        request.setNewAccountName(null);
        request.setNewCredential(null);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + MEMBER_5 + "' was successfully updated.", response.getReturnMessage());
    }

    @Test
    void testProcessSelfPasswordUpdateWithSession() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest loginRequest = prepareLoginRequest(MEMBER_5);
        final ProcessMemberResponse loginResponse = bean.processMember(loginRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), loginResponse.getReturnCode());
        assertEquals("The Member '" + MEMBER_5 + "' has successfully logged in.", loginResponse.getReturnMessage());

        final ProcessMemberRequest passwordRequest = prepareSessionRequest(ProcessMemberRequest.class, loginResponse.getSession());
        passwordRequest.setAction(Action.UPDATE);
        passwordRequest.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));

        final ProcessMemberResponse response = bean.processMember(passwordRequest);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("It is only permitted to update the credentials when authenticating with Passphrase.", response.getReturnMessage());
    }

    @Test
    void testInvitation() {
        final String account = "invitee";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName(account);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertNotNull(response.getMemberId());
        final byte[] invitation = response.getInvitation();

        final ProcessMemberRequest invitationRequest = new ProcessMemberRequest();
        invitationRequest.setAccountName(account);
        invitationRequest.setAction(Action.UPDATE);
        invitationRequest.setCredentialType(CredentialType.INVITATION);
        invitationRequest.setCredential(invitation);
        invitationRequest.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));

        final ProcessMemberResponse invitationResponse = bean.processMember(invitationRequest);
        assertNotNull(invitationResponse);
        assertEquals(ReturnCode.SUCCESS.getCode(), invitationResponse.getReturnCode());
        assertEquals("An invitation was successfully issued for '" + account + "'.", response.getReturnMessage());
    }

    @Test
    void testNullNewCredentialForInvitation() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName("null Invitee");
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.INVITATION);
        request.setCredential(crypto.stringToBytes("Invitation"));

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The newCredential is missing in Request.", response.getReturnMessage());
    }

    @Test
    void testEmptyNewCredentialForInvitation() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName("empty Invitee");
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.INVITATION);
        request.setCredential(crypto.stringToBytes("Invitation"));
        request.setNewCredential(crypto.stringToBytes(""));

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The newCredential is missing in Request.", response.getReturnMessage());
    }

    @Test
    void testInvitationWithInvalidToken() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());

        final byte[] bogusToken = new byte[32];
        RANDOM.nextBytes(bogusToken);
        final ProcessMemberRequest invitationRequest = new ProcessMemberRequest();
        invitationRequest.setAccountName("invitee");
        invitationRequest.setAction(Action.UPDATE);
        invitationRequest.setCredentialType(CredentialType.INVITATION);
        invitationRequest.setCredential(bogusToken);
        invitationRequest.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));

        final ProcessMemberResponse invitationResponse = bean.processMember(invitationRequest);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), invitationResponse.getReturnCode());
        assertEquals("The given invitation is invalid.", invitationResponse.getReturnMessage());
    }

    @Test
    void testExpiredInvitation() {
        final String account = "late invitee";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName(account);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());

        final MemberEntity invitee = dao.findMemberByName(account);
        invitee.setInvitationExpire(Utilities.newDate().minusMinutes(1));
        dao.save(invitee);

        final ProcessMemberRequest invitationRequest = new ProcessMemberRequest();
        invitationRequest.setAccountName(account);
        invitationRequest.setAction(Action.UPDATE);
        invitationRequest.setCredentialType(CredentialType.INVITATION);
        invitationRequest.setCredential(response.getInvitation());
        invitationRequest.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));

        final ProcessMemberResponse invitationResponse = bean.processMember(invitationRequest);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), invitationResponse.getReturnCode());
        assertEquals("The invitation has expired.", invitationResponse.getReturnMessage());
    }

    @Test
    void testInvitationTokenIsOnlyStoredAsKeyedHash() {
        final String account = "hashed invitee";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName(account);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        final byte[] token = response.getInvitation();
        assertEquals(32, token.length);

        final MemberEntity invitee = dao.findMemberByName(account);
        final String armoredToken = Base64.getEncoder().encodeToString(token);
        assertNotEquals(armoredToken, invitee.getSalt());
        assertFalse(invitee.getSalt().contains(armoredToken));
        assertFalse(invitee.getPublicKey().contains(armoredToken));
        assertFalse(invitee.getPrivateKey().contains(armoredToken));
        assertEquals(crypto.hmacWithMasterKey("eds/invitation", token), invitee.getSalt());
        assertNotNull(invitee.getInvitationExpire());
    }

    @Test
    void testInvitationCanOnlyBeUsedOnce() {
        final String account = "once invitee";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName(account);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        final byte[] token = response.getInvitation();

        final ProcessMemberRequest first = new ProcessMemberRequest();
        first.setAccountName(account);
        first.setAction(Action.UPDATE);
        first.setCredentialType(CredentialType.INVITATION);
        first.setCredential(Arrays.copyOf(token, token.length));
        first.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processMember(first).getReturnCode());
        assertNull(dao.findMemberByName(account).getInvitationExpire());

        final ProcessMemberRequest second = new ProcessMemberRequest();
        second.setAccountName(account);
        second.setAction(Action.UPDATE);
        second.setCredentialType(CredentialType.INVITATION);
        second.setCredential(token);
        second.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));
        final ProcessMemberResponse secondResponse = bean.processMember(second);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), secondResponse.getReturnCode());
        assertEquals("Account does not have an invitation pending.", secondResponse.getReturnMessage());

        // The new passphrase works for the account.
        final FetchMemberRequest fetchRequest = prepareRequest(FetchMemberRequest.class, account);
        fetchRequest.setCredential(crypto.stringToBytes(NEW_PASSPHRASE));
        assertTrue(bean.fetchMembers(fetchRequest).isOk());
    }

    @Test
    void testInvitationWithoutPendingInvitation() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.INVITATION);
        request.setCredential(crypto.stringToBytes(UUID.randomUUID().toString()));
        request.setNewCredential(crypto.stringToBytes(UUID.randomUUID().toString()));

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Account does not have an invitation pending.", response.getReturnMessage());
    }

    @Test
    void testInvitationWithoutAccount() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName("Who knows");
        request.setNewAccountName("wannabe");
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.INVITATION);
        request.setCredential(crypto.stringToBytes(UUID.randomUUID().toString()));
        request.setNewCredential(crypto.stringToBytes(UUID.randomUUID().toString()));

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Account does not exist.", response.getReturnMessage());
    }

    @Test
    void testInviteExistingAccount() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName(MEMBER_4);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.CONSTRAINT_ERROR.getCode(), response.getReturnCode());
        assertEquals("Cannot create an invitation, as the account already exists.", response.getReturnMessage());
    }

    @Test
    void testInvitingWithoutPermission() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("Members may only invite into a Circle which they administer.", response.getReturnMessage());
    }

    @Test
    void testLoginWithSession() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest loginRequest = prepareLoginRequest(MEMBER_1);
        final ProcessMemberResponse loginResponse = bean.processMember(loginRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), loginResponse.getReturnCode());

        // Just performing an action using the Session
        final ProcessCircleRequest circleRequest = prepareSessionRequest(ProcessCircleRequest.class, loginResponse.getSession());
        circleRequest.setAction(Action.UPDATE);
        circleRequest.setCircleId(CIRCLE_1_ID);
        circleRequest.setCircleName("new Circle1 name");
        final ProcessCircleResponse circleResponse = bean.processCircle(circleRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), circleResponse.getReturnCode());

        final ProcessMemberRequest logoutRequest = prepareLogoutRequest(loginResponse.getSession());
        final ProcessMemberResponse logoutResponse = bean.processMember(logoutRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), logoutResponse.getReturnCode());
    }

    @Test
    void testLogoutMissingSession() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareLogoutRequest(generateData(32));

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Session could be found.", response.getReturnMessage());
    }

    /**
     * Requests with a Session don't run PBKDF2 (PLAN.md, Crypto §2a), only the
     * login with the passphrase does.
     */
    @Test
    void testSessionRequestsRunNoPbkdf2() {
        final ManagementBean bean = prepareManagementBean();
        final long beforeLogin = Crypto.passwordKeyDerivations();
        final ProcessMemberResponse loginResponse = bean.processMember(prepareLoginRequest(MEMBER_1));
        assertEquals(ReturnCode.SUCCESS.getCode(), loginResponse.getReturnCode());
        assertEquals(beforeLogin + 1, Crypto.passwordKeyDerivations());

        final long beforeRequests = Crypto.passwordKeyDerivations();
        for (int i = 0; i < 3; i++) {
            final FetchMemberResponse response = bean.fetchMembers(prepareSessionRequest(FetchMemberRequest.class, loginResponse.getSession()));
            assertTrue(response.isOk(), response.getReturnMessage());
        }
        assertEquals(beforeRequests, Crypto.passwordKeyDerivations());
    }

    @Test
    void testSessionTokenIsOnlyStoredAsKeyedHash() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberResponse loginResponse = bean.processMember(prepareLoginRequest(MEMBER_1));
        final byte[] token = loginResponse.getSession();
        assertEquals(32, token.length);

        final SessionEntity entity = dao.findSessionByChecksum(crypto.hmacWithMasterKey("eds/session/lookup", token));
        final String armoredToken = Base64.getEncoder().encodeToString(token);
        assertNotNull(entity);
        assertEquals(MEMBER_1, entity.getMember().getName());
        assertNotEquals(armoredToken, entity.getChecksum());
        assertFalse(entity.getCrypto().contains(armoredToken));
    }

    @Test
    void testUnknownSessionIsRejected() {
        final ManagementBean bean = prepareManagementBean();
        final FetchMemberResponse response = bean.fetchMembers(prepareSessionRequest(FetchMemberRequest.class, generateData(32)));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Session could be found.", response.getReturnMessage());
    }

    @Test
    void testPasswordChangeEndsSession() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberResponse loginResponse = bean.processMember(prepareLoginRequest(MEMBER_1));
        assertTrue(loginResponse.isOk());

        final ProcessMemberRequest update = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        update.setAction(Action.UPDATE);
        update.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));
        assertTrue(bean.processMember(update).isOk());

        final FetchMemberResponse response = bean.fetchMembers(prepareSessionRequest(FetchMemberRequest.class, loginResponse.getSession()));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
    }

        @Test
    void testLogoutExpiredSession() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SESSION_TIMEOUT.getKey(), "-1");

        final ManagementBean bean = prepareManagementBean(mySettings);
        final ProcessMemberRequest loginRequest = prepareLoginRequest(MEMBER_2);
        final ProcessMemberResponse loginResponse = bean.processMember(loginRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), loginResponse.getReturnCode());

        final ProcessMemberRequest logoutRequest = prepareLogoutRequest(loginResponse.getSession());
        final ProcessMemberResponse logoutResponse = bean.processMember(logoutRequest);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), logoutResponse.getReturnCode());
        assertEquals("The Session has expired.", logoutResponse.getReturnMessage());
    }

    @Test
    void testProcessSelf() {
        final String newName = "Supreme Member";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewAccountName(newName);
        request.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + newName + "' was successfully updated.", response.getReturnMessage());
    }

    @Test
    void testProcessSelfChangeAccountNameToExisting() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewAccountName(MEMBER_2);
        request.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.CONSTRAINT_ERROR.getCode(), response.getReturnCode());
        assertEquals("The new Account Name already exists.", response.getReturnMessage());
    }

    /**
     * When a member is updating the passphrase, then it will result in a new
     * KeyPair generated internally. This means that the internal Keys must also
     * be updated for each Circle, which the Member belongs to. This test will
     * add some data, change the Passphrase, and verify that the data can be
     * read out both before and after the change.
     */
    @Test
    void testUpdatePassphraseWithDataVerification() {
        final ManagementBean bean = prepareManagementBean();
        final ShareBean shareBean = prepareShareBean();

        final String dataId = addData();
        final byte[] data1 = fetchData(dataId);

        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));
        final ProcessMemberResponse response = bean.processMember(request);
        assertTrue(response.isOk());

        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setCredential(crypto.stringToBytes(NEW_PASSPHRASE));
        fetchRequest.setDataId(dataId);

        final FetchDataResponse dataResponse = shareBean.fetchData(fetchRequest);
        assertTrue(dataResponse.isOk());
        final byte[] data2 = dataResponse.getData();
        assertArrayEquals(data2, data1);
    }

    @Test
    void testInvalidateSelf() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_4);
        request.setAction(Action.INVALIDATE);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + MEMBER_4 + "' has been Invalidated.", response.getReturnMessage());

        // The request credential has been overwritten with zeros, which must
        // not unlock the invalidated account.
        request.setAction(Action.UPDATE);
        request.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));
        final ProcessMemberResponse response2 = bean.processMember(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response2.getReturnCode());
    }

    @Test
    void testInvalidateAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVALIDATE);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("The System Administrator Account may not be invalidated.", response.getReturnMessage());
    }

    @Test
    void testDeleteMember() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setMemberId(MEMBER_2_ID);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member 'member2' has successfully been deleted.", response.getReturnMessage());
    }

    @Test
    void testDeleteMemberAsMember() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_5);
        request.setAction(Action.DELETE);
        request.setMemberId(MEMBER_3_ID);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("Members are not permitted to delete Accounts.", response.getReturnMessage());
    }

    @Test
    void testDeleteUnknownAccount() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        // Random MemberId, should not exist!
        request.setMemberId(UUID.randomUUID().toString());

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No such Account exist.", response.getReturnMessage());
    }

    @Test
    void testDeleteSelf() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_3);
        request.setAction(Action.DELETE);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + MEMBER_3 + "' has been successfully deleted.", response.getReturnMessage());
    }

    @Test
    void testDeleteAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setMemberId(ADMIN_ID);
        assertTrue(request.validate().isEmpty());

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("It is not permitted to delete yourself.", response.getReturnMessage());
    }

    @Test
    void testInvalidatedAccountCannotUseZeroedCredential() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_4);
        final int credentialLength = request.getCredential().length;
        request.setAction(Action.INVALIDATE);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());

        // The credential of the request was overwritten with zeros, which must
        // not have become the new password of the invalidated account.
        final FetchMemberRequest fetchRequest = prepareRequest(FetchMemberRequest.class, MEMBER_4);
        fetchRequest.setCredential(new byte[credentialLength]);
        final FetchMemberResponse fetchResponse = bean.fetchMembers(fetchRequest);
        assertFalse(fetchResponse.isOk());

        // Neither may the original credential be used any longer.
        final FetchMemberRequest originalRequest = prepareRequest(FetchMemberRequest.class, MEMBER_4);
        assertFalse(bean.fetchMembers(originalRequest).isOk());
    }

    @Test
    void testDeleteSelfAsSystemAdministrator() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("The System Administrator Account may not be deleted.", response.getReturnMessage());
        assertNotNull(dao.findMemberByName(Constants.ADMIN_ACCOUNT));
    }

    @Test
    void testDeleteSystemAdministratorAsOtherAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final String otherAdmin = createAdministrator(bean);

        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, otherAdmin);
        request.setAction(Action.DELETE);
        request.setMemberId(ADMIN_ID);

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("The System Administrator Account may not be deleted.", response.getReturnMessage());
        assertNotNull(dao.findMemberByName(Constants.ADMIN_ACCOUNT));
    }

    @Test
    void testAlterSystemAdministratorRoleAsOtherAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final String otherAdmin = createAdministrator(bean);

        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, otherAdmin);
        request.setAction(Action.ALTER);
        request.setMemberId(ADMIN_ID);
        request.setMemberRole(MemberRole.STANDARD);

        // Only the first System Administrator changes roles, so another
        // administrator is refused before it comes to whose role it is.
        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Only the first System Administrator may make, unmake or delete administrators.", response.getReturnMessage());
        assertEquals(MemberRole.ADMIN, dao.findMemberByName(Constants.ADMIN_ACCOUNT).getMemberRole());
    }

    @Test
    void testRenameSystemAdministrator() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.UPDATE);
        request.setNewAccountName("root");

        // The System Administrator is known by being the first account of
        // the stone, not by a name, and may change its name as others do.
        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode(), response.getReturnMessage());
        assertNull(dao.findMemberByName(Constants.ADMIN_ACCOUNT));
        assertEquals(MemberRole.ADMIN, dao.findMemberByName("root").getMemberRole());
        assertEquals(ADMIN_ID, dao.findMemberByName("root").getExternalId());
    }

    /**
     * With another name it is still the System Administrator: the first
     * account of the stone, which another administrator can neither take
     * the role from nor delete.
     */
    @Test
    void testRenamedSystemAdministratorIsStillProtected() {
        final ManagementBean bean = prepareManagementBean();
        final String otherAdmin = createAdministrator(bean);
        final ProcessMemberRequest rename = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        rename.setAction(Action.UPDATE);
        rename.setNewAccountName("root");
        assertTrue(bean.processMember(rename).isOk());

        final ProcessMemberRequest alter = prepareRequest(ProcessMemberRequest.class, otherAdmin);
        alter.setAction(Action.ALTER);
        alter.setMemberId(ADMIN_ID);
        alter.setMemberRole(MemberRole.STANDARD);
        final ProcessMemberResponse altered = bean.processMember(alter);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), altered.getReturnCode());
        assertEquals("Only the first System Administrator may make, unmake or delete administrators.", altered.getReturnMessage());

        final ProcessMemberRequest delete = prepareRequest(ProcessMemberRequest.class, otherAdmin);
        delete.setAction(Action.DELETE);
        delete.setMemberId(ADMIN_ID);
        final ProcessMemberResponse deleted = bean.processMember(delete);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), deleted.getReturnCode());
        assertEquals("The System Administrator Account may not be deleted.", deleted.getReturnMessage());
        assertEquals(MemberRole.ADMIN, dao.findMemberByName("root").getMemberRole());
    }

    @Test
    void testRenameMemberToSystemAdministrator() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewAccountName("ADMIN");

        // No name is reserved; this one is taken, as the System
        // Administrator of the tests has it.
        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.CONSTRAINT_ERROR.getCode(), response.getReturnCode());
        assertEquals("The new Account Name already exists.", response.getReturnMessage());
        assertNotNull(dao.findMemberByName(MEMBER_1));
    }

    // =========================================================================
    // Entropy Verification of new Credentials
    // =========================================================================

    private static final String WEAK_CREDENTIAL_MESSAGE = "The Credential is too weak. Use at least 20 random characters (letters, digits and symbols), or a passphrase of several random words.";

    @Test
    void testCreateMemberWithWeakCredential() {
        final String account = "weak member";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes("Password123!"));

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals(WEAK_CREDENTIAL_MESSAGE, response.getReturnMessage());
        assertNull(dao.findMemberByName(account));
    }

    @Test
    void testCreateMemberWithAccountNameAsCredential() {
        final String account = "a rather long account name";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals(WEAK_CREDENTIAL_MESSAGE, response.getReturnMessage());
        assertNull(dao.findMemberByName(account));
    }

    @Test
    void testUpdateSelfWithWeakCredential() {
        final String newName = "Renamed Member";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewAccountName(newName);
        request.setNewCredential(crypto.stringToBytes("qwertzuiop123456"));

        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals(WEAK_CREDENTIAL_MESSAGE, response.getReturnMessage());

        // Nothing may have been changed, so the old Credential still works.
        assertNull(dao.findMemberByName(newName));
        final FetchMemberRequest fetchRequest = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        assertTrue(bean.fetchMembers(fetchRequest).isOk());
    }

    @Test
    void testInvitationWithWeakCredential() {
        final String account = "weak invitee";
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName(account);
        final ProcessMemberResponse response = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());

        final ProcessMemberRequest invitationRequest = new ProcessMemberRequest();
        invitationRequest.setAccountName(account);
        invitationRequest.setAction(Action.UPDATE);
        invitationRequest.setCredentialType(CredentialType.INVITATION);
        invitationRequest.setCredential(response.getInvitation());
        invitationRequest.setNewCredential(crypto.stringToBytes("weak invitee"));

        final ProcessMemberResponse invitationResponse = bean.processMember(invitationRequest);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), invitationResponse.getReturnCode());
        assertEquals(WEAK_CREDENTIAL_MESSAGE, invitationResponse.getReturnMessage());
        assertEquals("INVITATION", dao.findMemberByName(account).getPrivateKey());
    }

    @Test
    void testCreateSystemAdministratorWithWeakCredential() {
        for (final MemberEntity entity : dao.findAllAscending(MemberEntity.class, "id")) {
            dao.delete(entity);
        }

        final ManagementBean bean = prepareManagementBean();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));

        final FetchMemberResponse response = bean.fetchMembers(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals(WEAK_CREDENTIAL_MESSAGE, response.getReturnMessage());
        assertNull(dao.findMemberByName(Constants.ADMIN_ACCOUNT));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private String createAdministrator(final ManagementBean bean) {
        final String account = "second admin";
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(credentialOf(account));
        request.setMemberRole(MemberRole.ADMIN);
        assertTrue(bean.processMember(request).isOk());

        return account;
    }

    private static ProcessMemberRequest prepareLoginRequest(final String accountName) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, accountName);
        request.setAction(Action.LOGIN);

        return request;
    }

    private static ProcessMemberRequest prepareLogoutRequest(final byte[] session) {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setCredential(Arrays.copyOf(session, session.length));
        request.setCredentialType(CredentialType.SESSION);
        request.setAction(Action.LOGOUT);

        return request;
    }

    private String addData() {
        final ShareBean bean = prepareShareBean();

        final ProcessDataRequest dataRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        dataRequest.setAction(Action.ADD);
        dataRequest.setCircleId(CIRCLE_1_ID);
        dataRequest.setDataName(UUID.randomUUID().toString());
        dataRequest.setData(generateData(LARGE_SIZE_BYTES));

        final ProcessDataResponse response = bean.processData(dataRequest);
        assertTrue(response.isOk());
        return response.getDataId();
    }

    private byte[] fetchData(final String dataId) {
        final ShareBean bean = prepareShareBean();
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_1);
        request.setDataId(dataId);

        final FetchDataResponse response = bean.fetchData(request);
        assertTrue(response.isOk());
        return response.getData();
    }
}
