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
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ManagementBean;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class TrusteeServiceTest extends DatabaseSetup {

    @Test
    public void testAdd() {
        final TrusteeService service = prepareService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.add(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFlawedAdd() {
        final TrusteeService service = prepareFlawedService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.add(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testAlter() {
        final TrusteeService service = prepareService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.alter(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFlawedAlter() {
        final TrusteeService service = prepareFlawedService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.alter(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testRemove() {
        final TrusteeService service = prepareService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.remove(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFlawedRemove() {
        final TrusteeService service = prepareFlawedService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.remove(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFetch() {
        final TrusteeService service = prepareService();
        final FetchTrusteeRequest request = new FetchTrusteeRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFlawedFetch() {
        final TrusteeService service = prepareFlawedService();
        final FetchTrusteeRequest request = new FetchTrusteeRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static TrusteeService prepareFlawedService() {
        final TrusteeService service = instantiate(TrusteeService.class);
        setField(service, "bean", null);

        return service;
    }

    private TrusteeService prepareService() {
        final ManagementBean bean = instantiate(ManagementBean.class);
        setField(bean, "entityManager", entityManager);

        final TrusteeService service = instantiate(TrusteeService.class);
        setField(service, "bean", bean);

        return service;
    }
}
