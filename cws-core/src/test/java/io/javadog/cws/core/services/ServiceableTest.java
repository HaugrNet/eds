/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
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
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.AuthenticationException;
import io.javadog.cws.core.exceptions.AuthorizationException;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ServiceableTest extends DatabaseSetup {

    /**
     * If there was a problem with the database check, which is made initially,
     * then the isReady flag is set to false in the settings, and any request
     * should fail.
     */
    @Test
    public void testRequestWhenNotReady() {
        prepareCause(CWSException.class, ReturnCode.DATABASE_ERROR, "The Database is invalid, CWS neither can nor will work correctly until resolved.");
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.IS_READY.getKey(), "false");

        final SettingService service = new SettingService(mySettings, entityManager);
        final SettingRequest request = new SettingRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(crypto.stringToBytes("Invalid Credentials"));
        assertThat(request, is(not(nullValue())));

        service.perform(request);
    }

    @Test
    public void testAccesWithInvalidPassword() {
        prepareCause(AuthenticationException.class, ReturnCode.AUTHENTICATION_WARNING, "Cannot authenticate the Account from the given Credentials.");

        final SettingService service = new SettingService(settings, entityManager);
        final SettingRequest request = new SettingRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(crypto.stringToBytes("Invalid Credentials"));
        assertThat(request, is(not(nullValue())));

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
    public void testAuthorizationWithInvalidCredentials() {
        prepareCause(AuthenticationException.class, ReturnCode.AUTHENTICATION_WARNING, "Cannot authenticate the Account from the given Credentials.");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        request.setCredential(crypto.stringToBytes("something wrong"));
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAuthorizationWithCredentialTypingMistake() {
        prepareCause(AuthenticationException.class, ReturnCode.AUTHENTICATION_WARNING, "Cannot authenticate the Account from the given Credentials.");

        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        request.setCredential(crypto.stringToBytes(MEMBER_4));
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
