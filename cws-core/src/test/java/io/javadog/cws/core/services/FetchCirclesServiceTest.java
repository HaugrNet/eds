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
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.DatabaseSetup;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.MemberEntity;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchCirclesServiceTest extends DatabaseSetup {

    @Test
    public void testInvalidRequest() {
        prepareCause(VerificationException.class, Constants.VERIFICATION_WARNING,
                "Request Object contained errors: Key: credentialTypeError: CredentialType is missing, null or invalid.\n" +
                        "Key: credentialError: Credential is missing, null or invalid.\n" +
                        "Key: accountError: Account is missing, null or invalid.\n");
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final FetchCircleRequest request = new FetchCircleRequest();
        // Just making sure that the account is missing
        assertThat(request.getAccount(), is(nullValue()));

        // Should throw a VerificationException, as the request is invalid.
        service.perform(request);
    }

    @Test
    public void testFetchAllCirclesAsAdmin() {
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        createTwoCircleWith5Members();

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getTrustees().size(), is(0));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getCircles().get(1).getName(), is("circle2"));
    }

    @Test
    public void testFetchAllCirclesAsMember1() {
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        createTwoCircleWith5Members();

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount("member1");
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential("member1".toCharArray());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getTrustees().size(), is(0));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getCircles().get(1).getName(), is("circle2"));
    }

    @Test
    public void testFetchCircle1WithShowTrueAsAdmin() {
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getTrustees().size(), is(4));
        assertThat(response.getTrustees().get(0).getMember().getAuthentication().getAccount(), is("member1"));
        assertThat(response.getTrustees().get(1).getMember().getAuthentication().getAccount(), is("member2"));
        assertThat(response.getTrustees().get(2).getMember().getAuthentication().getAccount(), is("member3"));
        assertThat(response.getTrustees().get(3).getMember().getAuthentication().getAccount(), is("member4"));
    }

    @Test
    public void testFetchCircle1WithShowFalseAsAdmin() {
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getTrustees().size(), is(4));
        assertThat(response.getTrustees().get(0).getMember().getAuthentication().getAccount(), is("member1"));
        assertThat(response.getTrustees().get(1).getMember().getAuthentication().getAccount(), is("member2"));
        assertThat(response.getTrustees().get(2).getMember().getAuthentication().getAccount(), is("member3"));
        assertThat(response.getTrustees().get(3).getMember().getAuthentication().getAccount(), is("member4"));
    }

    @Test
    public void testFetchCircle1WithShowTrueAsMember1() {
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount("member1");
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential("member1".toCharArray());
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getTrustees().size(), is(4));
        assertThat(response.getTrustees().get(0).getMember().getAuthentication().getAccount(), is("member1"));
        assertThat(response.getTrustees().get(1).getMember().getAuthentication().getAccount(), is("member2"));
        assertThat(response.getTrustees().get(2).getMember().getAuthentication().getAccount(), is("member3"));
        assertThat(response.getTrustees().get(3).getMember().getAuthentication().getAccount(), is("member4"));
    }

    @Test
    public void testFetchCircle1WithShowFalseAsMember1() {
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount("member1");
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential("member1".toCharArray());
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getTrustees().size(), is(4));
        assertThat(response.getTrustees().get(0).getMember().getAuthentication().getAccount(), is("member1"));
        assertThat(response.getTrustees().get(1).getMember().getAuthentication().getAccount(), is("member2"));
        assertThat(response.getTrustees().get(2).getMember().getAuthentication().getAccount(), is("member3"));
        assertThat(response.getTrustees().get(3).getMember().getAuthentication().getAccount(), is("member4"));
    }

    @Test
    public void testFetchCircle1WithShowTrueAsMember5() {
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "true");

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount("member5");
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential("member5".toCharArray());
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getTrustees().size(), is(4));
        assertThat(response.getTrustees().get(0).getMember().getAuthentication().getAccount(), is("member1"));
        assertThat(response.getTrustees().get(1).getMember().getAuthentication().getAccount(), is("member2"));
        assertThat(response.getTrustees().get(2).getMember().getAuthentication().getAccount(), is("member3"));
        assertThat(response.getTrustees().get(3).getMember().getAuthentication().getAccount(), is("member4"));
    }

    @Test
    public void testFetchCircle1WithShowFalseAsMember5() {
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = createTwoCircleWith5Members();

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_OTHER_MEMBER_INFORMATION, "false");

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount("member1");
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential("member1".toCharArray());
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getTrustees().size(), is(0));
    }
o
    private Servicable<FetchCircleResponse, FetchCircleRequest> prepareService() {
        final Settings settings = new Settings();
        return new FetchCirclesService(settings, entityManager);
    }

    private CircleEntity createTwoCircleWith5Members() {
        final MemberEntity member1 = createMember("member1");
        final MemberEntity member2 = createMember("member2");
        final MemberEntity member3 = createMember("member3");
        final MemberEntity member4 = createMember("member4");
        final MemberEntity member5 = createMember("member5");

        final CircleEntity circle1 = prepareCircle("circle1");
        final CircleEntity circle2 = prepareCircle("circle2");

        addKeyAndTrusteesToCircle(circle1, member1, member2, member3, member4);
        addKeyAndTrusteesToCircle(circle2, member3, member4, member5);

        return circle1;
    }
}
