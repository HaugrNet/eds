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

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.AuthenticateResponse;
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
public final class AuthenticatedService extends Serviceable<CommonDao, AuthenticateResponse, Authentication> {

    public AuthenticatedService(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthenticateResponse perform(final Authentication request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.PROCESS_MEMBER);
        Arrays.fill(request.getCredential(), (byte) 0);

        // And done... Authentication check completed
        final AuthenticateResponse response = new AuthenticateResponse();
        response.setReturnMessage(member.getName() + " successfully authenticated.");
        response.setMemberId(member.getExternalId());
        response.setReturnCode(ReturnCode.SUCCESS);

        return response;
    }
}
