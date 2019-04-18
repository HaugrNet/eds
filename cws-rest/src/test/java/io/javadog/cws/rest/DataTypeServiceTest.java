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
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ShareBean;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class DataTypeServiceTest extends DatabaseSetup {

    @Test
    public void testProcess() {
        final DataTypeService service = prepareService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.process(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFlawedProcess() {
        final DataTypeService service = prepareFlawedService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.process(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testDelete() {
        final DataTypeService service = prepareService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.delete(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFlawedDelete() {
        final DataTypeService service = prepareFlawedService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.delete(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFetch() {
        final DataTypeService service = prepareService();
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFlawedFetch() {
        final DataTypeService service = prepareFlawedService();
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static DataTypeService prepareFlawedService() {
        final DataTypeService service = instantiate(DataTypeService.class);
        setField(service, "bean", null);

        return service;
    }

    private DataTypeService prepareService() {
        final ShareBean bean = instantiate(ShareBean.class);
        setField(bean, "entityManager", entityManager);

        final DataTypeService service = instantiate(DataTypeService.class);
        setField(service, "bean", bean);

        return service;
    }
}
