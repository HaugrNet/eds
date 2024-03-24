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
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class CircleServiceTest extends BeanSetup {

    @Test
    void testCreate() {
        final CircleService service = prepareCircleService(settings, entityManager);
        final ProcessCircleRequest request = new ProcessCircleRequest();

        try (final Response response = service.create(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testFlawedCreate() {
        final CircleService service = prepareCircleService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        try (final Response response = service.create(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testUpdate() {
        final CircleService service = prepareCircleService(settings, entityManager);
        final ProcessCircleRequest request = new ProcessCircleRequest();

        try (final Response response = service.update(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testFlawedUpdate() {
        final CircleService service = prepareCircleService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        try (final Response response = service.update(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testDelete() {
        final CircleService service = prepareCircleService(settings, entityManager);
        final ProcessCircleRequest request = new ProcessCircleRequest();

        try (final Response response = service.delete(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testFlawedDelete() {
        final CircleService service = prepareCircleService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        try (final Response response = service.delete(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testFetch() {
        final CircleService service = prepareCircleService(settings, entityManager);
        final FetchCircleRequest request = new FetchCircleRequest();

        try (final Response response = service.fetch(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }

    @Test
    void testFlawedFetch() {
        final CircleService service = prepareCircleService();
        final FetchCircleRequest request = new FetchCircleRequest();

        try (final Response response = service.fetch(request)) {
            assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
        }
    }
}
