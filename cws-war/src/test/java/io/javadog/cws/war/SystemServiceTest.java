/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-war)
 * =============================================================================
 */
package io.javadog.cws.war;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SystemServiceTest extends BeanSetup {

    @Test
    public void testVersion() {
        final SystemService system = prepareSystemService();

        final VersionResponse response = system.version();
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testFlawedVersion() {
        final SystemService system = prepareFlawedSystemService();

        final VersionResponse response = system.version();
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR));
    }

    @Test
    public void testSettings() {
        final SystemService system = prepareSystemService();
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testSettingsWithNullRequest() {
        final SystemService system = prepareSystemService();
        final SettingRequest request = null;

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
    }

    @Test
    public void testSettingsWithEmptyRequest() {
        final SystemService system = prepareSystemService();
        final SettingRequest request = new SettingRequest();

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
    }

    @Test
    public void testFlawedSettings() {
        final SystemService system = prepareFlawedSystemService();
        final SettingRequest request = null;

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR));
    }

    @Test
    public void testFetchMembers() {
        final SystemService system = prepareSystemService();
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_1);

        final FetchMemberResponse response = system.fetchMembers(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testFetchMembersWithNullRequest() {
        final SystemService system = prepareSystemService();
        final FetchMemberRequest request = null;

        final FetchMemberResponse response = system.fetchMembers(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
    }

    @Test
    public void testFetchMembersWithEmptyRequest() {
        final SystemService system = prepareSystemService();
        final FetchMemberRequest request = new FetchMemberRequest();

        final FetchMemberResponse response = system.fetchMembers(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
    }

    @Test
    public void testFlawedFetchMembers() {
        final SystemService system = prepareFlawedSystemService();
        final FetchMemberRequest request = null;

        final FetchMemberResponse response = system.fetchMembers(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR));
    }

    @Test
    public void testProcessMember() {
        final SystemService system = prepareSystemService();
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.INVITE);
        request.setNewAccountName("new Account");

        final ProcessMemberResponse response = system.processMember(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testProcessMemberWithNullRequest() {
        final SystemService system = prepareSystemService();
        final ProcessMemberRequest request = null;

        final ProcessMemberResponse response = system.processMember(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
    }

    @Test
    public void testProcessMemberWithEmptyRequest() {
        final SystemService system = prepareSystemService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final ProcessMemberResponse response = system.processMember(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
    }

    @Test
    public void testFlawedProcessMember() {
        final SystemService system = prepareFlawedSystemService();
        final ProcessMemberRequest request = null;

        final ProcessMemberResponse response = system.processMember(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR));
    }

    @Test
    public void testFetchCircle() {
        final SystemService system = prepareSystemService();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);

        final FetchCircleResponse response = system.fetchCircles(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testFetchCircleWithNullRequest() {
        final SystemService system = prepareSystemService();
        final FetchCircleRequest request = null;

        final FetchCircleResponse response = system.fetchCircles(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
    }

    @Test
    public void testFetchCircleWithEmptyRequest() {
        final SystemService system = prepareSystemService();
        final FetchCircleRequest request = new FetchCircleRequest();

        final FetchCircleResponse response = system.fetchCircles(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
    }

    @Test
    public void testFlawedFetchCircle() {
        final SystemService system = prepareFlawedSystemService();
        final FetchCircleRequest request = null;

        final FetchCircleResponse response = system.fetchCircles(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR));
    }

    @Test
    public void testProcessCircle() {
        final SystemService system = prepareSystemService();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName("Test Circle");
        request.setMemberId(MEMBER_1_ID);

        final ProcessCircleResponse response = system.processCircle(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testProcessCircleWithNullRequest() {
        final SystemService system = prepareSystemService();
        final ProcessCircleRequest request = null;

        final ProcessCircleResponse response = system.processCircle(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
    }

    @Test
    public void testProcessCircleWithEmptyRequest() {
        final SystemService system = prepareSystemService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final ProcessCircleResponse response = system.processCircle(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
    }

    @Test
    public void testFlawedProcessCircle() {
        final SystemService system = prepareFlawedSystemService();
        final ProcessCircleRequest request = null;

        final ProcessCircleResponse response = system.processCircle(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR));
    }
}
