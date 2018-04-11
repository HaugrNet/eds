/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-soap)
 * =============================================================================
 */
package io.javadog.cws.soap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.core.enums.StandardSetting;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ManagementServiceTest extends BeanSetup {

    @Test
    public void testVersion() throws IOException {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final String propertiesFile = "cws.config";

        if (loader != null) {
            final ManagementService system = prepareSystemService();
            final String version;

            try (final InputStream stream = loader.getResourceAsStream(propertiesFile)) {
                final Properties properties = new Properties();
                properties.load(stream);
                version = properties.getProperty("cws.version");
            }

            final VersionResponse response = system.version();
            assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
            assertThat(response.getVersion(), is(version));
        } else {
            fail("Could not open the Class Loader, to read the '" + propertiesFile + "' file from the test resource path.");
        }
    }

    @Test
    public void testFlawedVersion() {
        final ManagementService system = prepareFlawedSystemService();

        final VersionResponse response = system.version();
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testSettingsAsMember() {
        final ManagementService system = prepareSystemService();
        final SettingRequest request = prepareRequest(SettingRequest.class, MEMBER_1);

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHORIZATION_WARNING.getCode()));
        assertThat(response.getSettings().size(), is(0));
    }

    @Test
    public void testSettings() {
        final ManagementService system = prepareSystemService();
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
    }

    @Test
    public void testUpdateSettingsWithInvalidData() {
        final ManagementService system = prepareSystemService();
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> map = request.getSettings();
        map.put(StandardSetting.PBE_ALGORITHM.getKey(), "Hash Them");
        request.setSettings(map);

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SETTING_WARNING.getCode()));
        assertThat(response.getSettings().size(), is(StandardSetting.values().length));
    }

    @Test
    public void testSettingsWithNullRequest() {
        final ManagementService system = prepareSystemService();
        final SettingRequest request = null;

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testSettingsWithEmptyRequest() {
        final ManagementService system = prepareSystemService();
        final SettingRequest request = new SettingRequest();

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedSettings() {
        final ManagementService system = prepareFlawedSystemService();
        final SettingRequest request = null;

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testSanity() {
        final ManagementService system = prepareSystemService();
        final SanityRequest request = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);

        final SanityResponse response = system.sanitized(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
    }

    @Test
    public void testSanityWithNullRequest() {
        final ManagementService system = prepareSystemService();
        final SanityRequest request = null;

        final SanityResponse response = system.sanitized(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testSanityWithEmptyRequest() {
        final ManagementService system = prepareSystemService();
        final SanityRequest request = new SanityRequest();

        final SanityResponse response = system.sanitized(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedSanity() {
        final ManagementService system = prepareFlawedSystemService();
        final SanityRequest request = null;

        final SanityResponse response = system.sanitized(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testFetchMembers() {
        final ManagementService system = prepareSystemService();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_1);

        final FetchMemberResponse response = system.fetchMembers(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
    }

    @Test
    public void testFetchMembersWithNullRequest() {
        final ManagementService system = prepareSystemService();
        final FetchMemberRequest request = null;

        final FetchMemberResponse response = system.fetchMembers(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFetchMembersWithEmptyRequest() {
        final ManagementService system = prepareSystemService();
        final FetchMemberRequest request = new FetchMemberRequest();

        final FetchMemberResponse response = system.fetchMembers(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedFetchMembers() {
        final ManagementService system = prepareFlawedSystemService();
        final FetchMemberRequest request = null;

        final FetchMemberResponse response = system.fetchMembers(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testProcessMember() {
        final ManagementService system = prepareSystemService();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("new Account");

        final ProcessMemberResponse response = system.processMember(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
    }

    @Test
    public void testProcessMemberWithNullRequest() {
        final ManagementService system = prepareSystemService();
        final ProcessMemberRequest request = null;

        final ProcessMemberResponse response = system.processMember(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testProcessMemberWithEmptyRequest() {
        final ManagementService system = prepareSystemService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final ProcessMemberResponse response = system.processMember(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedProcessMember() {
        final ManagementService system = prepareFlawedSystemService();
        final ProcessMemberRequest request = null;

        final ProcessMemberResponse response = system.processMember(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testFetchCircle() {
        final ManagementService system = prepareSystemService();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);

        final FetchCircleResponse response = system.fetchCircles(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
    }

    @Test
    public void testFetchCircleWithNullRequest() {
        final ManagementService system = prepareSystemService();
        final FetchCircleRequest request = null;

        final FetchCircleResponse response = system.fetchCircles(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFetchCircleWithEmptyRequest() {
        final ManagementService system = prepareSystemService();
        final FetchCircleRequest request = new FetchCircleRequest();

        final FetchCircleResponse response = system.fetchCircles(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedFetchCircle() {
        final ManagementService system = prepareFlawedSystemService();
        final FetchCircleRequest request = null;

        final FetchCircleResponse response = system.fetchCircles(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testProcessCircle() {
        final ManagementService system = prepareSystemService();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName("Test Circle");
        request.setMemberId(MEMBER_1_ID);

        final ProcessCircleResponse response = system.processCircle(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
    }

    @Test
    public void testProcessCircleWithNullRequest() {
        final ManagementService system = prepareSystemService();
        final ProcessCircleRequest request = null;

        final ProcessCircleResponse response = system.processCircle(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testProcessCircleWithEmptyRequest() {
        final ManagementService system = prepareSystemService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final ProcessCircleResponse response = system.processCircle(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedProcessCircle() {
        final ManagementService system = prepareFlawedSystemService();
        final ProcessCircleRequest request = null;

        final ProcessCircleResponse response = system.processCircle(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testFetchTrustee() {
        final ManagementService system = prepareSystemService();
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);

        final FetchTrusteeResponse response = system.fetchTrustees(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
    }

    @Test
    public void testFetchTrusteeWithNullRequest() {
        final ManagementService system = prepareSystemService();
        final FetchTrusteeRequest request = null;

        final FetchTrusteeResponse response = system.fetchTrustees(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFetchTrusteeWithEmptyRequest() {
        final ManagementService system = prepareSystemService();
        final FetchTrusteeRequest request = new FetchTrusteeRequest();

        final FetchTrusteeResponse response = system.fetchTrustees(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedFetchTrustee() {
        final ManagementService system = prepareFlawedSystemService();
        final FetchTrusteeRequest request = null;

        final FetchTrusteeResponse response = system.fetchTrustees(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testProcessTrustee() {
        final ManagementService system = prepareSystemService();
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.ALTER);
        request.setMemberId(MEMBER_2_ID);
        request.setCircleId(CIRCLE_1_ID);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = system.processTrustee(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
    }

    @Test
    public void testProcessTrusteeWithNullRequest() {
        final ManagementService system = prepareSystemService();
        final ProcessTrusteeRequest request = null;

        final ProcessTrusteeResponse response = system.processTrustee(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testProcessTrusteeWithEmptyRequest() {
        final ManagementService system = prepareSystemService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final ProcessTrusteeResponse response = system.processTrustee(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedProcessTrustee() {
        final ManagementService system = prepareFlawedSystemService();
        final ProcessTrusteeRequest request = null;

        final ProcessTrusteeResponse response = system.processTrustee(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }
}
