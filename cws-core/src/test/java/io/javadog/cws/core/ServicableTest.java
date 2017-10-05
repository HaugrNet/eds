/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.common.exceptions.AuthenticationException;
import io.javadog.cws.common.exceptions.AuthorizationException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.services.FetchCircleService;
import io.javadog.cws.core.services.SettingService;
import io.javadog.cws.core.services.SignService;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ServicableTest extends DatabaseSetup {

    @Test
    public void testAccessSettingsWithNullRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING, "Cannot Process a NULL Object.");

        final SettingService service = new SettingService(settings, entityManager);
        final SettingRequest request = null;
        assertThat(request, is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testAccessSettingsAsMember() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "Cannot complete this request, as it is only allowed for the System Administrator.");

        final SettingService service = new SettingService(settings, entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, MEMBER_1);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testCreateSignatureWithoutPermission() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING, "he requesting Account is not permitted to Create Digital Signature.");

        final SignService service = new SignService(settings, entityManager);
        final SignRequest request = prepareRequest(SignRequest.class, MEMBER_5);
        request.setData(generateData(262144));
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAuthorizationWithInvalidCredentials() {
        prepareCause(AuthenticationException.class, ReturnCode.AUTHENTICATION_WARNING, "Cannot authenticate the Account 'member5' from the given Credentials.");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        request.setCredential("something wrong");
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAuthorizationWithCredentialTypingMistake() {
        prepareCause(AuthenticationException.class, ReturnCode.AUTHENTICATION_WARNING, "Cannot authenticate the Account 'member5' from the given Credentials.");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        request.setCredential(MEMBER_4);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testFetchCirclesAsNonExistingMember() {
        prepareCause(AuthenticationException.class, ReturnCode.AUTHENTICATION_WARNING, "Could not uniquely identify an account for 'member6'.");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, "member6");
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }
}
