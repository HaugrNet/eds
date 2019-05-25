/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2019 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ManagementBean;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.1
 */
public final class AuthenticatedServiceTest extends DatabaseSetup {

    @Test
    public void testAuthenticate() {
        final AuthenticatedService service = prepareService();
        final Authentication request = new Authentication();

        final Response response = service.authenticated(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFlawedCreate() {
        final AuthenticatedService service = prepareFlawedService();
        final Authentication request = new Authentication();

        final Response response = service.authenticated(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static AuthenticatedService prepareFlawedService() {
        final AuthenticatedService service = instantiate(AuthenticatedService.class);
        setField(service, "bean", null);

        return service;
    }

    private AuthenticatedService prepareService() {
        final ManagementBean bean = instantiate(ManagementBean.class);
        setField(bean, "entityManager", entityManager);

        final AuthenticatedService service = instantiate(AuthenticatedService.class);
        setField(service, "bean", bean);

        return service;
    }
}
