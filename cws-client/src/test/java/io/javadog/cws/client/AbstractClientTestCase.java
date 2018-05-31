/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.Share;
import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.Trustee;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.api.responses.VersionResponse;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class AbstractClientTestCase {

    abstract Management getManagement();
    abstract Share getShare();

    @Test
    public void testVersion() {
        final VersionResponse response = getManagement().version();
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getHttpCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getVersion(), is("1.1-SNAPSHOT"));
    }

    @Test
    public void testSettings() {
        final SettingRequest request = Base.prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = getManagement().settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getHttpCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testMasterKey() {
        final MasterKeyRequest request = Base.prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());

        final MasterKeyResponse response = getManagement().masterKey(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getHttpCode()));
        assertThat(response.getReturnMessage(), is("MasterKey unlocked."));
    }

    @Test
    public void testSanitized() {
        final SanityRequest request = Base.prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);

        final SanityResponse response = getManagement().sanitized(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getHttpCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testFetchMembers() {
        final FetchMemberRequest request = Base.prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchMemberResponse response = getManagement().fetchMembers(request);
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

        final ProcessMemberResponse response = getManagement().processMember(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testFetchCircles() {
        final FetchCircleRequest request = Base.prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchCircleResponse response = getManagement().fetchCircles(request);
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
        final ProcessMemberResponse memberResponse = getManagement().processMember(memberRequest);
        assertThat(memberResponse.isOk(), is(true));

        final ProcessCircleRequest request = Base.prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName(accountName);
        request.setMemberId(memberResponse.getMemberId());
        request.setCircleName(UUID.randomUUID().toString());

        final ProcessCircleResponse response = getManagement().processCircle(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testDataTypes() {
        final ProcessDataTypeRequest processRequest = Base.prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        processRequest.setTypeName("ObjectType");
        processRequest.setType("Object Mapping Rules");
        processRequest.setAction(Action.PROCESS);
        final ProcessDataTypeResponse processResponse = getShare().processDataType(processRequest);
        assertThat(processResponse.isOk(), is(true));
        assertThat(processResponse.getDataType().getTypeName(), is(processRequest.getTypeName()));

        final FetchDataTypeRequest fetchRequest = Base.prepareRequest(FetchDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchDataTypeResponse fetchResponse = getShare().fetchDataTypes(fetchRequest);
        assertThat(fetchResponse.isOk(), is(true));
        assertThat(fetchResponse.getDataTypes().size(), is(3));
    }

    @Test
    public void testAddData() {
        // Step 1; Create a new Circle with 2 Trustees
        final String accountName1 = UUID.randomUUID().toString();
        final String accountName2 = UUID.randomUUID().toString();
        Base.createAccount(getManagement(), accountName1);
        final String memberId2 = Base.createAccount(getManagement(), accountName2);
        final String circleId = Base.createCircle(getManagement(), accountName1, accountName1);
        Base.addTrustee(getManagement(), accountName1, circleId, memberId2);
        final List<Trustee> trustees = Base.fetchTrustees(getManagement(), accountName1, circleId);
        assertThat(trustees.size(), is(2));

        // Step 2; Add 2 Data Objects
        final String data1 = Base.toString(Base.generateData(1024000));
        final String data2 = Base.toString(Base.generateData(1024000));
        final String dataId1 = Base.addData(getShare(), accountName2, circleId, "data1", Base.toBytes(data1));
        final String dataId2 = Base.addData(getShare(), accountName1, circleId, "data2", Base.toBytes(data2));

        // Step 3; Check the stored content of the Circle
        final FetchDataResponse response = Base.readFolderContent(getShare(), accountName1, circleId);
        assertThat(response.getRecords(), is(2L));
        final byte[] read1 = Base.readData(getShare(), accountName2, dataId1);
        assertThat(Base.toString(read1), is(data1));
        final byte[] read2 = Base.readData(getShare(), accountName2, dataId2);
        assertThat(Base.toString(read2), is(data2));
    }

    @Test
    public void testSignatures() {
        // 1. Generate a Signature
        final SignRequest signRequest = Base.prepareRequest(SignRequest.class, Constants.ADMIN_ACCOUNT);
        final byte[] document = UUID.randomUUID().toString().getBytes(Charset.forName("UTF-8"));
        signRequest.setData(document);
        final SignResponse signResponse = getShare().sign(signRequest);
        assertThat(signResponse.isOk(), is(true));
        assertThat(signResponse.getSignature(), is(not(nullValue())));

        // 2. Fetch Signatures, to see that we have at least one
        final FetchSignatureRequest fetchRequest = Base.prepareRequest(FetchSignatureRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchSignatureResponse fetchResponse = getShare().fetchSignatures(fetchRequest);
        assertThat(fetchResponse.isOk(), is(true));
        assertThat(fetchResponse.getSignatures().isEmpty(), is(false));

        // 3. Verify the Document, using the created Signature
        final VerifyRequest verifyRequest = Base.prepareRequest(VerifyRequest.class, Constants.ADMIN_ACCOUNT);
        verifyRequest.setData(document);
        verifyRequest.setSignature(signResponse.getSignature());

        final VerifyResponse verifyResponse = getShare().verify(verifyRequest);
        assertThat(verifyResponse.isOk(), is(true));
        assertThat(verifyResponse.isVerified(), is(true));
    }
}
