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
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ShareBean;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class DataServiceTest extends DatabaseSetup {

    @Test
    public void testAdd() {
        final DataService service = prepareService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.add(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFlawedAdd() {
        final DataService service = prepareFlawedService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.add(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testCopy() {
        final DataService service = prepareService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.copy(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFlawedCopy() {
        final DataService service = prepareFlawedService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.copy(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testMove() {
        final DataService service = prepareService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.move(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFlawedMove() {
        final DataService service = prepareFlawedService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.move(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testUpdate() {
        final DataService service = prepareService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.update(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFlawedUpdate() {
        final DataService service = prepareFlawedService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.update(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testDelete() {
        final DataService service = prepareService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFlawedDelete() {
        final DataService service = prepareFlawedService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFetch() {
        final DataService service = prepareService();
        final FetchDataRequest request = new FetchDataRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    public void testFlawedFetch() {
        final DataService service = prepareFlawedService();
        final FetchDataRequest request = new FetchDataRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static DataService prepareFlawedService() {
        final DataService service = instantiate(DataService.class);
        setField(service, "bean", null);

        return service;
    }

    private DataService prepareService() {
        final ShareBean bean = instantiate(ShareBean.class);
        setField(bean, "entityManager", entityManager);

        final DataService service = instantiate(DataService.class);
        setField(service, "bean", bean);

        return service;
    }
}
