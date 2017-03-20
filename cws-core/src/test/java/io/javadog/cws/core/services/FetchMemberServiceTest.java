/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchMemberServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyRequest() {
        prepareCause(VerificationException.class, Constants.VERIFICATION_WARNING,
                "Request Object contained errors: Key: credentialTypeError: CredentialType is missing, null or invalid.\n" +
                "Key: credentialError: Credential is missing, null or invalid.\n" +
                "Key: accountError: Account is missing, null or invalid.\n");
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final FetchMemberRequest request = new FetchMemberRequest();

        service.perform(request);
    }

    @Test
    public void testAdminRequest() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        final FetchMemberResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().isEmpty(), is(true));
        assertThat(response.getMembers().get(0).getAuthentication().getAccount(), is(Constants.ADMIN_ACCOUNT));
        assertThat(response.getMembers().get(0).getId(), is("483833a4-2af7-4d9d-953d-b1e86cac8035"));
    }

    @Test
    public void testAdminRequestWithMemberId() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        request.setMemberId("483833a4-2af7-4d9d-953d-b1e86cac8035");
        final FetchMemberResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().isEmpty(), is(true));
        assertThat(response.getMembers().get(0).getAuthentication().getAccount(), is(Constants.ADMIN_ACCOUNT));
        assertThat(response.getMembers().get(0).getId(), is("483833a4-2af7-4d9d-953d-b1e86cac8035"));
    }

    @Test
    public void testAdminRequestWithInvalidMemberId() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        request.setMemberId("483833a4-2af7-4d9d-953d-b1e86cac8021");
        final FetchMemberResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(Constants.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("The requested Member cannot be found."));
        assertThat(response.getMembers().isEmpty(), is(true));
        assertThat(response.getCircles().isEmpty(), is(true));
    }

    private Servicable<FetchMemberResponse, FetchMemberRequest> prepareService() {
        final Settings settings = new Settings();
        return new FetchMemberService(settings, entityManager);
    }

}
