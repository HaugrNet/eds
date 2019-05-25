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
import io.javadog.cws.core.model.Settings;
import java.util.Base64;
import java.util.UUID;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ProcessMemberServiceTest extends DatabaseSetup {

    @Test
    public void testNullRequest() {
        prepareCause(ReturnCode.VERIFICATION_WARNING, "Cannot Process a NULL Object.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = null;
        assertThat(service, is(not(nullValue())));

        service.perform(request);
    }

    @Test
    public void testAddingWithoutRole() {
        final String account = "Created Member";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));
        final ProcessMemberResponse response = service.perform(request);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testAddingWithAdminRole() {
        final String account = "Created Member";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));
        request.setMemberRole(MemberRole.ADMIN);
        final ProcessMemberResponse response = service.perform(request);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testAddingWithStandardRole() {
        final String account = "Created Member";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));
        request.setMemberRole(MemberRole.STANDARD);
        final ProcessMemberResponse response = service.perform(request);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testAddingAsMember() {
        prepareCause(ReturnCode.AUTHORIZATION_WARNING, "Members are not permitted to create new Accounts.");

        final String account = "Member Added Member";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));
        request.setMemberRole(MemberRole.STANDARD);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAddingWithPublicKey() {
        final String account = "Member with PublicKey";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));
        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));

        final ProcessMemberRequest updateRequest = prepareRequest(ProcessMemberRequest.class, account);
        updateRequest.setAction(Action.UPDATE);
        updateRequest.setPublicKey(UUID.randomUUID().toString());
        final ProcessMemberResponse updateResponse = service.perform(updateRequest);
        assertThat(updateResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(updateResponse.getReturnMessage(), is("Ok"));

        final FetchMemberService fetchService = new FetchMemberService(settings, entityManager);
        final FetchMemberRequest fetchRequest = prepareRequest(FetchMemberRequest.class, MEMBER_4);
        fetchRequest.setMemberId(response.getMemberId());
        final FetchMemberResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchResponse.getReturnMessage(), is("Ok"));
        assertThat(fetchResponse.getMembers().size(), is(1));
        assertThat(fetchResponse.getMembers().get(0).getPublicKey(), is(updateRequest.getPublicKey()));
    }

    @Test
    public void testAddingWithExistingAccountName() {
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "An Account with the requested AccountName already exist.");

        final String account = MEMBER_4;
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(crypto.stringToBytes(account));
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAlterAccount() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ALTER);
        request.setMemberId(MEMBER_1_ID);
        request.setMemberRole(MemberRole.ADMIN);
        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testAlterSelf() {
        prepareCause(ReturnCode.ILLEGAL_ACTION, "It is not permitted to alter own account.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ALTER);
        request.setMemberId(ADMIN_ID);
        request.setMemberRole(MemberRole.STANDARD);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAlterAccountAsMember() {
        prepareCause(ReturnCode.AUTHORIZATION_WARNING, "Only Administrators may update the Role of a member.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_3);
        request.setAction(Action.ALTER);
        request.setMemberId(MEMBER_1_ID);
        request.setMemberRole(MemberRole.ADMIN);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testProcessingSelf() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_5);
        request.setAction(Action.UPDATE);
        request.setMemberId(MEMBER_5_ID);
        request.setNewAccountName(null);
        request.setNewCredential(null);

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testProcessSelfPasswordUpdateWithSession() {
        prepareCause(ReturnCode.VERIFICATION_WARNING, "It is only permitted to update the credentials when authenticating with Passphrase.");

        final String session = UUID.randomUUID().toString();
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest loginRequest = prepareLoginRequest(MEMBER_5, session);
        final ProcessMemberResponse loginResponse = service.perform(loginRequest);
        assertThat(loginResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(loginResponse.getReturnMessage(), is("Ok"));

        final ProcessMemberRequest passwordRequest = prepareSessionRequest(ProcessMemberRequest.class, session);
        passwordRequest.setAction(Action.UPDATE);
        passwordRequest.setNewCredential(loginRequest.getNewCredential());
        service.perform(passwordRequest);
    }

    @Test
    public void testInvitation() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        final byte[] signature = response.getSignature();

        final ProcessMemberRequest invitationRequest = new ProcessMemberRequest();
        invitationRequest.setAccountName("invitee");
        invitationRequest.setAction(Action.UPDATE);
        invitationRequest.setCredentialType(CredentialType.SIGNATURE);
        invitationRequest.setCredential(signature);
        invitationRequest.setNewCredential(crypto.stringToBytes("New Passphrase"));
        final ProcessMemberResponse invitationResponse = service.perform(invitationRequest);
        assertThat(invitationResponse, is(not(nullValue())));
        assertThat(invitationResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testNullNewCredentialForInvitation() {
        prepareCause(ReturnCode.VERIFICATION_WARNING, "The newCredential is missing in Request.");
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName("null Invitee");
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.SIGNATURE);
        request.setCredential(crypto.stringToBytes("Signature"));
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testEmptyNewCredentialForInvitation() {
        prepareCause(ReturnCode.VERIFICATION_WARNING, "The newCredential is missing in Request.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName("empty Invitee");
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.SIGNATURE);
        request.setCredential(crypto.stringToBytes("Signature"));
        request.setNewCredential(crypto.stringToBytes(""));
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testInvitationWithInvalidSignature() {
        prepareCause(ReturnCode.AUTHENTICATION_WARNING, "The given signature is invalid.");

        final String bogusSignature = "T+OoZiBpm36P868XUZYWFsW1jUFlD31x+FeQuDjcm4DmmIk+qWd8KuUzLdnETRPIxo/OuYLcpvFiPxMf0v78feiw/yVVV5+1xjO+FR/KYgB4JTaJ6p0RIEpS3rjs27bY+1OYclsk4MPRKbxZN06ZFHlSY4btk1G4ML7x0/iUCLBbOO2y3S4JZpKwAR7kAyhVeqyi8qKi13o+7z/J0KP2EhHrF8+2y3z63TKLyClZRrAhvy3/g/k0q7MccFOKDGsxxIpe2jfOHtxLEYfbgrdly/fZHEQL5vbbf/LbQ7MISfcwXSLtJMD0COXsm/V1nkmI/ficjskvNuUj+h739KEmuQ==";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        final ProcessMemberRequest invitationRequest = new ProcessMemberRequest();
        invitationRequest.setAccountName("invitee");
        invitationRequest.setAction(Action.UPDATE);
        invitationRequest.setCredentialType(CredentialType.SIGNATURE);
        invitationRequest.setCredential(Base64.getDecoder().decode(bogusSignature));
        invitationRequest.setNewCredential(crypto.stringToBytes(UUID.randomUUID().toString()));

        service.perform(invitationRequest);
    }

    @Test
    public void testInvitationWithInvalidSignature2() {
        prepareCause(ReturnCode.CRYPTO_ERROR, "Signature length not correct: got 36 but was expecting 256");
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        final ProcessMemberRequest invitationRequest = new ProcessMemberRequest();
        invitationRequest.setAccountName("invitee");
        invitationRequest.setAction(Action.UPDATE);
        invitationRequest.setCredentialType(CredentialType.SIGNATURE);
        invitationRequest.setCredential(crypto.stringToBytes(UUID.randomUUID().toString()));
        invitationRequest.setNewCredential(crypto.stringToBytes(UUID.randomUUID().toString()));

        service.perform(invitationRequest);
    }

    @Test
    public void testInvitationWithoutPendingInvitation() {
        prepareCause(ReturnCode.VERIFICATION_WARNING, "Account does not have an invitation pending.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.SIGNATURE);
        request.setCredential(crypto.stringToBytes(UUID.randomUUID().toString()));
        request.setNewCredential(crypto.stringToBytes(UUID.randomUUID().toString()));
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testInvitationWithoutAccount() {
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "Account does not exist.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName("Who knows");
        request.setNewAccountName("wannabe");
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.SIGNATURE);
        request.setCredential(crypto.stringToBytes(UUID.randomUUID().toString()));
        request.setNewCredential(crypto.stringToBytes(UUID.randomUUID().toString()));
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testInviteExistingAccount() {
        prepareCause(ReturnCode.CONSTRAINT_ERROR, "Cannot create an invitation, as the account already exists.");
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName(MEMBER_4);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testInvitingWithoutPermission() {
        prepareCause(ReturnCode.ILLEGAL_ACTION, "Members are not permitted to invite new Members.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testLoginWithSession() {
        final String sessionKey = "sessionKey";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest loginRequest = prepareLoginRequest(MEMBER_1, sessionKey);
        final ProcessMemberResponse loginResponse = service.perform(loginRequest);
        assertThat(loginResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        // Just performing an action using the Session
        final ProcessCircleService circleService = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest circleRequest = prepareSessionRequest(ProcessCircleRequest.class, sessionKey);
        circleRequest.setAction(Action.UPDATE);
        circleRequest.setCircleId(CIRCLE_1_ID);
        circleRequest.setCircleName("new Circle1 name");
        final ProcessCircleResponse circleResponse = circleService.perform(circleRequest);
        assertThat(circleResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        // Have to generate the SessionKey a second time, since the first request will override it.
        final ProcessMemberRequest logoutRequest = prepareLogoutRequest(sessionKey);
        final ProcessMemberResponse logoutResponse = service.perform(logoutRequest);
        assertThat(logoutResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
    }

    @Test
    public void testLogoutMissingSession() {
        prepareCause(ReturnCode.AUTHENTICATION_WARNING, "No Session could be found.");

        final String sessionKey = UUID.randomUUID().toString();
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareLogoutRequest(sessionKey);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testLogoutExpiredSession() {
        prepareCause(ReturnCode.AUTHENTICATION_WARNING, "The Session has expired.");

        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SESSION_TIMEOUT.getKey(), "-1");
        final String sessionKey = UUID.randomUUID().toString();

        final ProcessMemberService service = new ProcessMemberService(mySettings, entityManager);
        final ProcessMemberRequest loginRequest = prepareLoginRequest(MEMBER_2, sessionKey);
        final ProcessMemberResponse loginResponse = service.perform(loginRequest);
        assertThat(loginResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        final ProcessMemberRequest logoutRequest = prepareLogoutRequest(sessionKey);
        service.perform(logoutRequest);
    }

    @Test
    public void testProcessSelf() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewAccountName("Supreme Member");
        request.setNewCredential(crypto.stringToBytes("Bla bla bla"));

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testProcessSelfChangeAccountNameToExisting() {
        prepareCause(ReturnCode.CONSTRAINT_ERROR, "The new Account Name already exists.");
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewAccountName(MEMBER_2);
        request.setNewCredential(crypto.stringToBytes("Bla bla bla"));
        assertThat(request.validate().size(), is(0));

        service.perform(request);
    }

    /**
     * When a member is updating the passphrase, then it will result in a new
     * KeyPair generated internally, this means that the internal Keys must also
     * be updated for each Circle, which the Member belongs to. This test will
     * add some data, change the Passphrase and verify that the data can be
     * read out both before and after the change.
     */
    @Test
    public void testUpdatePassphraseWithDataVerification() {
        final String dataId = addData();
        final byte[] data1 = fetchData(dataId);

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewCredential(crypto.stringToBytes("My new Passphrase"));
        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));

        final FetchDataService fetchService = new FetchDataService(settings, entityManager);
        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setCredential(crypto.stringToBytes("My new Passphrase"));
        fetchRequest.setDataId(dataId);

        final FetchDataResponse dataResponse = fetchService.perform(fetchRequest);
        assertThat(dataResponse.isOk(), is(true));
        final byte[] data2 = dataResponse.getData();
        assertThat(data1, is(data2));
    }

    @Test
    public void testInvalidateSelf() {
        // Note, that the default error message between Java 8 & Java 11 has changed.
        //   Java  8: Decryption error
        //   Java 11: Message is larger than modulus
        prepareCause(ReturnCode.CRYPTO_ERROR, "");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_4);
        request.setAction(Action.INVALIDATE);

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Account has been Invalidated."));

        request.setAction(Action.UPDATE);
        request.setNewCredential(crypto.stringToBytes("New Passphrase"));
        service.perform(request);
    }

    @Test
    public void testInvalidateAdmin() {
        prepareCause(ReturnCode.ILLEGAL_ACTION, "The System Administrator Account may not be invalidated.");
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVALIDATE);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testDeleteMember() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setMemberId(MEMBER_2_ID);

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("The Member 'member2' has successfully been deleted."));
    }

    @Test
    public void testDeleteMemberAsMember() {
        prepareCause(ReturnCode.ILLEGAL_ACTION, "Members are not permitted to delete Accounts.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_5);
        request.setAction(Action.DELETE);
        request.setMemberId(MEMBER_3_ID);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testDeleteUnknownAccount() {
        prepareCause(ReturnCode.IDENTIFICATION_WARNING, "No such Account exist.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        // Random MemberId, should not exist!
        request.setMemberId(UUID.randomUUID().toString());
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testDeleteSelf() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_3);
        request.setAction(Action.DELETE);

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("The Member '" + MEMBER_3 + "' has been successfully deleted."));
    }

    @Test
    public void testDeleteAdmin() {
        prepareCause(ReturnCode.ILLEGAL_ACTION, "It is not permitted to delete yourself.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setMemberId(ADMIN_ID);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
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
        assertThat(response.isOk(), is(true));
        return response.getDataId();
    }

    private byte[] fetchData(final String dataId) {
        final FetchDataService service = new FetchDataService(settings, entityManager);
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_1);
        request.setDataId(dataId);

        final FetchDataResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));
        return response.getData();
    }
}
