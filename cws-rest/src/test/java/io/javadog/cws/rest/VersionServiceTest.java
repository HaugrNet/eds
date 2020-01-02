/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
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
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ManagementBean;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class VersionServiceTest extends DatabaseSetup {

    @Test
    void testVersion() {
        final VersionService service = prepareService();

        final Response response = service.version();
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedVersion() {
        final VersionService service = prepareFlawedService();

        final Response response = service.version();
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static VersionService prepareFlawedService() {
        final VersionService service = instantiate(VersionService.class);
        setField(service, "bean", null);

        return service;
    }

    private VersionService prepareService() {
        final ManagementBean bean = instantiate(ManagementBean.class);
        setField(bean, "entityManager", entityManager);

        final VersionService service = instantiate(VersionService.class);
        setField(service, "bean", bean);

        return service;
    }
}
