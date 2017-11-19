/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.System;
import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.Authentication;
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

import java.lang.reflect.InvocationTargetException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class SystemSoapClientTest {

    private static final String SYSTEM_URL = "http://localhost:8080/cws/system?wsdl";

    @Test
    public void testVersion() {
        final System system = new SystemSoapClient(SYSTEM_URL);
        final VersionResponse response = system.version();
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getVersion(), is("0.7-SNAPSHOT"));
    }

    @Test
    public void testSettings() {
        final System system = new SystemSoapClient(SYSTEM_URL);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testFetchMembers() {
        final System system = new SystemSoapClient(SYSTEM_URL);
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchMemberResponse response = system.fetchMembers(request);
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testProcessMembers() {
        final System system = new SystemSoapClient(SYSTEM_URL);
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName("newMember");
        request.setNewCredential("Password");

        final ProcessMemberResponse response = system.processMember(request);
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testFetchCircles() {
        final System system = new SystemSoapClient(SYSTEM_URL);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchCircleResponse response = system.fetchCircles(request);
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testProcessCircles() {
        final System system = new SystemSoapClient(SYSTEM_URL);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName("Circle Name");

        final ProcessCircleResponse response = system.processCircle(request);
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    // =========================================================================
    // Internal methods
    // =========================================================================

    private static <A extends Authentication> A prepareRequest(final Class<A> clazz, final String account) {
        try {
            final A request = clazz.getConstructor().newInstance();

            request.setAccountName(account);
            request.setCredential(account);
            request.setCredentialType(CredentialType.PASSPHRASE);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CWSClientException("Cannot instantiate Request Object", e);
        }
    }
}
