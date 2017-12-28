/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
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
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.exceptions.AuthenticationException;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.exceptions.CryptoException;
import org.junit.Test;

import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberServiceTest extends DatabaseSetup {

    @Test
    public void testNullRequest() {
        prepareCause(CWSException.class, ReturnCode.VERIFICATION_WARNING, "Cannot Process a NULL Object.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = null;
        assertThat(service, is(not(nullValue())));

        service.perform(request);
    }

    @Test
    public void testAdding() {
        final String account = "Created Member";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(account);
        final ProcessMemberResponse response = service.perform(request);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testAddingWithExistingAccountName() {
        prepareCause(CWSException.class, ReturnCode.CONSTRAINT_ERROR, "An Account with the same AccountName already exist.");

        final String account = MEMBER_4;
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(account);
        request.setNewCredential(account);
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
        assertThat(response.isOk(), is(true));
    }

    @Test
    public void testInvitation() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        final String signature = response.getSignature();

        final ProcessMemberRequest invationRequest = new ProcessMemberRequest();
        invationRequest.setAccountName("invitee");
        invationRequest.setCredentialType(CredentialType.SIGNATURE);
        invationRequest.setCredential(signature);
        final ProcessMemberResponse invitationResponse = service.perform(invationRequest);
        assertThat(invitationResponse, is(not(nullValue())));
        assertThat(invitationResponse.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testInvitationWithInvalidSignature() {
        final String bogusSignature = "T+OoZiBpm36P868XUZYWFsW1jUFlD31x+FeQuDjcm4DmmIk+qWd8KuUzLdnETRPIxo/OuYLcpvFiPxMf0v78feiw/yVVV5+1xjO+FR/KYgB4JTaJ6p0RIEpS3rjs27bY+1OYclsk4MPRKbxZN06ZFHlSY4btk1G4ML7x0/iUCLBbOO2y3S4JZpKwAR7kAyhVeqyi8qKi13o+7z/J0KP2EhHrF8+2y3z63TKLyClZRrAhvy3/g/k0q7MccFOKDGsxxIpe2jfOHtxLEYfbgrdly/fZHEQL5vbbf/LbQ7MISfcwXSLtJMD0COXsm/V1nkmI/ficjskvNuUj+h739KEmuQ==";
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));

        final ProcessMemberRequest invationRequest = new ProcessMemberRequest();
        invationRequest.setAccountName("invitee");
        invationRequest.setCredentialType(CredentialType.SIGNATURE);
        invationRequest.setCredential(bogusSignature);
        final ProcessMemberResponse invitationResponse = service.perform(invationRequest);
        assertThat(invitationResponse.getReturnCode(), is(ReturnCode.AUTHENTICATION_WARNING));
        assertThat(invitationResponse.getReturnMessage(), is("The given signature is invalid."));
    }

    @Test
    public void testInvitationWithInvalidSignature2() {
        prepareCause(CryptoException.class, ReturnCode.CRYPTO_ERROR, "Illegal base64 character 2d");
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));

        final ProcessMemberRequest invationRequest = new ProcessMemberRequest();
        invationRequest.setAccountName("invitee");
        invationRequest.setCredentialType(CredentialType.SIGNATURE);
        invationRequest.setCredential(UUID.randomUUID().toString());

        service.perform(invationRequest);
    }

    @Test
    public void testInvitationWithoutPendingInvitation() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(MEMBER_1);
        request.setCredentialType(CredentialType.SIGNATURE);
        request.setCredential(UUID.randomUUID().toString());

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("Account does not have an invitation pending."));
    }

    @Test
    public void testInvitationWithoutAccount() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setNewAccountName("wannabe");
        request.setCredentialType(CredentialType.SIGNATURE);
        request.setCredential(UUID.randomUUID().toString());

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("Account does not exist."));
    }

    @Test
    public void testInviteExistingAccount() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName(MEMBER_4);

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.CONSTRAINT_ERROR));
        assertThat(response.getReturnMessage(), is("Cannot create an invitation, as as the account already exists."));
    }

    @Test
    public void testInvitingWithoutPermission() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.INVITE);
        request.setNewAccountName("invitee");

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ILLEGAL_ACTION));
        assertThat(response.getReturnMessage(), is("Not permitted to perform this Action."));
    }

    @Test
    public void testProcessSelf() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewAccountName("Supreme Member");
        request.setNewCredential("Bla bla bla");

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testProcessSelfChangeAccountNameToExisting() {
        prepareCause(CWSException.class, ReturnCode.CONSTRAINT_ERROR, "The new Account Name already exists.");
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewAccountName(MEMBER_2);
        request.setNewCredential("Bla bla bla");
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
        final String dataId = addData(MEMBER_1, CIRCLE_1_ID);
        final byte[] data1 = fetchData(MEMBER_1, dataId);

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setNewCredential("My new Passphrase");
        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));

        final FetchDataService fetchService = new FetchDataService(settings, entityManager);
        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setCredential("My new Passphrase");
        fetchRequest.setDataId(dataId);

        final FetchDataResponse dataResponse = fetchService.perform(fetchRequest);
        assertThat(dataResponse.isOk(), is(true));
        final byte[] data2 = dataResponse.getData();
        assertThat(data1, is(data2));
    }

    @Test
    public void testInvalidateSelf() {
        prepareCause(AuthenticationException.class, ReturnCode.AUTHENTICATION_WARNING, "Cannot authenticate the Account '" + MEMBER_4 + "' from the given Credentials.");

        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_4);
        request.setAction(Action.INVALIDATE);

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Account has been Invalidated."));

        request.setAction(Action.UPDATE);
        request.setNewCredential("New Passphrase");
        service.perform(request);
    }

    @Test
    public void testDeleteMember() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        // Member 2
        request.setMemberId("d842fa67-5387-44e6-96e3-4e8a7ead4c8d");

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testDeleteUnknownAccount() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        // Random MemberId, should not exist!
        request.setMemberId(UUID.randomUUID().toString());

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_ERROR));
        assertThat(response.getReturnMessage(), is("No such Account exist."));
    }

    @Test
    public void testDeleteAdmin() {
        final ProcessMemberService service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setMemberId(ADMIN_ID);

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_ERROR));
        assertThat(response.getReturnMessage(), is("It is not permitted to delete the Admin Account."));
    }

    private String addData(final String member, final String circleId) {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);

        final ProcessDataRequest dataRequest = prepareRequest(ProcessDataRequest.class, member);
        dataRequest.setAction(Action.ADD);
        dataRequest.setCircleId(circleId);
        dataRequest.setDataName(UUID.randomUUID().toString());
        dataRequest.setData(generateData(1048576));

        final ProcessDataResponse response = service.perform(dataRequest);
        assertThat(response.isOk(), is(true));
        return response.getDataId();
    }

    private byte[] fetchData(final String member, final String dataId) {
        final FetchDataService service = new FetchDataService(settings, entityManager);
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, member);
        request.setDataId(dataId);

        final FetchDataResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));
        return response.getData();
    }
}
