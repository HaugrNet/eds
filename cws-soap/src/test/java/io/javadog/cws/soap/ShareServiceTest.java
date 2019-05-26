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
package io.javadog.cws.soap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ShareServiceTest extends BeanSetup {

    @Test
    public void testProcessDataType() {
        final ShareService service = prepareShareService();
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        request.setTypeName("TestType");
        request.setType("Test Type Value");

        final ProcessDataTypeResponse response = service.processDataType(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertNotNull(response.getDataType());
        assertEquals("TestType", response.getDataType().getTypeName());
        assertEquals("Test Type Value", response.getDataType().getType());
    }

    @Test
    public void testProcessDataTypeWithNullRequest() {
        final ShareService service = prepareShareService();
        final ProcessDataTypeRequest request = null;

        final ProcessDataTypeResponse response = service.processDataType(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testProcessDataTypeWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final ProcessDataTypeResponse response = service.processDataType(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedProcessDataType() {
        final ShareService service = prepareFlawedShareService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final ProcessDataTypeResponse response = service.processDataType(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchDataTypes() {
        final ShareService service = prepareShareService();
        final FetchDataTypeRequest request = prepareRequest(FetchDataTypeRequest.class, MEMBER_1);

        final FetchDataTypeResponse response = service.fetchDataTypes(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(2, response.getDataTypes().size());
        assertEquals("data", response.getDataTypes().get(0).getTypeName());
        assertEquals("Data Object", response.getDataTypes().get(0).getType());
        assertEquals("folder", response.getDataTypes().get(1).getTypeName());
        assertEquals("Folder", response.getDataTypes().get(1).getType());
    }

    @Test
    public void testFetchDataTypesWithNullRequest() {
        final ShareService service = prepareShareService();
        final FetchDataTypeRequest request = null;

        final FetchDataTypeResponse response = service.fetchDataTypes(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchDataTypesWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final FetchDataTypeResponse response = service.fetchDataTypes(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedFetchDataTypes() {
        final ShareService service = prepareFlawedShareService();
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final FetchDataTypeResponse response = service.fetchDataTypes(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testProcessData() {
        final ShareService service = prepareShareService();
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setData("alfa beta gamma".getBytes(settings.getCharset()));
        request.setCircleId(CIRCLE_1_ID);
        request.setDataName("Data Name");
        request.setTypeName("data");

        final ProcessDataResponse response = service.processData(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertNotNull(response.getDataId());
    }

    @Test
    public void testProcessDataWithNullRequest() {
        final ShareService service = prepareShareService();
        final ProcessDataRequest request = null;

        final ProcessDataResponse response = service.processData(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testProcessDataWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final ProcessDataResponse response = service.processData(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedProcessData() {
        final ShareService service = prepareFlawedShareService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final ProcessDataResponse response = service.processData(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchData() {
        final ShareService service = prepareShareService();
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);

        final FetchDataResponse response = service.fetchData(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertTrue(response.getMetadata().isEmpty());
    }

    @Test
    public void testFetchDataWithNullRequest() {
        final ShareService service = prepareShareService();
        final FetchDataRequest request = null;

        final FetchDataResponse response = service.fetchData(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchDataWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final FetchDataRequest request = new FetchDataRequest();

        final FetchDataResponse response = service.fetchData(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedFetchData() {
        final ShareService service = prepareFlawedShareService();
        final FetchDataRequest request = new FetchDataRequest();

        final FetchDataResponse response = service.fetchData(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testSignatures() {
        final ShareService service = prepareShareService();
        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        final byte[] data = "alfa".getBytes(settings.getCharset());
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
    public void testSignWithNullRequest() {
        final ShareService service = prepareShareService();
        final SignRequest request = null;

        final SignResponse response = service.sign(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testSignWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final SignRequest request = new SignRequest();

        final SignResponse response = service.sign(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedSign() {
        final ShareService service = prepareFlawedShareService();
        final SignRequest request = new SignRequest();

        final SignResponse response = service.sign(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testVerifyWithNullRequest() {
        final ShareService service = prepareShareService();
        final VerifyRequest request = null;

        final VerifyResponse response = service.verify(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testVerifyWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final VerifyRequest request = new VerifyRequest();

        final VerifyResponse response = service.verify(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedVerify() {
        final ShareService service = prepareFlawedShareService();
        final VerifyRequest request = new VerifyRequest();

        final VerifyResponse response = service.verify(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchSignatures() {
        final ShareService service = prepareShareService();
        final FetchSignatureRequest request = prepareRequest(FetchSignatureRequest.class, MEMBER_1);

        final FetchSignatureResponse response = service.fetchSignatures(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertTrue(response.getSignatures().isEmpty());
    }

    @Test
    public void testFetchSignaturesWithNullRequest() {
        final ShareService service = prepareShareService();
        final FetchSignatureRequest request = null;

        final FetchSignatureResponse response = service.fetchSignatures(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFetchSignaturesWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final FetchSignatureResponse response = service.fetchSignatures(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
    }

    @Test
    public void testFlawedFetchSignatures() {
        final ShareService service = prepareFlawedShareService();
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final FetchSignatureResponse response = service.fetchSignatures(request);
        assertEquals(ReturnCode.ERROR.getCode(), response.getReturnCode());
    }
}
