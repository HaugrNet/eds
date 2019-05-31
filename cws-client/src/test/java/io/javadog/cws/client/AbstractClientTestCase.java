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
package io.javadog.cws.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
abstract class AbstractClientTestCase {

    abstract Management getManagement();

    abstract Share getShare();

    @Test
    void testVersion() {
        final VersionResponse response = getManagement().version();
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals("1.1-SNAPSHOT", response.getVersion());
    }

    @Test
    void testSettings() {
        final SettingRequest request = Base.prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = getManagement().settings(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testMasterKey() {
        final MasterKeyRequest request = Base.prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());

        final MasterKeyResponse response = getManagement().masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getReturnCode());
        assertEquals("MasterKey unlocked.", response.getReturnMessage());
    }

    @Test
    void testSanitized() {
        final SanityRequest request = Base.prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);

        final SanityResponse response = getManagement().sanitized(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testFetchMembers() {
        final FetchMemberRequest request = Base.prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchMemberResponse response = getManagement().fetchMembers(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testProcessMembers() {
        final String accountName = UUID.randomUUID().toString();

        final ProcessMemberRequest request = Base.prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(accountName);
        request.setNewCredential(accountName.getBytes(StandardCharsets.UTF_8));

        final ProcessMemberResponse response = getManagement().processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testFetchCircles() {
        final FetchCircleRequest request = Base.prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchCircleResponse response = getManagement().fetchCircles(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testProcessCircles() {
        final String accountName = UUID.randomUUID().toString();

        final ProcessMemberRequest memberRequest = Base.prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        memberRequest.setAction(Action.CREATE);
        memberRequest.setNewAccountName(accountName);
        memberRequest.setNewCredential(accountName.getBytes(StandardCharsets.UTF_8));
        final ProcessMemberResponse memberResponse = getManagement().processMember(memberRequest);
        assertTrue(memberResponse.isOk());

        final ProcessCircleRequest request = Base.prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName(accountName);
        request.setMemberId(memberResponse.getMemberId());
        request.setCircleName(UUID.randomUUID().toString());

        final ProcessCircleResponse response = getManagement().processCircle(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testDataTypes() {
        final ProcessDataTypeRequest processRequest = Base.prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        processRequest.setTypeName("ObjectType");
        processRequest.setType("Object Mapping Rules");
        processRequest.setAction(Action.PROCESS);
        final ProcessDataTypeResponse processResponse = getShare().processDataType(processRequest);
        assertTrue(processResponse.isOk());
        assertEquals(processRequest.getTypeName(), processResponse.getDataType().getTypeName());

        final FetchDataTypeRequest fetchRequest = Base.prepareRequest(FetchDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchDataTypeResponse fetchResponse = getShare().fetchDataTypes(fetchRequest);
        assertTrue(fetchResponse.isOk());
        // If the tests is running against a system with more DataTypes added,
        // this test will fail if hardcoded to 3, hence it expects at least 3.
        assertTrue(fetchResponse.getDataTypes().size() >= 3);
    }

    @Test
    void testAddData() {
        // Step 1; Create a new Circle with 2 Trustees
        final String accountName1 = UUID.randomUUID().toString();
        final String accountName2 = UUID.randomUUID().toString();
        Base.createAccount(getManagement(), accountName1);
        final String memberId2 = Base.createAccount(getManagement(), accountName2);
        final String circleId = Base.createCircle(getManagement(), accountName1, accountName1);
        Base.addTrustee(getManagement(), accountName1, circleId, memberId2);
        final List<Trustee> trustees = Base.fetchTrustees(getManagement(), accountName1, circleId);
        assertEquals(2, trustees.size());

        // Step 2; Add 2 Data Objects
        final String data1 = Base.toString(Base.generateData(1024000));
        final String data2 = Base.toString(Base.generateData(1024000));
        final String dataId1 = Base.addData(getShare(), accountName2, circleId, "data1", Base.toBytes(data1));
        final String dataId2 = Base.addData(getShare(), accountName1, circleId, "data2", Base.toBytes(data2));

        // Step 3; Check the stored content of the Circle
        final FetchDataResponse response = Base.readFolderContent(getShare(), accountName1, circleId);
        assertEquals(2L, response.getRecords());
        final byte[] read1 = Base.readData(getShare(), accountName2, dataId1);
        assertEquals(data1, Base.toString(read1));
        final byte[] read2 = Base.readData(getShare(), accountName2, dataId2);
        assertEquals(data2, Base.toString(read2));
    }

    @Test
    void testSignatures() {
        // 1. Generate a Signature
        final SignRequest signRequest = Base.prepareRequest(SignRequest.class, Constants.ADMIN_ACCOUNT);
        final byte[] document = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        signRequest.setData(document);
        final SignResponse signResponse = getShare().sign(signRequest);
        assertTrue(signResponse.isOk());
        assertNotNull(signResponse.getSignature());

        // 2. Fetch Signatures, to see that we have at least one
        final FetchSignatureRequest fetchRequest = Base.prepareRequest(FetchSignatureRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchSignatureResponse fetchResponse = getShare().fetchSignatures(fetchRequest);
        assertTrue(fetchResponse.isOk());
        assertFalse(fetchResponse.getSignatures().isEmpty());

        // 3. Verify the Document, using the created Signature
        final VerifyRequest verifyRequest = Base.prepareRequest(VerifyRequest.class, Constants.ADMIN_ACCOUNT);
        verifyRequest.setData(document);
        verifyRequest.setSignature(signResponse.getSignature());

        final VerifyResponse verifyResponse = getShare().verify(verifyRequest);
        assertTrue(verifyResponse.isOk());
        assertTrue(verifyResponse.isVerified());
    }
}
