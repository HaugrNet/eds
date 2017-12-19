/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.Share;
import io.javadog.cws.api.System;
import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.api.responses.VersionResponse;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SoapClientTest {

    private final System system = new SystemSoapClient(Base.URL);
    private final Share share = new ShareSoapClient(Base.URL);

    @Test
    public void testVersion() {
        final VersionResponse response = system.version();
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getVersion(), is("0.8-SNAPSHOT"));
    }

    @Test
    public void testSettings() {
        final SettingRequest request = Base.prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testFetchMembers() {
        final FetchMemberRequest request = Base.prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchMemberResponse response = system.fetchMembers(request);
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testProcessMembers() {
        final String accountName = UUID.randomUUID().toString();

        final ProcessMemberRequest request = Base.prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(accountName);
        request.setNewCredential(accountName);

        final ProcessMemberResponse response = system.processMember(request);
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testFetchCircles() {
        final FetchCircleRequest request = Base.prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchCircleResponse response = system.fetchCircles(request);
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testProcessCircles() {
        final String accountName = UUID.randomUUID().toString();

        final ProcessMemberRequest memberRequest = Base.prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        memberRequest.setAction(Action.CREATE);
        memberRequest.setNewAccountName(accountName);
        memberRequest.setNewCredential(accountName);
        final ProcessMemberResponse memberResponse = system.processMember(memberRequest);
        assertThat(memberResponse.isOk(), is(true));

        final ProcessCircleRequest request = Base.prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName(accountName);
        request.setMemberId(memberResponse.getMemberId());
        request.setCircleName(UUID.randomUUID().toString());

        final ProcessCircleResponse response = system.processCircle(request);
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testSigningVerify() {
        final SignRequest signRequest = Base.prepareRequest(SignRequest.class, Constants.ADMIN_ACCOUNT);
        final byte[] document = UUID.randomUUID().toString().getBytes(Charset.forName("UTF-8"));
        signRequest.setData(document);
        final SignResponse signResponse = share.sign(signRequest);
        assertThat(signResponse.isOk(), is(true));
        assertThat(signResponse.getSignature(), is(not(nullValue())));

        final VerifyRequest verifyRequest = Base.prepareRequest(VerifyRequest.class, Constants.ADMIN_ACCOUNT);
        verifyRequest.setData(document);
        verifyRequest.setSignature(signResponse.getSignature());

        final VerifyResponse verifyResponse = share.verify(verifyRequest);
        assertThat(verifyResponse.isOk(), is(true));
        assertThat(verifyResponse.isVerified(), is(true));
    }
}
