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
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberServiceTest extends DatabaseSetup {

    @Test(expected = CWSException.class)
    public void testService() {
        final Serviceable<ProcessMemberResponse, ProcessMemberRequest> service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareAdminRequest();
        request.setAction(Action.PROCESS);

        service.perform(request);
    }

    @Test
    public void testInvitation() {
        final Serviceable<ProcessMemberResponse, ProcessMemberRequest> service = new ProcessMemberService(settings, entityManager);
        final ProcessMemberRequest request = prepareAdminRequest();
        request.setAction(Action.INVITE);
        request.setAccountName("invitee");

        final ProcessMemberResponse response = service.perform(request);
        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        final String signature = response.getSignature();

        final ProcessMemberRequest invationRequest = new ProcessMemberRequest();
        invationRequest.setAccount("invitee");
        invationRequest.setCredentialType(CredentialType.SIGNATURE);
        invationRequest.setCredential(signature);
        final ProcessMemberResponse invitationResponse = service.perform(invationRequest);
        assertThat(invitationResponse, is(not(nullValue())));
        assertThat(invitationResponse.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    private static ProcessMemberRequest prepareAdminRequest() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT);

        return request;
    }
}
