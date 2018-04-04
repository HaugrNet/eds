/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.Share;
import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SoapClientTest {

    private final Management management = new ManagementSoapClient(Base.URL);
    private final Share share = new ShareSoapClient(Base.URL);

    @Test
    public void testVersion() {
        final VersionResponse response = management.version();
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getVersion(), is("1.0-SNAPSHOT"));
    }

    @Test
    public void testSettings() {
        final SettingRequest request = Base.prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = management.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testFetchMembers() {
        final FetchMemberRequest request = Base.prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchMemberResponse response = management.fetchMembers(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testProcessMembers() {
        final String accountName = UUID.randomUUID().toString();

        final ProcessMemberRequest request = Base.prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(accountName);
        request.setNewCredential(accountName.getBytes(Charset.forName("UTF-8")));

        final ProcessMemberResponse response = management.processMember(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testFetchCircles() {
        final FetchCircleRequest request = Base.prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchCircleResponse response = management.fetchCircles(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testProcessCircles() {
        final String accountName = UUID.randomUUID().toString();

        final ProcessMemberRequest memberRequest = Base.prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        memberRequest.setAction(Action.CREATE);
        memberRequest.setNewAccountName(accountName);
        memberRequest.setNewCredential(accountName.getBytes(Charset.forName("UTF-8")));
        final ProcessMemberResponse memberResponse = management.processMember(memberRequest);
        assertThat(memberResponse.isOk(), is(true));

        final ProcessCircleRequest request = Base.prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName(accountName);
        request.setMemberId(memberResponse.getMemberId());
        request.setCircleName(UUID.randomUUID().toString());

        final ProcessCircleResponse response = management.processCircle(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testAddData() {
        final String accountName1 = UUID.randomUUID().toString();
        final String accountName2 = UUID.randomUUID().toString();
        final String memberId1 = Base.createAccount(management, accountName1);
        final String memberId2 = Base.createAccount(management, accountName2);
        final String circleId = Base.createCircle(management, accountName1, accountName1);
        Base.addTrustee(management, accountName1, circleId, memberId2);

        final String dataId1 = Base.addData(share, accountName2, circleId, "data1", Base.generateData(1024000));
        final String dataId2 = Base.addData(share, accountName1, circleId, "data2", Base.generateData(1024000));

        final FetchDataRequest request = Base.prepareRequest(FetchDataRequest.class, accountName1);
        request.setCircleId(circleId);
        final FetchDataResponse response = share.fetchData(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getRecords(), is(2L));
    }
}
