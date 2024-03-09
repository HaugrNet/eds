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
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Disabled("Upgrading to Jakarta EE 10 requires a re-write of the Endpoint tests")
final class DataServiceTest extends BeanSetup {

    @Test
    void testAdd() {
        final DataService service = prepareDataService(settings, entityManager);
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.add(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedAdd() {
        final DataService service = prepareDataService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.add(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testCopy() {
        final DataService service = prepareDataService(settings, entityManager);
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.copy(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedCopy() {
        final DataService service = prepareDataService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.copy(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testMove() {
        final DataService service = prepareDataService(settings, entityManager);
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.move(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedMove() {
        final DataService service = prepareDataService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.move(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testUpdate() {
        final DataService service = prepareDataService(settings, entityManager);
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.update(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedUpdate() {
        final DataService service = prepareDataService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.update(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testDelete() {
        final DataService service = prepareDataService(settings, entityManager);
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedDelete() {
        final DataService service = prepareDataService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFetch() {
        final DataService service = prepareDataService(settings, entityManager);
        final FetchDataRequest request = new FetchDataRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedFetch() {
        final DataService service = prepareDataService();
        final FetchDataRequest request = new FetchDataRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }
}
