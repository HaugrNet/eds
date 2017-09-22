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
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SystemServiceTest extends DatabaseSetup {

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
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, "member1");

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
        request.setAccountName("new Account");

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
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, "member1");

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
        request.setMemberId("073dcc8f-ffa6-4cda-8d61-09ba9441e78e");

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

    // =========================================================================
    // Internal Methods
    // =========================================================================

    private static SystemService prepareFlawedSystemService() {
        try {
            final SystemService service = SystemService.class.getConstructor().newInstance();
            setField(service, "bean", null);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private SystemService prepareSystemService() {
        try {
            final SystemBean bean = SystemBean.class.getConstructor().newInstance();
            setField(bean, "entityManager", entityManager);

            final SystemService service = SystemService.class.getConstructor().newInstance();
            setField(service, "bean", bean);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private static void setField(final Object instance, final String fieldName, final Object value) {
        try {
            final Class<?> clazz = instance.getClass();
            final Field field;

            field = clazz.getDeclaredField(fieldName);
            final boolean accessible = field.isAccessible();

            field.setAccessible(true);
            field.set(instance, value);
            field.setAccessible(accessible);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot set Field", e);
        }
    }
}
