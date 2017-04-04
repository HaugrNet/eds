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

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.DatabaseSetup;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.MemberEntity;
import org.junit.Test;

/**
 * <p>Fetching members is similar to fetching Circles, in that it is possible to
 * retrieve a list of Members in the CWS, and information about a specific
 * Member Account. However, there's also a couple of property values which
 * influence who may see what:</p>
 * <ul>
 *   <li><b>cws.expose.admin</b><br>
 *       <i>The Administrator is not a &quot;real&quot; Member, as the Account
 *       cannot belong to any Circles. It is only present as the initial
 *       Account, with permission to process Members & Circles.</i></li>
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
 * @since  CWS 1.0
 */
public final class FetchMemberServiceTest extends DatabaseSetup {

    /**
     * Testing a Request without any credentials. This should always result in
     * an error from CWS.
     */
    @Test
    public void testEmptyRequest() {
        prepareCause(VerificationException.class, Constants.VERIFICATION_WARNING,
                "Request Object contained errors: Key: credentialTypeError: CredentialType is missing, null or invalid.\n" +
                "Key: credentialError: Credential is missing, null or invalid.\n" +
                "Key: accountError: Account is missing, null or invalid.\n");
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final FetchMemberRequest request = new FetchMemberRequest();
        // Just making sure that the account is missing
        assertThat(request.getAccount(), is(nullValue()));

        // Should throw a VerificationException, as the request is invalid.
        service.perform(request);
    }

    /**
     * Testing retrieval of all Members as Administrator, with the Expose Admin
     * Property set to True, which should result in a List of all Accounts.
     */
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

    /**
     * Testing retrieval of a specific account as Administrator, this should
     * always work, regardless of the Property settings.
     */
    @Test
    public void testAdminRequestWithMemberId() {
        final Settings settings = new Settings();
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService(settings);
        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        request.setMemberId("483833a4-2af7-4d9d-953d-b1e86cac8035");

        // First test, with the Both ExposeAdmin & ShowMembers set to true
        // Second test, with ExposeAdmin true & ShowMembers false
        // Third test, with ExposeAdmin false and ShowMembers true
        // Fourth test, with Both ExposeAdmin & ShowMembers set to false

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

    @Test
    public void testFindAllMembersNoAdminIsolateTrustees() {
        createTwoCircleWith5Members();
        final Settings settings = new Settings();
        settings.set(Settings.EXPOSE_ADMIN, "false");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService(settings);
        final FetchMemberRequest request = buildRequest("member1");
        final FetchMemberResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void testFindAllMembersNoAdminAllTrustees() {
        createTwoCircleWith5Members();
        final Settings settings = new Settings();
        settings.set(Settings.EXPOSE_ADMIN, "false");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService(settings);
        final FetchMemberRequest request = buildRequest("member1");
        final FetchMemberResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void testFindAllMembersAndAdminIsolateTrustees() {
        createTwoCircleWith5Members();
        final Settings settings = new Settings();
        settings.set(Settings.EXPOSE_ADMIN, "true");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService(settings);
        final FetchMemberRequest request = buildRequest("member1");
        final FetchMemberResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void testFindAllMembersAndAdminAllTrustees() {
        createTwoCircleWith5Members();
        final Settings settings = new Settings();
        settings.set(Settings.EXPOSE_ADMIN, "true");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService(settings);
        final FetchMemberRequest request = buildRequest("member1");
        final FetchMemberResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
    }

    private Servicable<FetchMemberResponse, FetchMemberRequest> prepareService() {
        final Settings settings = new Settings();
        return new FetchMemberService(settings, entityManager);
    }

    private Servicable<FetchMemberResponse, FetchMemberRequest> prepareService(final Settings settings) {
        return new FetchMemberService(settings, entityManager);
    }

    private static FetchMemberRequest buildRequest(final String member) {
        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccount(member);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(member.toCharArray());

        return request;
    }

    private void createTwoCircleWith5Members() {
        final MemberEntity member1 = createMember("member1");
        final MemberEntity member2 = createMember("member2");
        final MemberEntity member3 = createMember("member3");
        final MemberEntity member4 = createMember("member4");
        final MemberEntity member5 = createMember("member5");

        final CircleEntity circle1 = prepareCircle("circle1");
        final CircleEntity circle2 = prepareCircle("circle2");

        addKeyAndTrusteesToCircle(circle1, member1, member2, member3, member4);
        addKeyAndTrusteesToCircle(circle2, member3, member4, member5);
    }
}
