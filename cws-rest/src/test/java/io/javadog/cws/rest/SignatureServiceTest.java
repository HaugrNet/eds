/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
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
package io.javadog.cws.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class SignatureServiceTest extends BeanSetup {

    @Test
    void testSign() {
        final SignatureService service = prepareSignatureService(settings, entityManager);
        final SignRequest request = new SignRequest();

        final Response response = service.sign(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedSign() {
        final SignatureService service = prepareSignatureService();
        final SignRequest request = new SignRequest();

        final Response response = service.sign(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testVerify() {
        final SignatureService service = prepareSignatureService(settings, entityManager);
        final VerifyRequest request = new VerifyRequest();

        final Response response = service.verify(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedVerify() {
        final SignatureService service = prepareSignatureService();
        final VerifyRequest request = new VerifyRequest();

        final Response response = service.verify(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFetchSignatures() {
        final SignatureService service = prepareSignatureService(settings, entityManager);
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedFetchSignatures() {
        final SignatureService service = prepareSignatureService();
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }
}
