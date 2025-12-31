/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.FetchSignatureRequest;
import net.haugr.eds.api.requests.SignRequest;
import net.haugr.eds.api.requests.VerifyRequest;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class SignatureServiceTest extends DatabaseSetup {

    @Test
    void testSign() {
        final SignatureService service = new SignatureService(prepareShareBean());
        final SignRequest request = new SignRequest();

        try (final Response response = service.sign(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testFlawedSign() {
        final SignatureService service = new SignatureService();
        final SignRequest request = new SignRequest();

        try (final Response response = service.sign(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testVerify() {
        final SignatureService service = new SignatureService(prepareShareBean());
        final VerifyRequest request = new VerifyRequest();

        try (final Response response = service.verify(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testFlawedVerify() {
        final SignatureService service = new SignatureService();
        final VerifyRequest request = new VerifyRequest();

        try (final Response response = service.verify(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testFetchSignatures() {
        final SignatureService service = new SignatureService(prepareShareBean());
        final FetchSignatureRequest request = new FetchSignatureRequest();

        try (final Response response = service.fetch(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testFlawedFetchSignatures() {
        final SignatureService service = new SignatureService();
        final FetchSignatureRequest request = new FetchSignatureRequest();

        try (final Response response = service.fetch(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }
}
