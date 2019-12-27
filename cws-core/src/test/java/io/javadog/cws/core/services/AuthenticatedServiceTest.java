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
package io.javadog.cws.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.core.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>This Test Class, is testing the Authenticated Service Class.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
final class AuthenticatedServiceTest extends DatabaseSetup {

    @Test
    void testAuthenticate() {
        final AuthenticatedService service = new AuthenticatedService(settings, entityManager);
        final Authentication request = prepareRequest(Authentication.class, MEMBER_1);
        final CwsResponse response = service.perform(request);

        assertNotNull(response);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("member1 successfully authenticated.", response.getReturnMessage());
    }
}
