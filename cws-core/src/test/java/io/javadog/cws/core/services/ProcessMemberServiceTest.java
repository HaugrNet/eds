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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class ProcessMemberServiceTest extends DatabaseSetup {

    @Test
    void testNullRequest() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(null));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Cannot Process a NULL Object.", cause.getMessage());
    }

    @Test
    void testAddingWithoutRole() {
        final String account = "Created Member";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));
        final ProcessMemberResponse response = service.perform(request);

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + account + "' was successfully added to CWS.", response.getReturnMessage());
    }

    @Test
    void testAddingWithAdminRole() {
        final String account = "Created Member";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));
        request.setMemberRole(MemberRole.ADMIN);
        final ProcessMemberResponse response = service.perform(request);

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + account + "' was successfully added to CWS.", response.getReturnMessage());
    }

    @Test
    void testAddingWithStandardRole() {
        final String account = "Created Member";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));
        request.setMemberRole(MemberRole.STANDARD);
        final ProcessMemberResponse response = service.perform(request);

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + account + "' was successfully added to CWS.", response.getReturnMessage());
    }

    @Test
    void testAddingAsMember() {
        final String account = "Member Added Member";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));
        request.setMemberRole(MemberRole.STANDARD);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("Members are not permitted to create new Accounts.", cause.getMessage());
    }

    @Test
    void testAddingWithPublicKey() {
        final String account = "Member with PublicKey";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));
        final ProcessMemberResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + account + "' was successfully added to CWS.", response.getReturnMessage());

        final ProcessMemberRequest updateRequest = prepareRequest(ProcessMemberRequest.class, account);
        updateRequest.setAction(Action.UPDATE);
        updateRequest.setPublicKey(UUID.randomUUID().toString());
        final ProcessMemberResponse updateResponse = service.perform(updateRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());
        assertEquals("The Member '" + account + "' was successfully updated.", updateResponse.getReturnMessage());

        final FetchMemberService fetchService = new FetchMemberService(settings, entityManager);
        final FetchMemberRequest fetchRequest = prepareRequest(FetchMemberRequest.class, MEMBER_4);
        fetchRequest.setMemberId(response.getMemberId());
        final FetchMemberResponse fetchResponse = fetchService.perform(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals("Ok", fetchResponse.getReturnMessage());
        assertEquals(1, fetchResponse.getMembers().size());
        assertEquals(updateRequest.getPublicKey(), fetchResponse.getMembers().get(0).getPublicKey());
    }

    @Test
    void testAddingWithExistingAccountName() {
        final String account = MEMBER_4;
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("An Account with the requested AccountName already exist.", cause.getMessage());
    }

    @Test
    void testAlterAccount() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ALTER);
        request.setMemberId(MEMBER_1_ID);
        request.setMemberRole(MemberRole.ADMIN);

        final ProcessMemberResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + MEMBER_1 + "' has successfully been given the new role '" + MemberRole.ADMIN + "'.", response.getReturnMessage());
    }

    @Test
    void testAlterSelf() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ALTER);
        request.setMemberId(ADMIN_ID);
        request.setMemberRole(MemberRole.STANDARD);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.ILLEGAL_ACTION, cause.getReturnCode());
        assertEquals("It is not permitted to alter own account.", cause.getMessage());
    }

    @Test
    void testAlterAccountAsMember() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_3);
        request.setAction(Action.ALTER);
        request.setMemberId(MEMBER_1_ID);
        request.setMemberRole(MemberRole.ADMIN);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("Only Administrators may update the Role of a member.", cause.getMessage());
    }

    @Test
    void testProcessingSelf() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_5);
        request.setAction(Action.UPDATE);
        request.setMemberId(MEMBER_5_ID);
        request.setNewAccountName(null);
        request.setNewCredential(null);

        final ProcessMemberResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + MEMBER_5 + "' was successfully updated.", response.getReturnMessage());
    }

    @Test
    void testProcessSelfPasswordUpdateWithSession() {
        final String session = UUID.randomUUID().toString();
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest loginRequest = prepareLoginRequest(MEMBER_5, session);
        final ProcessMemberResponse loginResponse = service.perform(loginRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), loginResponse.getReturnCode());
        assertEquals("The Member '" + MEMBER_5 + "' has successfully logged in.", loginResponse.getReturnMessage());

        final ProcessMemberRequest passwordRequest = prepareSessionRequest(ProcessMemberRequest.class, session);
        passwordRequest.setAction(Action.UPDATE);
        passwordRequest.setNewCredential(loginRequest.getNewCredential());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(passwordRequest));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("It is only permitted to update the credentials when authenticating with Passphrase.", cause.getMessage());
    }

    @Test
    void testInvitation() {
        final String account = "invitee";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName(account);

        final ProcessMemberResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertNotNull(response.getMemberId());
        final byte[] signature = response.getSignature();

        final ProcessMemberRequest invitationRequest = new ProcessMemberRequest();
        invitationRequest.setAccountName(account);
        invitationRequest.setAction(Action.UPDATE);
        invitationRequest.setCredentialType(CredentialType.SIGNATURE);
        invitationRequest.setCredential(signature);
        invitationRequest.setNewCredential(crypto.stringToBytes("New Passphrase"));

        final ProcessMemberResponse invitationResponse = service.perform(invitationRequest);
        assertNotNull(invitationResponse);
        assertEquals(ReturnCode.SUCCESS.getCode(), invitationResponse.getReturnCode());
        assertEquals("An invitation was successfully issued for '" + account + "'.", response.getReturnMessage());
    }

    @Test
    void testNullNewCredentialForInvitation() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName("null Invitee");
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.SIGNATURE);
        request.setCredential(crypto.stringToBytes("Signature"));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The newCredential is missing in Request.", cause.getMessage());
    }

    @Test
    void testEmptyNewCredentialForInvitation() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName("empty Invitee");
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.SIGNATURE);
        request.setCredential(crypto.stringToBytes("Signature"));
        request.setNewCredential(crypto.stringToBytes(""));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The newCredential is missing in Request.", cause.getMessage());
    }

    @Test
    void testInvitationWithInvalidSignature() {
        final String bogusSignature = "T+OoZiBpm36P868XUZYWFsW1jUFlD31x+FeQuDjcm4DmmIk+qWd8KuUzLdnETRPIxo/OuYLcpvFiPxMf0v78feiw/yVVV5+1xjO+FR/KYgB4JTaJ6p0RIEpS3rjs27bY+1OYclsk4MPRKbxZN06ZFHlSY4btk1G4ML7x0/iUCLBbOO2y3S4JZpKwAR7kAyhVeqyi8qKi13o+7z/J0KP2EhHrF8+2y3z63TKLyClZRrAhvy3/g/k0q7MccFOKDGsxxIpe2jfOHtxLEYfbgrdly/fZHEQL5vbbf/LbQ7MISfcwXSLtJMD0COXsm/V1nkmI/ficjskvNuUj+h739KEmuQ==";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final ProcessMemberResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());

        final ProcessMemberRequest invitationRequest = new ProcessMemberRequest();
        invitationRequest.setAccountName("invitee");
        invitationRequest.setAction(Action.UPDATE);
        invitationRequest.setCredentialType(CredentialType.SIGNATURE);
        invitationRequest.setCredential(Base64.getDecoder().decode(bogusSignature));
        invitationRequest.setNewCredential(crypto.stringToBytes(UUID.randomUUID().toString()));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(invitationRequest));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING, cause.getReturnCode());
        assertEquals("The given signature is invalid.", cause.getMessage());
    }

    @Test
    void testInvitationWithInvalidSignature2() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final ProcessMemberResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());

        final ProcessMemberRequest invitationRequest = new ProcessMemberRequest();
        invitationRequest.setAccountName("invitee");
        invitationRequest.setAction(Action.UPDATE);
        invitationRequest.setCredentialType(CredentialType.SIGNATURE);
        invitationRequest.setCredential(crypto.stringToBytes(UUID.randomUUID().toString()));
        invitationRequest.setNewCredential(crypto.stringToBytes(UUID.randomUUID().toString()));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(invitationRequest));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
        assertEquals("Signature length not correct: got 36 but was expecting 256", cause.getMessage());
    }

    @Test
    void testInvitationWithoutPendingInvitation() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.SIGNATURE);
        request.setCredential(crypto.stringToBytes(UUID.randomUUID().toString()));
        request.setNewCredential(crypto.stringToBytes(UUID.randomUUID().toString()));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Account does not have an invitation pending.", cause.getMessage());
    }

    @Test
    void testInvitationWithoutAccount() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName("Who knows");
        request.setNewAccountName("wannabe");
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.SIGNATURE);
        request.setCredential(crypto.stringToBytes(UUID.randomUUID().toString()));
        request.setNewCredential(crypto.stringToBytes(UUID.randomUUID().toString()));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Account does not exist.", cause.getMessage());
    }

    @Test
    void testInviteExistingAccount() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName(MEMBER_4);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.CONSTRAINT_ERROR, cause.getReturnCode());
        assertEquals("Cannot create an invitation, as the account already exists.", cause.getMessage());
    }

    @Test
    void testInvitingWithoutPermission() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.ILLEGAL_ACTION, cause.getReturnCode());
        assertEquals("Members are not permitted to invite new Members.", cause.getMessage());
    }

    @Test
    void testLoginWithSession() {
        final String sessionKey = "sessionKey";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest loginRequest = prepareLoginRequest(MEMBER_1, sessionKey);
        final ProcessMemberResponse loginResponse = service.perform(loginRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), loginResponse.getReturnCode());

        // Just performing an action using the Session
        final ProcessCircleService circleService = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest circleRequest = prepareSessionRequest(ProcessCircleRequest.class, sessionKey);
        circleRequest.setAction(Action.UPDATE);
        circleRequest.setCircleId(CIRCLE_1_ID);
        circleRequest.setCircleName("new Circle1 name");
        final ProcessCircleResponse circleResponse = circleService.perform(circleRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), circleResponse.getReturnCode());

        // Have to generate the SessionKey a second time, since the first request will override it.
        final ProcessMemberRequest logoutRequest = prepareLogoutRequest(sessionKey);
        final ProcessMemberResponse logoutResponse = service.perform(logoutRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), logoutResponse.getReturnCode());
    }

    @Test
    void testLogoutMissingSession() {
        final String sessionKey = UUID.randomUUID().toString();
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareLogoutRequest(sessionKey);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING, cause.getReturnCode());
        assertEquals("No Session could be found.", cause.getMessage());
    }

    @Test
    void testLogoutExpiredSession() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SESSION_TIMEOUT.getKey(), "-1");
        final String sessionKey = UUID.randomUUID().toString();

        final ProcessMemberService service = new ProcessMemberService(mySettings, entityManager);
        final ProcessMemberRequest loginRequest = prepareLoginRequest(MEMBER_2, sessionKey);
        final ProcessMemberResponse loginResponse = service.perform(loginRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), loginResponse.getReturnCode());

        final ProcessMemberRequest logoutRequest = prepareLogoutRequest(sessionKey);
        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(logoutRequest));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING, cause.getReturnCode());
        assertEquals("The Session has expired.", cause.getMessage());
    }

    @Test
    void testProcessSelf() {
        final String newName = "Supreme Member";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewAccountName(newName);
        request.setNewCredential(crypto.stringToBytes("Bla bla bla"));

        final ProcessMemberResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + newName + "' was successfully updated.", response.getReturnMessage());
    }

    @Test
    void testProcessSelfChangeAccountNameToExisting() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewAccountName(MEMBER_2);
        request.setNewCredential(crypto.stringToBytes("Bla bla bla"));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.CONSTRAINT_ERROR, cause.getReturnCode());
        assertEquals("The new Account Name already exists.", cause.getMessage());
    }

    /**
     * When a member is updating the passphrase, then it will result in a new
     * KeyPair generated internally, this means that the internal Keys must also
     * be updated for each Circle, which the Member belongs to. This test will
     * add some data, change the Passphrase and verify that the data can be
     * read out both before and after the change.
     */
    @Test
    void testUpdatePassphraseWithDataVerification() {
        final String dataId = addData();
        final byte[] data1 = fetchData(dataId);

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewCredential(crypto.stringToBytes("My new Passphrase"));
        final ProcessMemberResponse response = service.perform(request);
        assertTrue(response.isOk());

        final FetchDataService fetchService = new FetchDataService(settings, entityManager);
        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setCredential(crypto.stringToBytes("My new Passphrase"));
        fetchRequest.setDataId(dataId);

        final FetchDataResponse dataResponse = fetchService.perform(fetchRequest);
        assertTrue(dataResponse.isOk());
        final byte[] data2 = dataResponse.getData();
        assertArrayEquals(data2, data1);
    }

    @Test
    void testInvalidateSelf() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_4);
        request.setAction(Action.INVALIDATE);

        final ProcessMemberResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + MEMBER_4 + "' has been Invalidated.", response.getReturnMessage());

        request.setAction(Action.UPDATE);
        request.setNewCredential(crypto.stringToBytes("New Passphrase"));
        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.CRYPTO_ERROR, cause.getReturnCode());
    }

    @Test
    void testInvalidateAdmin() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVALIDATE);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.ILLEGAL_ACTION, cause.getReturnCode());
        assertEquals("The System Administrator Account may not be invalidated.", cause.getMessage());
    }

    @Test
    void testDeleteMember() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setMemberId(MEMBER_2_ID);

        final ProcessMemberResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member 'member2' has successfully been deleted.", response.getReturnMessage());
    }

    @Test
    void testDeleteMemberAsMember() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_5);
        request.setAction(Action.DELETE);
        request.setMemberId(MEMBER_3_ID);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.ILLEGAL_ACTION, cause.getReturnCode());
        assertEquals("Members are not permitted to delete Accounts.", cause.getMessage());
    }

    @Test
    void testDeleteUnknownAccount() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        // Random MemberId, should not exist!
        request.setMemberId(UUID.randomUUID().toString());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No such Account exist.", cause.getMessage());
    }

    @Test
    void testDeleteSelf() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_3);
        request.setAction(Action.DELETE);

        final ProcessMemberResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + MEMBER_3 + "' has been successfully deleted.", response.getReturnMessage());
    }

    @Test
    void testDeleteAdmin() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setMemberId(ADMIN_ID);
        assertTrue(request.validate().isEmpty());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.ILLEGAL_ACTION, cause.getReturnCode());
        assertEquals("It is not permitted to delete yourself.", cause.getMessage());
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private ProcessMemberRequest prepareLoginRequest(final String accountName, final String sessionKey) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, accountName);
        request.setNewCredential(crypto.stringToBytes(sessionKey));
        request.setAction(Action.LOGIN);

        return request;
    }

    private ProcessMemberRequest prepareLogoutRequest(final String sessionKey) {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setCredential(crypto.stringToBytes(sessionKey));
        request.setCredentialType(CredentialType.SESSION);
        request.setAction(Action.LOGOUT);

        return request;
    }

    private String addData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);

        final ProcessDataRequest dataRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        dataRequest.setAction(Action.ADD);
        dataRequest.setCircleId(CIRCLE_1_ID);
        dataRequest.setDataName(UUID.randomUUID().toString());
        dataRequest.setData(generateData(1048576));

        final ProcessDataResponse response = service.perform(dataRequest);
        assertTrue(response.isOk());
        return response.getDataId();
    }

    private byte[] fetchData(final String dataId) {
        final FetchDataService service = new FetchDataService(settings, entityManager);
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_1);
        request.setDataId(dataId);

        final FetchDataResponse response = service.perform(request);
        assertTrue(response.isOk());
        return response.getData();
    }
}
