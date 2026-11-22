/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.responses.AuthenticateResponse;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.SettingResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>This Test Class is testing the Authenticated Service Class.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1
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
        final ManagementBean bean = prepareManagementBean(mySettings);
        mySettings.set(StandardSetting.IS_READY.getKey(), "false");

        final SettingRequest request = new SettingRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(crypto.stringToBytes("Invalid Credentials"));

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.DATABASE_ERROR.getCode(), response.getReturnCode());
        assertEquals("The Database is invalid, EDS neither can nor will work correctly until resolved.", response.getReturnMessage());
    }

    /**
     * A stone has no account to begin with, and no name for one: the first
     * who comes becomes its System Administrator, under the name given.
     * After that an unknown name is unknown, "admin" as any other.
     */
    @Test
    void testFirstAccountBecomesSystemAdministrator() {
        for (final net.haugr.eds.core.model.entities.MemberEntity member : dao.findAllAscending(net.haugr.eds.core.model.entities.MemberEntity.class, "id")) {
            dao.delete(member);
        }
        assertEquals(0L, dao.countMembers());
        final ManagementBean bean = prepareManagementBean();

        final AuthenticateResponse first = bean.authenticated(prepareRequest(Authentication.class, "dude"));
        assertEquals(ReturnCode.SUCCESS.getCode(), first.getReturnCode(), first.getReturnMessage());
        assertEquals(net.haugr.eds.api.common.MemberRole.ADMIN, dao.findMemberByName("dude").getMemberRole());
        assertEquals(1L, dao.countMembers());

        // The stone has an account now, so nobody else is made one by coming.
        for (final String name : new String[] { Constants.ADMIN_ACCOUNT, "sysop" }) {
            final AuthenticateResponse later = bean.authenticated(prepareRequest(Authentication.class, name));
            assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), later.getReturnCode());
            assertEquals("Could not uniquely identify an account for '" + name + "'.", later.getReturnMessage());
        }
        assertEquals(1L, dao.countMembers());

        // And the first one is known again, by the passphrase it came with.
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.authenticated(prepareRequest(Authentication.class, "dude")).getReturnCode());
        final Authentication wrong = prepareRequest(Authentication.class, "dude");
        wrong.setCredential(credentialOf("someone else"));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), bean.authenticated(wrong).getReturnCode());
    }

    @Test
    void testAuthenticateNullRequest() {
        final ManagementBean bean = prepareManagementBean();
        final Authentication request = null;

        // Null is passed on purpose, to check how it is refused.
        @SuppressWarnings("findbugs:NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
        final AuthenticateResponse response = bean.authenticated(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot Process a NULL Object.", response.getReturnMessage());
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
        request.setCredential(credentialOf(MEMBER_4));

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
