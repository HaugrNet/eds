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
import net.haugr.eds.api.requests.Authentication;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.1
 */
@Disabled("Upgrading to Jakarta EE 10 requires a re-write of the Endpoint tests")
final class AuthenticatedServiceTest extends BeanSetup {

    @Test
    void testAuthenticate() {
        final AuthenticatedService service = prepareAuthenticatedService(settings, entityManager);
        final Authentication request = new Authentication();

        final Response response = service.authenticated(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedCreate() {
        final AuthenticatedService service = prepareAuthenticatedService();
        final Authentication request = new Authentication();

        final Response response = service.authenticated(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }
}
