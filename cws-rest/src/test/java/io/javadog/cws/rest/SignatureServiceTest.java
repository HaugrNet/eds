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
package io.javadog.cws.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ShareBean;
import org.junit.Test;

import javax.ws.rs.core.Response;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class SignatureServiceTest extends DatabaseSetup {

    @Test
    public void testSign() {
        final SignatureService service = prepareService();
        final SignRequest request = new SignRequest();

        final Response response = service.sign(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedSign() {
        final SignatureService service = prepareFlawedService();
        final SignRequest request = new SignRequest();

        final Response response = service.sign(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testVerify() {
        final SignatureService service = prepareService();
        final VerifyRequest request = new VerifyRequest();

        final Response response = service.verify(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedVerify() {
        final SignatureService service = prepareFlawedService();
        final VerifyRequest request = new VerifyRequest();

        final Response response = service.verify(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testFetchSignatures() {
        final SignatureService service = prepareService();
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedFetchSignatures() {
        final SignatureService service = prepareFlawedService();
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static SignatureService prepareFlawedService() {
        final SignatureService service = instantiate(SignatureService.class);
        setField(service, "bean", null);

        return service;
    }

    private SignatureService prepareService() {
        final ShareBean bean = instantiate(ShareBean.class);
        setField(bean, "entityManager", entityManager);

        final SignatureService service = instantiate(SignatureService.class);
        setField(service, "bean", bean);

        return service;
    }
}
