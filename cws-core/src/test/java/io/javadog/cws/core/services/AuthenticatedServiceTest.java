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
package io.javadog.cws.core.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.core.DatabaseSetup;
import org.junit.Test;

/**
 * <p>This Test Class, is testing the Authenticated Service Class.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
public final class AuthenticatedServiceTest extends DatabaseSetup {

    @Test
    public void testAuthenticate() {
        final AuthenticatedService service = new AuthenticatedService(settings, entityManager);
        final Authentication request = prepareRequest(Authentication.class, MEMBER_1);
        final CwsResponse response = service.perform(request);

        assertNotNull(response);
        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }
}
