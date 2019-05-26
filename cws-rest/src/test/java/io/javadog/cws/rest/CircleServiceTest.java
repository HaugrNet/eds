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

import static org.junit.Assert.assertEquals;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ManagementBean;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class CircleServiceTest extends DatabaseSetup {

    @Test
    public void testCreate() {
        final CircleService service = prepareService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.create(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFlawedCreate() {
        final CircleService service = prepareFlawedService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.create(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testUpdatw() {
        final CircleService service = prepareService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.update(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFlawedUpdate() {
        final CircleService service = prepareFlawedService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.update(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testDelete() {
        final CircleService service = prepareService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFlawedDelete() {
        final CircleService service = prepareFlawedService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFetch() {
        final CircleService service = prepareService();
        final FetchCircleRequest request = new FetchCircleRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFlawedFetch() {
        final CircleService service = prepareFlawedService();
        final FetchCircleRequest request = new FetchCircleRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static CircleService prepareFlawedService() {
        final CircleService service = instantiate(CircleService.class);
        setField(service, "bean", null);

        return service;
    }

    private CircleService prepareService() {
        final ManagementBean bean = instantiate(ManagementBean.class);
        setField(bean, "entityManager", entityManager);

        final CircleService service = instantiate(CircleService.class);
        setField(service, "bean", bean);

        return service;
    }
}
