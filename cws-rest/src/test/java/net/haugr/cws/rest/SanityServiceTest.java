/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.requests.SanityRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class SanityServiceTest extends BeanSetup {

    @Test
    void testSanitized() {
        final SanityService service = prepareSanityService(settings, entityManager);
        final SanityRequest request = new SanityRequest();

        final Response response = service.sanitized(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedSanitized() {
        final SanityService service = prepareSanityService();
        final SanityRequest request = new SanityRequest();

        final Response response = service.sanitized(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }
}
