/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
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
package io.javadog.cws.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.core.enums.StandardSetting;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class ManagementServiceTest extends BeanSetup {

    @Test
    void testVersion() throws IOException {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final String propertiesFile = "cws.config";

        if (loader != null) {
            final ManagementService management = prepareManagementService();
            final String version;

            try (final InputStream stream = loader.getResourceAsStream(propertiesFile)) {
                final Properties properties = new Properties();
                properties.load(stream);
                version = properties.getProperty("cws.version");
            }

            final VersionResponse response = management.version();
            assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
            assertEquals(version, response.getVersion());
        } else {
            fail("Could not open the Class Loader, to read the '" + propertiesFile + "' file from the test resource path.");
        }
    }

    @Test
    void testFlawedVersion() {
        final ManagementService management = prepareFlawedManagementService();

        final VersionResponse response = management.version();
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testSettingsAsMember() {
        final ManagementService management = prepareManagementService();
        final SettingRequest request = prepareRequest(SettingRequest.class, MEMBER_1);

        final SettingResponse response = management.settings(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertTrue(response.getSettings().isEmpty());
    }

    @Test
    void testSettings() {
        final ManagementService management = prepareManagementService();
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = management.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    void testUpdateSettingsWithInvalidData() {
        final ManagementService management = prepareManagementService();
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> map = request.getSettings();
        map.put(StandardSetting.PBE_ALGORITHM.getKey(), "Hash Them");
        request.setSettings(map);

        final SettingResponse response = management.settings(request);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response.getReturnCode());
        assertEquals(StandardSetting.values().length, response.getSettings().size());
    }

    @Test
    void testSettingsWithNullRequest() {
        final ManagementService management = prepareManagementService();

        final SettingResponse response = management.settings(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testSettingsWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final SettingRequest request = new SettingRequest();

        final SettingResponse response = management.settings(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedSettings() {
        final ManagementService management = prepareFlawedManagementService();

        final SettingResponse response = management.settings(null);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testMasterKey() {
        final ManagementService management = prepareManagementService();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());

        final MasterKeyResponse response = management.masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    void testMasterKeyWithNullRequest() {
        final ManagementService management = prepareManagementService();

        final MasterKeyResponse response = management.masterKey(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testMasterKeyWithProblems() {
        final ManagementService management = prepareFlawedManagementService();
        final MasterKeyRequest request = new MasterKeyRequest();

        final MasterKeyResponse response = management.masterKey(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
        assertEquals("An unknown error occurred. Please consult the CWS System Log.", response.getReturnMessage());
    }

    @Test
    void testFlawedMasterKey() {
        // The MasterKey must be robust, meaning that it should be _really_ hard
        // to mess with it. So, as settings are controlled and checked before
        // being set - it is only possible to mess with the MasterKey, by doing
        // something illegal outside of the normal work flow.
        //   Hence, the Salt is being set to an illegal value before we start
        // the test.
        settings.set(StandardSetting.CWS_SALT.getKey(), "");
        final ManagementService management = prepareManagementService(settings);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());

        final MasterKeyResponse response = management.masterKey(request);
        assertEquals(ReturnCode.CRYPTO_ERROR.getCode(), response.getReturnCode());
        assertEquals("the salt parameter must not be empty", response.getReturnMessage());

        // Before completing the test, revert the standard setting to the
        // original value, as it will otherwise cause problems for other tests.
        settings.set(StandardSetting.CWS_SALT.getKey(), StandardSetting.CWS_SALT.getValue());
    }

    @Test
    void testSanity() {
        final ManagementService management = prepareManagementService();
        final SanityRequest request = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);

        final SanityResponse response = management.sanitized(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    void testSanityWithNullRequest() {
        final ManagementService management = prepareManagementService();

        final SanityResponse response = management.sanitized(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testSanityWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final SanityRequest request = new SanityRequest();

        final SanityResponse response = management.sanitized(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedSanity() {
        final ManagementService management = prepareFlawedManagementService();

        final SanityResponse response = management.sanitized(null);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testAuthenticated() {
        final ManagementService management = prepareManagementService();
        final Authentication request = prepareRequest(Authentication.class, Constants.ADMIN_ACCOUNT);

        final CwsResponse response = management.authenticated(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    void testAuthenticatedWithNullRequest() {
        final ManagementService management = prepareManagementService();

        final CwsResponse response = management.authenticated(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testAuthenticatedWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final Authentication request = new SanityRequest();

        final CwsResponse response = management.authenticated(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedAuthenticated() {
        final ManagementService management = prepareFlawedManagementService();

        final CwsResponse response = management.authenticated(null);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchMembers() {
        final ManagementService management = prepareManagementService();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_1);

        final FetchMemberResponse response = management.fetchMembers(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals(6, response.getMembers().size());
        assertTrue(response.getCircles().isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, response.getMembers().get(0).getAccountName());
        assertEquals(ADMIN_ID, response.getMembers().get(0).getMemberId());
        assertNull(response.getMembers().get(0).getPublicKey());
        assertEquals(MemberRole.ADMIN, response.getMembers().get(0).getMemberRole());
        assertEquals(MEMBER_1, response.getMembers().get(1).getAccountName());
        assertEquals(MEMBER_1_ID, response.getMembers().get(1).getMemberId());
        assertNull(response.getMembers().get(1).getPublicKey());
        assertEquals(MemberRole.STANDARD, response.getMembers().get(1).getMemberRole());
        assertEquals(MEMBER_2, response.getMembers().get(2).getAccountName());
        assertEquals(MEMBER_2_ID, response.getMembers().get(2).getMemberId());
        assertNull(response.getMembers().get(2).getPublicKey());
        assertEquals(MemberRole.STANDARD, response.getMembers().get(2).getMemberRole());
        assertEquals(MEMBER_3, response.getMembers().get(3).getAccountName());
        assertEquals(MEMBER_3_ID, response.getMembers().get(3).getMemberId());
        assertNull(response.getMembers().get(3).getPublicKey());
        assertEquals(MemberRole.STANDARD, response.getMembers().get(3).getMemberRole());
        assertEquals(MEMBER_4, response.getMembers().get(4).getAccountName());
        assertEquals(MEMBER_4_ID, response.getMembers().get(4).getMemberId());
        assertNull(response.getMembers().get(4).getPublicKey());
        assertEquals(MemberRole.STANDARD, response.getMembers().get(4).getMemberRole());
        assertEquals(MEMBER_5, response.getMembers().get(5).getAccountName());
        assertEquals(MEMBER_5_ID, response.getMembers().get(5).getMemberId());
        assertNull(response.getMembers().get(5).getPublicKey());
        assertEquals(MemberRole.STANDARD, response.getMembers().get(5).getMemberRole());
    }

    @Test
    void testFetchMembersWithNullRequest() {
        final ManagementService management = prepareManagementService();

        final FetchMemberResponse response = management.fetchMembers(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchMembersWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final FetchMemberRequest request = new FetchMemberRequest();

        final FetchMemberResponse response = management.fetchMembers(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedFetchMembers() {
        final ManagementService management = prepareFlawedManagementService();

        final FetchMemberResponse response = management.fetchMembers(null);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessMember() {
        final ManagementService management = prepareManagementService();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("new Account");

        final ProcessMemberResponse response = management.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessMemberWithNullRequest() {
        final ManagementService management = prepareManagementService();

        final ProcessMemberResponse response = management.processMember(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessMemberWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final ProcessMemberResponse response = management.processMember(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedProcessMember() {
        final ManagementService management = prepareFlawedManagementService();

        final ProcessMemberResponse response = management.processMember(null);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchCircle() {
        final ManagementService management = prepareManagementService();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);

        final FetchCircleResponse response = management.fetchCircles(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchCircleWithNullRequest() {
        final ManagementService management = prepareManagementService();

        final FetchCircleResponse response = management.fetchCircles(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchCircleWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final FetchCircleRequest request = new FetchCircleRequest();

        final FetchCircleResponse response = management.fetchCircles(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedFetchCircle() {
        final ManagementService management = prepareFlawedManagementService();

        final FetchCircleResponse response = management.fetchCircles(null);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessCircle() {
        final ManagementService management = prepareManagementService();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName("Test Circle");
        request.setMemberId(MEMBER_1_ID);

        final ProcessCircleResponse response = management.processCircle(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessCircleWithNullRequest() {
        final ManagementService management = prepareManagementService();

        final ProcessCircleResponse response = management.processCircle(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessCircleWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final ProcessCircleResponse response = management.processCircle(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedProcessCircle() {
        final ManagementService management = prepareFlawedManagementService();

        final ProcessCircleResponse response = management.processCircle(null);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchTrustee() {
        final ManagementService management = prepareManagementService();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);

        final FetchTrusteeResponse response = management.fetchTrustees(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchTrusteeWithNullRequest() {
        final ManagementService management = prepareManagementService();

        final FetchTrusteeResponse response = management.fetchTrustees(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchTrusteeWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final FetchTrusteeRequest request = new FetchTrusteeRequest();

        final FetchTrusteeResponse response = management.fetchTrustees(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedFetchTrustee() {
        final ManagementService management = prepareFlawedManagementService();

        final FetchTrusteeResponse response = management.fetchTrustees(null);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessTrustee() {
        final ManagementService management = prepareManagementService();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ALTER);
        request.setMemberId(MEMBER_2_ID);
        request.setCircleId(CIRCLE_1_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = management.processTrustee(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessTrusteeWithNullRequest() {
        final ManagementService management = prepareManagementService();

        final ProcessTrusteeResponse response = management.processTrustee(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessTrusteeWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final ProcessTrusteeResponse response = management.processTrustee(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedProcessTrustee() {
        final ManagementService management = prepareFlawedManagementService();

        final ProcessTrusteeResponse response = management.processTrustee(null);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }
}
