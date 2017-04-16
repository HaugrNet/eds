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
import io.javadog.cws.common.exceptions.AuthorizationException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.DatabaseSetup;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.MemberEntity;
import org.junit.Test;

import java.util.UUID;

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

    private static final String ADMIN_ID = "483833a4-2af7-4d9d-953d-b1e86cac8035";
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

    @Test
    public void testFindNotExistingAccount() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        request.setMemberId(UUID.randomUUID().toString());
        assertThat(request.validate().isEmpty(), is(true));
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(Constants.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("The requested Member cannot be found."));
    }

    //==========================================================================
    // Testing fetching All members using different Accounts & Settings
    //==========================================================================

    /**
     * <p>Fetching all Accounts should per default be made with the Expose Admin
     * flag set to false, in this case, we expect to find all (excluding the
     * System Administrator) Accounts and no Circles.</p>
     */
    @Test
    public void testFindAllMembersWithExposeAdminFalseAsAdmin() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        assertThat(request.validate().isEmpty(), is(true));
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(5));
        assertThat(response.getCircles().isEmpty(), is(true));
    }

    /**
     * This test expects a list of All Members, including the System
     * Administrator, which should be 6 in total. No Circles is fetches,
     * as we're not looking at a specific Member Account.
     */
    @Test
    public void testFindAllMembersWithExposeAdminTrueAsAdmin() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        assertThat(request.validate().isEmpty(), is(true));
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(6));
        assertThat(response.getCircles().isEmpty(), is(true));
    }

    /**
     * This test expects a list of All Members, excluding the System
     * Administrator, which should be 5 in total. No Circles is fetches,
     * as we're not looking at a specific Member Account.
     */
    @Test
    public void testFindAllMembersWithExposeAdminFalseAsMember1() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member1");
        assertThat(request.validate().isEmpty(), is(true));
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(5));
        assertThat(response.getCircles().isEmpty(), is(true));
    }

    /**
     * This test expects a list of All Members, including the System
     * Administrator, which should be 6 in total. No Circles is fetches,
     * as we're not looking at a specific Member Account.
     */
    @Test
    public void testFindAllMembersWithExposeAdminTrueAsMember1() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member1");
        assertThat(request.validate().isEmpty(), is(true));
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(6));
        assertThat(response.getCircles().isEmpty(), is(true));
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
    public void testFindAdminWithExposeAdminTrueAsAdmin() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        request.setMemberId(ADMIN_ID);
        assertThat(request.validate().isEmpty(), is(true));
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().isEmpty(), is(true));
        assertThat(response.getMembers().get(0).getAuthentication().getAccount(), is(Constants.ADMIN_ACCOUNT));
    }

    /**
     * <p>The System Administrator should always be able to find all Accounts,
     * regardless of the Settings. The System Administrator is not and cannot
     * be part of any Circles.</p>
     */
    @Test
    public void testFindAdminWithExposeAdminFalseAsAdmin() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        request.setMemberId(ADMIN_ID);
        assertThat(request.validate().isEmpty(), is(true));
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().isEmpty(), is(true));
        assertThat(response.getMembers().get(0).getAuthentication().getAccount(), is(Constants.ADMIN_ACCOUNT));
    }

    /**
     * <p>The System Administrator should always be able to find all Accounts,
     * regardless of the Settings.</p>
     */
    @Test
    public void testFindMember1WithShowOtherFalseAsAdmin() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getMembers().get(0).getAuthentication().getAccount(), is(member.getName()));
    }

    /**
     * <p>The System Administrator should always be able to find all Accounts,
     * regardless of the Settings.</p>
     */
    @Test
    public void testFindMember1WithShowOtherTrueAsAdmin() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getMembers().get(0).getAuthentication().getAccount(), is(member.getName()));
    }

    /**
     * <p>The System Administrator should always be able to find all Accounts,
     * regardless of the Settings. The System Administrator is not and cannot
     * be part of any Circles.</p>
     */
    @Test
    public void testFindAdminWithExposeAdminTrueAsMember1() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(member.getName());
        request.setMemberId(ADMIN_ID);
        assertThat(request.validate().isEmpty(), is(true));
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().isEmpty(), is(true));
        assertThat(response.getMembers().get(0).getAuthentication().getAccount(), is(Constants.ADMIN_ACCOUNT));
    }

    /**
     * <p>The System Administrator should always be able to find all Accounts,
     * regardless of the Settings. The System Administrator is not and cannot
     * be part of any Circles.</p>
     */
    @Test
    public void testFindAdminWithExposeAdminFalseAsMember1() {
        prepareCause(AuthorizationException.class, Constants.AUTHORIZATION_WARNING, "Not Authorized to access this information.");
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(member.getName());
        request.setMemberId(ADMIN_ID);
        assertThat(request.validate().isEmpty(), is(true));
        service.perform(request);
    }

    @Test
    public void testFindMember1WithShowOtherTrueAsMember1() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member1");
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getMembers().get(0).getAuthentication().getAccount(), is(member.getName()));
    }

    @Test
    public void testFindMember1WithShowOtherFalseAsMember1() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member1");
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getMembers().get(0).getAuthentication().getAccount(), is(member.getName()));
    }

    @Test
    public void testFindMember1WithShowOtherTrueAsMember4() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member4");
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(2));
    }

    @Test
    public void testFindMember1WithShowOtherFalseAsMember4() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member4");
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(1));
    }

    @Test
    public void testFindMember1WithShowOtherTrueAsMember5() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member5");
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(2));
    }

    @Test
    public void testFindMember1WithShowOtherFalseAsMember5() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createThreeCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member5");
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(0));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private Servicable<FetchMemberResponse, FetchMemberRequest> prepareService() {
        return new FetchMemberService(settings, entityManager);
    }

    private static FetchMemberRequest buildRequestWithCredentials(final String account) {
        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccount(account);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(account.toCharArray());

        return request;
    }

    /**
     * Creates two Circles with a total of 5 Members, Member 2 - 5 is part of
     * both Circles, whereas Member1 is only member of the first Circle. The
     * returned Member is Member1.
     *
     * @return Member1, which is only a member of the first Circle
     */
    private MemberEntity createThreeCircleWith5Members() {
        final MemberEntity member1 = createMember("member1");
        final MemberEntity member2 = createMember("member2");
        final MemberEntity member3 = createMember("member3");
        final MemberEntity member4 = createMember("member4");
        final MemberEntity member5 = createMember("member5");

        final CircleEntity circle1 = prepareCircle("circle1");
        final CircleEntity circle2 = prepareCircle("circle2");
        final CircleEntity circle3 = prepareCircle("circle3");

        addKeyAndTrusteesToCircle(circle1, member1, member2, member3);
        addKeyAndTrusteesToCircle(circle2, member1, member2, member3, member4);
        addKeyAndTrusteesToCircle(circle3, member2, member3, member4, member5);

        return member1;
    }
}
