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
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class DataTypeServiceTest extends BeanSetup {

    @Test
    void testProcess() {
        final DataTypeService service = prepareDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.process(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedProcess() {
        final DataTypeService service = prepareDataTypeService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.process(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testDelete() {
        final DataTypeService service = prepareDataTypeService(settings, entityManager);
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedDelete() {
        final DataTypeService service = prepareDataTypeService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFetch() {
        final DataTypeService service = prepareDataTypeService(settings, entityManager);
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedFetch() {
        final DataTypeService service = prepareDataTypeService();
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }
}
