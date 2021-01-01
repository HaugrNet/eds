/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class ShareServiceTest extends BeanSetup {

    @Test
    void testProcessDataType() {
        final ShareService service = prepareShareService(settings, entityManager);
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        request.setTypeName("TestType");
        request.setType("Test Type Value");

        final ProcessDataTypeResponse response = service.processDataType(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Data Type 'TestType' was successfully processed.", response.getReturnMessage());
        assertNotNull(response.getDataType());
        assertEquals("TestType", response.getDataType().getTypeName());
        assertEquals("Test Type Value", response.getDataType().getType());
    }

    @Test
    void testProcessDataTypeWithNullRequest() {
        final ShareService service = prepareShareService(settings, entityManager);

        final ProcessDataTypeResponse response = service.processDataType(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessDataTypeWithEmptyRequest() {
        final ShareService service = prepareShareService(settings, entityManager);
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final ProcessDataTypeResponse response = service.processDataType(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedProcessDataType() {
        final ShareService service = prepareShareService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final ProcessDataTypeResponse response = service.processDataType(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchDataTypes() {
        final ShareService service = prepareShareService(settings, entityManager);
        final FetchDataTypeRequest request = prepareRequest(FetchDataTypeRequest.class, MEMBER_1);

        final FetchDataTypeResponse response = service.fetchDataTypes(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(2, response.getDataTypes().size());
        assertEquals("folder", response.getDataTypes().get(0).getTypeName());
        assertEquals("Folder", response.getDataTypes().get(0).getType());
        assertEquals("data", response.getDataTypes().get(1).getTypeName());
        assertEquals("Data Object", response.getDataTypes().get(1).getType());
    }

    @Test
    void testFetchDataTypesWithNullRequest() {
        final ShareService service = prepareShareService(settings, entityManager);

        final FetchDataTypeResponse response = service.fetchDataTypes(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchDataTypesWithEmptyRequest() {
        final ShareService service = prepareShareService(settings, entityManager);
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final FetchDataTypeResponse response = service.fetchDataTypes(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedFetchDataTypes() {
        final ShareService service = prepareShareService();
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final FetchDataTypeResponse response = service.fetchDataTypes(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessData() {
        final ShareService service = prepareShareService(settings, entityManager);
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setData("alpha beta gamma".getBytes(settings.getCharset()));
        request.setCircleId(CIRCLE_1_ID);
        request.setDataName("Data Name");
        request.setTypeName("data");

        final ProcessDataResponse response = service.processData(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Data Object 'Data Name' was successfully added to the Circle '" + CIRCLE_1 + "'.", response.getReturnMessage());
        assertNotNull(response.getDataId());
    }

    @Test
    void testProcessDataWithNullRequest() {
        final ShareService service = prepareShareService(settings, entityManager);

        final ProcessDataResponse response = service.processData(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testProcessDataWithEmptyRequest() {
        final ShareService service = prepareShareService(settings, entityManager);
        final ProcessDataRequest request = new ProcessDataRequest();

        final ProcessDataResponse response = service.processData(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedProcessData() {
        final ShareService service = prepareShareService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final ProcessDataResponse response = service.processData(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchData() {
        final ShareService service = prepareShareService(settings, entityManager);
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);

        final FetchDataResponse response = service.fetchData(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertTrue(response.getMetadata().isEmpty());
    }

    @Test
    void testFetchDataWithNullRequest() {
        final ShareService service = prepareShareService(settings, entityManager);

        final FetchDataResponse response = service.fetchData(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchDataWithEmptyRequest() {
        final ShareService service = prepareShareService(settings, entityManager);
        final FetchDataRequest request = new FetchDataRequest();

        final FetchDataResponse response = service.fetchData(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedFetchData() {
        final ShareService service = prepareShareService();
        final FetchDataRequest request = new FetchDataRequest();

        final FetchDataResponse response = service.fetchData(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testSignatures() {
        final ShareService service = prepareShareService(settings, entityManager);
        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        final byte[] data = "alpha".getBytes(settings.getCharset());
        signRequest.setData(data);
        final SignResponse signResponse = service.sign(signRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), signResponse.getReturnCode());

        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, MEMBER_2);
        verifyRequest.setData(data);
        verifyRequest.setSignature(signResponse.getSignature());
        final VerifyResponse verifyResponse = service.verify(verifyRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), verifyResponse.getReturnCode());
    }

    @Test
    void testSignWithNullRequest() {
        final ShareService service = prepareShareService(settings, entityManager);

        final SignResponse response = service.sign(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testSignWithEmptyRequest() {
        final ShareService service = prepareShareService(settings, entityManager);
        final SignRequest request = new SignRequest();

        final SignResponse response = service.sign(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedSign() {
        final ShareService service = prepareShareService();
        final SignRequest request = new SignRequest();

        final SignResponse response = service.sign(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testVerifyWithNullRequest() {
        final ShareService service = prepareShareService(settings, entityManager);

        final VerifyResponse response = service.verify(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testVerifyWithEmptyRequest() {
        final ShareService service = prepareShareService(settings, entityManager);
        final VerifyRequest request = new VerifyRequest();

        final VerifyResponse response = service.verify(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedVerify() {
        final ShareService service = prepareShareService();
        final VerifyRequest request = new VerifyRequest();

        final VerifyResponse response = service.verify(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchSignatures() {
        final ShareService service = prepareShareService(settings, entityManager);
        final FetchSignatureRequest request = prepareRequest(FetchSignatureRequest.class, MEMBER_1);

        final FetchSignatureResponse response = service.fetchSignatures(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertTrue(response.getSignatures().isEmpty());
    }

    @Test
    void testFetchSignaturesWithNullRequest() {
        final ShareService service = prepareShareService(settings, entityManager);

        final FetchSignatureResponse response = service.fetchSignatures(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFetchSignaturesWithEmptyRequest() {
        final ShareService service = prepareShareService(settings, entityManager);
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final FetchSignatureResponse response = service.fetchSignatures(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    void testFlawedFetchSignatures() {
        final ShareService service = prepareShareService();
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final FetchSignatureResponse response = service.fetchSignatures(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }
}
