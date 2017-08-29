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
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.DatabaseSetup;
import io.javadog.cws.model.entities.CircleEntity;
import org.junit.Test;

import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchCirclesServiceTest extends DatabaseSetup {

    @Test
    public void testInvalidRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors: Key: credentialTypeError: CredentialType is missing, null or invalid.\n" +
                        "Key: credentialError: Credential is missing, null or invalid.\n" +
                        "Key: accountError: Account is missing, null or invalid.\n");
        final Serviceable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final FetchCircleRequest request = new FetchCircleRequest();
        // Just making sure that the account is missing
        assertThat(request.getAccount(), is(nullValue()));

        // Should throw a VerificationException, as the request is invalid.
        service.perform(request);
    }

    @Test
    public void testFetchNotExistingCircle() {
        final Serviceable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final FetchCircleRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        request.setCircleId(UUID.randomUUID().toString());
        final FetchCircleResponse response = service.perform(request);

        // Verify that we have found the correct data
        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("The requested Circle cannot be found."));
    }

    @Test
    public void testFetchAllCirclesAsAdmin() {
        final Serviceable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final FetchCircleRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(3));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getCircles().get(1).getName(), is("circle2"));
        assertThat(response.getCircles().get(2).getName(), is("circle3"));
        assertThat(response.getTrustees().isEmpty(), is(true));
    }

    @Test
    public void testFetchAllCirclesAsMember1() {
        // TODO Correct test so it follows latest development...
        //prepareCause(ModelException.class, "No member found with 'member1'.");

        final Serviceable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final FetchCircleRequest request = buildRequestWithCredentials("member1");
        assertThat(request, is(not(nullValue())));
        service.perform(request);
    }

    @Test
    public void testFetchCircle1WithShowTrueAsAdmin() {
        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "true");

        final Serviceable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = findFirstCircle();
        final FetchCircleRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getTrustees().size(), is(3));
        assertThat(response.getTrustees().get(0).getMember().getAuthentication().getAccount(), is("member1"));
        assertThat(response.getTrustees().get(1).getMember().getAuthentication().getAccount(), is("member2"));
        assertThat(response.getTrustees().get(2).getMember().getAuthentication().getAccount(), is("member3"));
    }

    @Test
    public void testFetchCircle1WithShowFalseAsAdmin() {
        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "false");

        final Serviceable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = findFirstCircle();
        final FetchCircleRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getTrustees().size(), is(3));
        assertThat(response.getTrustees().get(0).getMember().getAuthentication().getAccount(), is("member1"));
        assertThat(response.getTrustees().get(1).getMember().getAuthentication().getAccount(), is("member2"));
        assertThat(response.getTrustees().get(2).getMember().getAuthentication().getAccount(), is("member3"));
    }

    @Test
    public void testFetchCircle1WithShowTrueAsMember1() {
        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "true");

        final Serviceable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = findFirstCircle();
        final FetchCircleRequest request = buildRequestWithCredentials("member1");
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getTrustees().size(), is(3));
        assertThat(response.getTrustees().get(0).getMember().getAuthentication().getAccount(), is("member1"));
        assertThat(response.getTrustees().get(1).getMember().getAuthentication().getAccount(), is("member2"));
        assertThat(response.getTrustees().get(2).getMember().getAuthentication().getAccount(), is("member3"));
    }

    @Test
    public void testFetchCircle1WithShowFalseAsMember1() {
        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "false");

        final Serviceable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = findFirstCircle();
        final FetchCircleRequest request = buildRequestWithCredentials("member1");
        request.setCircleId(circle.getExternalId());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getTrustees().size(), is(3));
        assertThat(response.getTrustees().get(0).getMember().getAuthentication().getAccount(), is("member1"));
        assertThat(response.getTrustees().get(1).getMember().getAuthentication().getAccount(), is("member2"));
        assertThat(response.getTrustees().get(2).getMember().getAuthentication().getAccount(), is("member3"));
    }

    @Test
    public void testFetchCircle1WithShowTrueAsMember5() {
        prepareCause(ModelException.class, "No member found with 'member5'.");

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "true");

        final Serviceable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = findFirstCircle();
        final FetchCircleRequest request = buildRequestWithCredentials("member5");
        assertThat(request, is(not(nullValue())));
        request.setCircleId(circle.getExternalId());
        service.perform(request);
    }

    @Test
    public void testFetchCircle1WithShowFalseAsMember5() {
        prepareCause(ModelException.class, "No member found with 'member5'.");

        // Ensure that we have the correct settings for the Service
        settings.set(Settings.SHOW_TRUSTEES, "false");

        final Serviceable<FetchCircleResponse, FetchCircleRequest> service = prepareService();
        final CircleEntity circle = findFirstCircle();
        final FetchCircleRequest request = buildRequestWithCredentials("member5");
        assertThat(request, is(not(nullValue())));
        request.setCircleId(circle.getExternalId());
        service.perform(request);
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private Serviceable<FetchCircleResponse, FetchCircleRequest> prepareService() {
        return new FetchCirclesService(settings, entityManager);
    }

    private static FetchCircleRequest buildRequestWithCredentials(final String account) {
        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount(account);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(account);

        return request;
    }

    private CircleEntity findFirstCircle() {
        return entityManager.find(CircleEntity.class, 1L);
    }
}
