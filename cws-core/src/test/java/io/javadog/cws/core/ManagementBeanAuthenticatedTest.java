/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.AuthenticateResponse;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.services.SettingService;
import io.javadog.cws.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>This Test Class, is testing the Authenticated Service Class.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
final class ManagementBeanAuthenticatedTest extends DatabaseSetup {

    /**
     * If there was a problem with the database check, which is made initially,
     * then the isReady flag is set to false in the settings, and any request
     * should fail.
     */
    @Test
    void testRequestWhenNotReady() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.IS_READY.getKey(), "false");

        final SettingService service = new SettingService(mySettings, entityManager);
        final SettingRequest request = new SettingRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(crypto.stringToBytes("Invalid Credentials"));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.DATABASE_ERROR, cause.getReturnCode());
        assertEquals("The Database is invalid, CWS neither can nor will work correctly until resolved.", cause.getMessage());
    }

    @Test
    void testAuthenticate() {
        final ManagementBean bean = prepareManagementBean();
        final Authentication request = prepareRequest(Authentication.class, MEMBER_1);
        final AuthenticateResponse response = bean.authenticated(request);

        assertNotNull(response);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("member1 successfully authenticated.", response.getReturnMessage());
        assertEquals(MEMBER_1_ID, response.getMemberId());
    }

    @Test
    void testAccessWithInvalidPassword() {
        final ManagementBean bean = prepareManagementBean();
        final SettingRequest request = new SettingRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(crypto.stringToBytes("Invalid Credentials"));

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot authenticate the Account from the given Credentials.", response.getReturnMessage());
    }

    @Test
    void testAccessSettingsAsMember() {
        final ManagementBean bean = prepareManagementBean();
        final SettingRequest request = prepareRequest(SettingRequest.class, MEMBER_1);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot complete this request, as it is only allowed for the System Administrator.", response.getReturnMessage());
    }

    @Test
    void testAuthorizationWithInvalidCredentials() {
        final ManagementBean bean = prepareManagementBean();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        request.setCredential(crypto.stringToBytes("something wrong"));

        final FetchCircleResponse response = bean.fetchCircles(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot authenticate the Account from the given Credentials.", response.getReturnMessage());
    }

    @Test
    void testAuthorizationWithCredentialTypingMistake() {
        final ManagementBean bean = prepareManagementBean();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        request.setCredential(crypto.stringToBytes(MEMBER_4));

        final FetchCircleResponse response = bean.fetchCircles(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot authenticate the Account from the given Credentials.", response.getReturnMessage());
    }

    @Test
    void testFetchCirclesAsNonExistingMember() {
        final ManagementBean bean = prepareManagementBean();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, "member6");

        final FetchCircleResponse response = bean.fetchCircles(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Could not uniquely identify an account for 'member6'.", response.getReturnMessage());
    }
}
