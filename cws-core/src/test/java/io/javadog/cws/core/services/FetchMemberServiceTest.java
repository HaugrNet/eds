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
import org.junit.Ignore;
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

    @Test
    @Ignore("To be completed.")
    public void testFindAllMembersAsAdminNoAdminIsolateTrustees() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindAllMembersAsMember1NoAdminIsolateTrustees() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member1");
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindAllMembersAsAdminWithAdminIsolateTrustees() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "true");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindAllMembersAsMember1WithAdminIsolateTrustees() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "true");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member1");
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindAllMembersAsAdminNoAdminShowTrustees() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(5));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindAllMembersAsMember1NoAdminShowTrustees() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member1");
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindAllMembersAsAdminWithAdminShowTrustees() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "true");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindAllMembersAsMember1WithAdminShowTrustees() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "true");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member1");
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindSpecificMemberAsAdmin() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindSpecificMemberAsMemberShared() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member1");
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindSpecificMemberAsMemberNotShared() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request. Member1 & Member5 are not in the same
        // Circles, so it should not be allowed for Member5 to retrieve detailed
        // information about Member1.
        final FetchMemberRequest request = buildRequestWithCredentials("member5");
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindYourselfAsAdmin() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
    }

    @Test
    @Ignore("To be completed.")
    public void testFindYourselfAsMember() {
        final Servicable<FetchMemberResponse, FetchMemberRequest> service = prepareService();
        final MemberEntity member = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.EXPOSE_ADMIN, "false");
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        // Build and send the Request
        final FetchMemberRequest request = buildRequestWithCredentials("member1");
        request.setMemberId(member.getExternalId());
        final FetchMemberResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getMembers().get(0).getId(), is(member.getId()));
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
    private MemberEntity createTwoCircleWith5Members() {
        final MemberEntity member1 = createMember("member1");
        final MemberEntity member2 = createMember("member2");
        final MemberEntity member3 = createMember("member3");
        final MemberEntity member4 = createMember("member4");
        final MemberEntity member5 = createMember("member5");

        final CircleEntity circle1 = prepareCircle("circle1");
        final CircleEntity circle2 = prepareCircle("circle2");

        addKeyAndTrusteesToCircle(circle1, member1, member2, member3, member4);
        addKeyAndTrusteesToCircle(circle2, member3, member4, member5);

        return member1;
    }
}
