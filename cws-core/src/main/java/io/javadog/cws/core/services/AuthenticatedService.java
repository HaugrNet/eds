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

import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import java.util.Arrays;
import javax.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the CWS Authenticated request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
public final class AuthenticatedService extends Serviceable<CommonDao, CwsResponse, Authentication> {

    public AuthenticatedService(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CwsResponse perform(final Authentication request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.PROCESS_MEMBER);
        Arrays.fill(request.getCredential(), (byte) 0);

        // And done... Authentication checks all completed
        return new CwsResponse(member.getName() + " successfully authenticated.");
    }
}
