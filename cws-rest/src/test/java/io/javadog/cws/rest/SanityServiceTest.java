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
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ManagementBean;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class SanityServiceTest extends DatabaseSetup {

    @Test
    public void testSanitized() {
        final SanityService service = prepareService();
        final SanityRequest request = new SanityRequest();

        final Response response = service.sanitized(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFlawedSanitized() {
        final SanityService service = prepareFlawedService();
        final SanityRequest request = new SanityRequest();

        final Response response = service.sanitized(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static SanityService prepareFlawedService() {
        final SanityService service = instantiate(SanityService.class);
        setField(service, "bean", null);

        return service;
    }

    private SanityService prepareService() {
        final ManagementBean bean = instantiate(ManagementBean.class);
        setField(bean, "entityManager", entityManager);

        final SanityService service = instantiate(SanityService.class);
        setField(service, "bean", bean);

        return service;
    }
}
