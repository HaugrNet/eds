/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ManagementServiceTest extends BeanSetup {

    @Test
    public void testVersion() throws IOException {
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
            assertThat(response.getVersion(), is(version));
        } else {
            fail("Could not open the Class Loader, to read the '" + propertiesFile + "' file from the test resource path.");
        }
    }

    @Test
    public void testFlawedVersion() {
        final ManagementService management = prepareFlawedManagementService();

        final VersionResponse response = management.version();
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testSettingsAsMember() {
        final ManagementService management = prepareManagementService();
        final SettingRequest request = prepareRequest(SettingRequest.class, MEMBER_1);

        final SettingResponse response = management.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING.getCode()));
        assertThat(response.getSettings().size(), is(0));
    }

    @Test
    public void testSettings() {
        final ManagementService management = prepareManagementService();
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = management.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    public void testUpdateSettingsWithInvalidData() {
        final ManagementService management = prepareManagementService();
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> map = request.getSettings();
        map.put(StandardSetting.PBE_ALGORITHM.getKey(), "Hash Them");
        request.setSettings(map);

        final SettingResponse response = management.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SETTING_WARNING.getCode()));
        assertThat(response.getSettings().size(), is(StandardSetting.values().length));
    }

    @Test
    public void testSettingsWithNullRequest() {
        final ManagementService management = prepareManagementService();
        final SettingRequest request = null;

        final SettingResponse response = management.settings(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testSettingsWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final SettingRequest request = new SettingRequest();

        final SettingResponse response = management.settings(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedSettings() {
        final ManagementService management = prepareFlawedManagementService();
        final SettingRequest request = null;

        final SettingResponse response = management.settings(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testMasterKey() {
        final ManagementService management = prepareManagementService();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());

        final MasterKeyResponse response = management.masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    public void testMasterKeyWithNullRequest() {
        final ManagementService management = prepareManagementService();
        final MasterKeyRequest request = null;

        final MasterKeyResponse response = management.masterKey(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testMasterKeyWithProblems() {
        final ManagementService management = prepareFlawedManagementService();
        final MasterKeyRequest request = new MasterKeyRequest();

        final MasterKeyResponse response = management.masterKey(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
        assertThat(response.getReturnMessage(), is("An unknown error occurred. Please consult the CWS System Log."));
    }

    @Test
    public void testFlawedMasterKey() {
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
        assertThat(response.getReturnCode(), is(ReturnCode.CRYPTO_ERROR.getCode()));
        assertThat(response.getReturnMessage(), is("the salt parameter must not be empty"));

        // Before completing the test, revert the standard setting to the
        // original value, as it will otherwise cause problems for other tests.
        settings.set(StandardSetting.CWS_SALT.getKey(), StandardSetting.CWS_SALT.getValue());
    }

    @Test
    public void testSanity() {
        final ManagementService management = prepareManagementService();
        final SanityRequest request = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);

        final SanityResponse response = management.sanitized(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    public void testSanityWithNullRequest() {
        final ManagementService management = prepareManagementService();
        final SanityRequest request = null;

        final SanityResponse response = management.sanitized(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testSanityWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final SanityRequest request = new SanityRequest();

        final SanityResponse response = management.sanitized(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedSanity() {
        final ManagementService management = prepareFlawedManagementService();
        final SanityRequest request = null;

        final SanityResponse response = management.sanitized(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testAuthenticated() {
        final ManagementService management = prepareManagementService();
        final Authentication request = prepareRequest(Authentication.class, Constants.ADMIN_ACCOUNT);

        final CwsResponse response = management.authenticated(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    public void testAuthenticatedWithNullRequest() {
        final ManagementService management = prepareManagementService();
        final Authentication request = null;

        final CwsResponse response = management.authenticated(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testAuthenticatedWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final Authentication request = new SanityRequest();

        final CwsResponse response = management.authenticated(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedAuthenticated() {
        final ManagementService management = prepareFlawedManagementService();
        final Authentication request = null;

        final CwsResponse response = management.authenticated(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchMembers() {
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
    public void testFetchMembersWithNullRequest() {
        final ManagementService management = prepareManagementService();
        final FetchMemberRequest request = null;

        final FetchMemberResponse response = management.fetchMembers(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchMembersWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final FetchMemberRequest request = new FetchMemberRequest();

        final FetchMemberResponse response = management.fetchMembers(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedFetchMembers() {
        final ManagementService management = prepareFlawedManagementService();
        final FetchMemberRequest request = null;

        final FetchMemberResponse response = management.fetchMembers(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testProcessMember() {
        final ManagementService management = prepareManagementService();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("new Account");

        final ProcessMemberResponse response = management.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    public void testProcessMemberWithNullRequest() {
        final ManagementService management = prepareManagementService();
        final ProcessMemberRequest request = null;

        final ProcessMemberResponse response = management.processMember(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testProcessMemberWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final ProcessMemberResponse response = management.processMember(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedProcessMember() {
        final ManagementService management = prepareFlawedManagementService();
        final ProcessMemberRequest request = null;

        final ProcessMemberResponse response = management.processMember(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchCircle() {
        final ManagementService management = prepareManagementService();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);

        final FetchCircleResponse response = management.fetchCircles(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchCircleWithNullRequest() {
        final ManagementService management = prepareManagementService();
        final FetchCircleRequest request = null;

        final FetchCircleResponse response = management.fetchCircles(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchCircleWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final FetchCircleRequest request = new FetchCircleRequest();

        final FetchCircleResponse response = management.fetchCircles(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedFetchCircle() {
        final ManagementService management = prepareFlawedManagementService();
        final FetchCircleRequest request = null;

        final FetchCircleResponse response = management.fetchCircles(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testProcessCircle() {
        final ManagementService management = prepareManagementService();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName("Test Circle");
        request.setMemberId(MEMBER_1_ID);

        final ProcessCircleResponse response = management.processCircle(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    public void testProcessCircleWithNullRequest() {
        final ManagementService management = prepareManagementService();
        final ProcessCircleRequest request = null;

        final ProcessCircleResponse response = management.processCircle(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testProcessCircleWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final ProcessCircleResponse response = management.processCircle(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedProcessCircle() {
        final ManagementService management = prepareFlawedManagementService();
        final ProcessCircleRequest request = null;

        final ProcessCircleResponse response = management.processCircle(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchTrustee() {
        final ManagementService management = prepareManagementService();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);

        final FetchTrusteeResponse response = management.fetchTrustees(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchTrusteeWithNullRequest() {
        final ManagementService management = prepareManagementService();
        final FetchTrusteeRequest request = null;

        final FetchTrusteeResponse response = management.fetchTrustees(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchTrusteeWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final FetchTrusteeRequest request = new FetchTrusteeRequest();

        final FetchTrusteeResponse response = management.fetchTrustees(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedFetchTrustee() {
        final ManagementService management = prepareFlawedManagementService();
        final FetchTrusteeRequest request = null;

        final FetchTrusteeResponse response = management.fetchTrustees(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testProcessTrustee() {
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
    public void testProcessTrusteeWithNullRequest() {
        final ManagementService management = prepareManagementService();
        final ProcessTrusteeRequest request = null;

        final ProcessTrusteeResponse response = management.processTrustee(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testProcessTrusteeWithEmptyRequest() {
        final ManagementService management = prepareManagementService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final ProcessTrusteeResponse response = management.processTrustee(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedProcessTrustee() {
        final ManagementService management = prepareFlawedManagementService();
        final ProcessTrusteeRequest request = null;

        final ProcessTrusteeResponse response = management.processTrustee(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }
}
