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
package net.haugr.eds.core.managers;

import java.util.Arrays;
import javax.persistence.EntityManager;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.responses.AuthenticateResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;

/**
 * <p>Business Logic implementation for the EDS Authenticated request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1
 */
public final class AuthenticatedManager extends AbstractManager<CommonDao, AuthenticateResponse, Authentication> {

    public AuthenticatedManager(final Settings settings, final EntityManager entityManager) {
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
