/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.Management;
import net.haugr.eds.api.Share;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.dtos.Trustee;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.FetchSignatureRequest;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.requests.InventoryRequest;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessDataTypeRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.requests.SignRequest;
import net.haugr.eds.api.requests.VerifyRequest;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.FetchDataTypeResponse;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.api.responses.FetchSignatureResponse;
import net.haugr.eds.api.responses.FetchTrusteeResponse;
import net.haugr.eds.api.responses.InventoryResponse;
import net.haugr.eds.api.responses.MasterKeyResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessDataTypeResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.api.responses.SanityResponse;
import net.haugr.eds.api.responses.SettingResponse;
import net.haugr.eds.api.responses.SignResponse;
import net.haugr.eds.api.responses.VerifyResponse;
import net.haugr.eds.api.responses.VersionResponse;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
@EnabledIfEnvironmentVariable(named = "local.instance.running", matches = "true")
final class RestClientTest {

    private static final String URL = "http://localhost:8080/eds";
    private final Management restManagement = new ManagementRestClient(URL);
    private final Share restShare = new ShareRestClient(URL);

    @Test
    void testVersion() {
        final VersionResponse response = restManagement.version();
        assertNotNull(response);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals("2.0-SNAPSHOT", response.getVersion());
    }

    @Test
    void testSettings() {
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = restManagement.settings(request);
        assertNotNull(response);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testMasterKey() {
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());

        final MasterKeyResponse response = restManagement.masterKey(request);
        assertNotNull(response);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getReturnCode());
        assertEquals("MasterKey unlocked.", response.getReturnMessage());
    }

    @Test
    void testSanitized() {
        final SanityRequest request = prepareRequest(SanityRequest.class, Constants.ADMIN_ACCOUNT);

        final SanityResponse response = restManagement.sanitized(request);
        assertNotNull(response);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testFetchMembers() {
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchMemberResponse response = restManagement.fetchMembers(request);
        assertNotNull(response);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testProcessMembers() {
        final String accountName = UUID.randomUUID().toString();

        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(accountName);
        request.setNewCredential(accountName.getBytes(StandardCharsets.UTF_8));

        final ProcessMemberResponse response = restManagement.processMember(request);
        assertNotNull(response);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Member '" + accountName + "' was successfully added to EDS.", response.getReturnMessage());
    }

    @Test
    void testFetchCircles() {
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);

        final FetchCircleResponse response = restManagement.fetchCircles(request);
        assertNotNull(response);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testProcessCircles() {
        final String accountName = UUID.randomUUID().toString();

        final ProcessMemberRequest memberRequest = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        memberRequest.setAction(Action.CREATE);
        memberRequest.setNewAccountName(accountName);
        memberRequest.setNewCredential(accountName.getBytes(StandardCharsets.UTF_8));
        final ProcessMemberResponse memberResponse = restManagement.processMember(memberRequest);
        assertNotNull(memberResponse);
        assertTrue(memberResponse.isOk());

        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName(accountName);
        request.setMemberId(memberResponse.getMemberId());

        final ProcessCircleResponse response = restManagement.processCircle(request);
        assertNotNull(response);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Circle '" + accountName + "' was successfully created.", response.getReturnMessage());
    }

    @Test
    void testDataTypes() {
        final ProcessDataTypeRequest processRequest = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        processRequest.setTypeName("ObjectType");
        processRequest.setType("Object Mapping Rules");
        processRequest.setAction(Action.PROCESS);
        final ProcessDataTypeResponse processResponse = restShare.processDataType(processRequest);
        assertNotNull(processResponse);
        assertTrue(processResponse.isOk());
        assertEquals(processRequest.getTypeName(), processResponse.getDataType().getTypeName());

        final FetchDataTypeRequest fetchRequest = prepareRequest(FetchDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchDataTypeResponse fetchResponse = restShare.fetchDataTypes(fetchRequest);
        assertNotNull(fetchResponse);
        assertTrue(fetchResponse.isOk());
        // If the tests are running against a system with more DataTypes added,
        // this test will fail if hardcoded to 3. Hence, it expects at least 3.
        assertTrue(fetchResponse.getDataTypes().size() >= 3);
    }

    @Test
    void testAddData() {
        // Step 1; Create a new Circle with 2 Trustees
        final String accountName1 = UUID.randomUUID().toString();
        final String accountName2 = UUID.randomUUID().toString();
        createAccount(accountName1);
        final String memberId2 = createAccount(accountName2);
        final String circleId = createCircle(accountName1, accountName1);
        addTrustee(accountName1, circleId, memberId2);
        final List<Trustee> trustees = fetchTrustees(accountName1, circleId);
        assertEquals(2, trustees.size());

        // Step 2; Add 2 Data Objects
        final String data1 = toString(generateData());
        final String data2 = toString(generateData());
        final String dataId1 = addData(accountName2, circleId, "data1", toBytes(data1));
        final String dataId2 = addData(accountName1, circleId, "data2", toBytes(data2));

        // Step 3; Check the stored content of the Circle
        final FetchDataResponse response = readFolderContent(accountName1, circleId);
        assertNotNull(response);
        assertEquals(2L, response.getRecords());
        final byte[] read1 = readData(accountName2, dataId1);
        assertEquals(data1, toString(read1));
        final byte[] read2 = readData(accountName2, dataId2);
        assertEquals(data2, toString(read2));
    }

    @Test
    void testUpdateData() {
        final String dataName = "status";
        final String initContent = "NEW";
        final String updateContent = "ACCEPTED";

        final String accountName = UUID.randomUUID().toString();
        createAccount(accountName);
        final String circleId = createCircle(accountName, accountName);

        // Step 2; Add & Update Data Objects
        final String dataId = addData(accountName, circleId, dataName, toBytes(initContent));
        updateData(accountName, circleId, dataId, toBytes(updateContent));

        // Step 3; Check the stored content of the Circle
        final FetchDataResponse response = readFolderContent(accountName, circleId);
        assertNotNull(response);
        final byte[] read = readData(accountName, dataId);
        assertEquals(updateContent, toString(read));
    }

    @Test
    void testSignatures() {
        // 1. Generate a Signature
        final SignRequest signRequest = prepareRequest(SignRequest.class, Constants.ADMIN_ACCOUNT);
        final byte[] document = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        signRequest.setData(document);
        final SignResponse signResponse = restShare.sign(signRequest);
        assertTrue(signResponse.isOk());
        assertNotNull(signResponse.getSignature());

        // 2. Fetch Signatures to see that we have at least one
        final FetchSignatureRequest fetchRequest = prepareRequest(FetchSignatureRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchSignatureResponse fetchResponse = restShare.fetchSignatures(fetchRequest);
        assertNotNull(fetchResponse);
        assertTrue(fetchResponse.isOk());
        assertFalse(fetchResponse.getSignatures().isEmpty());

        // 3. Verify the Document using the created Signature
        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, Constants.ADMIN_ACCOUNT);
        verifyRequest.setData(document);
        verifyRequest.setSignature(signResponse.getSignature());

        final VerifyResponse verifyResponse = restShare.verify(verifyRequest);
        assertNotNull(verifyResponse);
        assertTrue(verifyResponse.isOk());
        assertTrue(verifyResponse.isVerified());
    }

    @Test
    void testInventory() {
        final InventoryRequest request = prepareRequest(InventoryRequest.class, Constants.ADMIN_ACCOUNT);
        final InventoryResponse response = restManagement.inventory(request);
        assertTrue(response.isOk());
    }

    // =========================================================================
    // Internal functionality to help with the test setup
    // =========================================================================

    static <A extends Authentication> A prepareRequest(final Class<A> clazz, final String account) {
        try {
            final A request = clazz.getConstructor().newInstance();

            request.setAccountName(account);
            request.setCredential(account.getBytes(StandardCharsets.UTF_8));
            request.setCredentialType(CredentialType.PASSPHRASE);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RESTClientException("Cannot instantiate Request Object", e);
        }
    }

    private String createAccount(final String accountName) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(accountName);
        request.setNewCredential(accountName.getBytes(StandardCharsets.UTF_8));

        final ProcessMemberResponse response = restManagement.processMember(request);
        throwIfFailed(response);

        return response.getMemberId();
    }

    private String createCircle(final String circleName, final String accountName) {
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, accountName);
        request.setAction(Action.CREATE);
        request.setCircleName(circleName);

        final ProcessCircleResponse response = restManagement.processCircle(request);
        throwIfFailed(response);

        return response.getCircleId();
    }

    private void addTrustee(final String circleAdminAccount, final String circleId, final String memberId) {
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, circleAdminAccount);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setMemberId(memberId);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = restManagement.processTrustee(request);
        throwIfFailed(response);
    }

    private List<Trustee> fetchTrustees(final String memberAccount, final String circleId) {
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, memberAccount);
        request.setCircleId(circleId);
        final FetchTrusteeResponse response = restManagement.fetchTrustees(request);

        throwIfFailed(response);
        return response.getTrustees();
    }

    private String addData(final String accountName, final String circleId, final String dataName, final byte[] data) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, accountName);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setDataName(dataName);
        request.setData(data);

        final ProcessDataResponse response = restShare.processData(request);
        throwIfFailed(response);

        return response.getDataId();
    }

    private void updateData(final String accountName, final String circleId, final String dataId, final byte[] data) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, accountName);
        request.setAction(Action.UPDATE);
        request.setCircleId(circleId);
        request.setDataId(dataId);
        request.setDataName("status");
        request.setData(data);

        final ProcessDataResponse response = restShare.processData(request);
        throwIfFailed(response);
    }

    private FetchDataResponse readFolderContent(final String accountName, final String circleId, final String... folderId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, accountName);
        request.setCircleId(circleId);
        if ((folderId != null) && (folderId.length == 1)) {
            request.setDataId(folderId[0]);
        }

        final FetchDataResponse response = restShare.fetchData(request);
        throwIfFailed(response);

        return response;
    }

    private byte[] readData(final String accountName, final String dataId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, accountName);
        request.setDataId(dataId);

        final FetchDataResponse response = restShare.fetchData(request);
        throwIfFailed(response);

        return response.getData();
    }

    private static byte[] generateData() {
        final byte[] data = new byte[1024000];
        final SecureRandom random = new SecureRandom();
        random.nextBytes(data);

        return data;
    }

    private static byte[] toBytes(final String str) {
        return str.getBytes(Charset.defaultCharset());
    }

    private static String toString(final byte[] bytes) {
        return new String(bytes, Charset.defaultCharset());
    }

    private static void throwIfFailed(final EDSResponse response) {
        if (!response.isOk()) {
            throw new RESTClientException(response.getReturnMessage());
        }
    }
}
