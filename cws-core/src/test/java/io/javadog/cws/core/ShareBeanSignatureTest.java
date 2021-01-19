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
package io.javadog.cws.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;
import java.util.Date;

/**
 * <p>This Test Class, is testing the following Service Classes in one, as they
 * are all fairly small but also connected.</p>
 *
 * <ul>
 *   <li>SignService</li>
 *   <li>VerifyService</li>
 *   <li>FetchSignatureService</li>
 * </ul>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class ShareBeanSignatureTest extends DatabaseSetup {

    @Test
    void testSignWithNullRequest() {
        final ShareBean bean = prepareShareBean();

        final SignResponse response = bean.sign(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot Process a NULL Object.", response.getReturnMessage());
    }

    @Test
    void testVerifyWithNullRequest() {
        final ShareBean bean = prepareShareBean();

        final VerifyResponse response = bean.verify(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot Process a NULL Object.", response.getReturnMessage());
    }

    @Test
    void testFetchSignaturesWithNullRequest() {
        final ShareBean bean = prepareShareBean();

        final FetchSignatureResponse response = bean.fetchSignatures(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot Process a NULL Object.", response.getReturnMessage());
    }

    @Test
    void testSignVerifyFetch() {
        final ShareBean bean = prepareShareBean();
        final byte[] data = generateData(1048576);

        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        signRequest.setData(data);
        final SignResponse signResponse = bean.sign(signRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), signResponse.getReturnCode());

        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, MEMBER_2);
        verifyRequest.setData(data);
        verifyRequest.setSignature(signResponse.getSignature());
        final VerifyResponse verifyResponse = bean.verify(verifyRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), verifyResponse.getReturnCode());
        assertTrue(verifyResponse.isVerified());

        final FetchSignatureRequest fetchRequest = prepareRequest(FetchSignatureRequest.class, MEMBER_1);
        final FetchSignatureResponse fetchResponse = bean.fetchSignatures(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(1, fetchResponse.getSignatures().size());
        assertEquals(Long.valueOf(1L), fetchResponse.getSignatures().get(0).getVerifications());
    }

    @Test
    void testSignatureExpiringIn5Minutes() {
        final ShareBean bean = prepareShareBean();
        final byte[] data = generateData(1048576);

        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        signRequest.setData(data);
        // Let our new Signature expire in 5 minutes
        signRequest.setExpires(new Date(new Date().getTime() + 300000L));
        final SignResponse signResponse = bean.sign(signRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), signResponse.getReturnCode());

        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, MEMBER_2);
        verifyRequest.setData(data);
        verifyRequest.setSignature(signResponse.getSignature());
        final VerifyResponse verifyResponse = bean.verify(verifyRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), verifyResponse.getReturnCode());
        assertEquals("The signature has successfully been verified.", verifyResponse.getReturnMessage());
        assertTrue(verifyResponse.isVerified());
    }

    @Test
    void testDuplicateSigning() {
        final ShareBean bean = prepareShareBean();
        final byte[] data = generateData(1048576);

        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        signRequest.setData(data);
        // Let our new Signature expire in 5 minutes
        signRequest.setExpires(new Date(new Date().getTime() + 300000L));
        final SignResponse signResponse = bean.sign(signRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), signResponse.getReturnCode());

        signRequest.setCredential(crypto.stringToBytes(MEMBER_1));
        final SignResponse duplicateResponse = bean.sign(signRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), duplicateResponse.getReturnCode());
        assertEquals("This document has already been signed.", duplicateResponse.getReturnMessage());
    }

    @Test
    void testExpiredSignature() {
        final ShareBean bean = prepareShareBean();
        final byte[] data = generateData(1048576);

        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        signRequest.setData(data);
        signRequest.setExpires(new Date());
        final SignResponse signResponse = bean.sign(signRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), signResponse.getReturnCode());

        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, MEMBER_2);
        verifyRequest.setData(data);
        verifyRequest.setSignature(signResponse.getSignature());

        final VerifyResponse response = bean.verify(verifyRequest);
        assertEquals(ReturnCode.SIGNATURE_WARNING.getCode(), response.getReturnCode());
        assertEquals("The Signature has expired.", response.getReturnMessage());
    }

    @Test
    void testCorrectSignatureWrongData() {
        final ShareBean bean = prepareShareBean();
        final byte[] data = generateData(1048576);

        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        signRequest.setData(data);
        final SignResponse signResponse = bean.sign(signRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), signResponse.getReturnCode());

        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, MEMBER_2);
        final byte[] wrongData = generateData(524288);
        verifyRequest.setData(wrongData);
        verifyRequest.setSignature(signResponse.getSignature());
        final VerifyResponse verifyResponse = bean.verify(verifyRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), verifyResponse.getReturnCode());
        assertFalse(verifyResponse.isVerified());
    }

    @Test
    void testInvalidSignature() {
        final ShareBean bean = prepareShareBean();
        final VerifyRequest request = prepareRequest(VerifyRequest.class, MEMBER_1);
        request.setData(generateData(524288));
        request.setSignature("VYU8uIyr54AZGeM4aUilgEItfI39/b4YpFcry8ByJYVuJoI0gxNLiw9CMCaocOfXyGkmQJuI4KvL1lhNN6jnsY51OYxsxcJKUBgMnGMRdp9mr+ryiduotTYeD9Z+IyWXdUlQ9W3N/TX1uqLwVCh9qrngXtnOXx5rnZrWybQPsoLEnVOXvkL94Et0EcIUe6spRbaR+8I4oCtGToLcMCZdD32z6suhIfqz9UFiU10W01T2ebNV4SuTf56RSZ1vyWix6C8GJhwLqWE697femoqBWh1UYgKsi5x6d1SYC7ZWSxVj61PpPJ3MfzAxVc5rqJDk1og3zfciDWPMJmF4aJ60Sg==");

        final VerifyResponse response = bean.verify(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("It was not possible to find the Signature.", response.getReturnMessage());
    }
}
