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
package io.javadog.cws.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class TrusteeServiceTest extends BeanSetup {

    @Test
    void testAdd() {
        final TrusteeService service = prepareTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.add(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedAdd() {
        final TrusteeService service = prepareTrusteeService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.add(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testAlter() {
        final TrusteeService service = prepareTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.alter(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedAlter() {
        final TrusteeService service = prepareTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.alter(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testRemove() {
        final TrusteeService service = prepareTrusteeService(settings, entityManager);
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.remove(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedRemove() {
        final TrusteeService service = prepareTrusteeService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.remove(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFetch() {
        final TrusteeService service = prepareTrusteeService(settings, entityManager);
        final FetchTrusteeRequest request = new FetchTrusteeRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedFetch() {
        final TrusteeService service = prepareTrusteeService();
        final FetchTrusteeRequest request = new FetchTrusteeRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }
}
