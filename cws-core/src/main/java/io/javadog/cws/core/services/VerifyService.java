/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;

import javax.persistence.EntityManager;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class VerifyService extends Serviceable<VerifyResponse, VerifyRequest> {

    public VerifyService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyResponse perform(final VerifyRequest request) {
        verifyRequest(request, Permission.VERIFY_SIGNATURE);

        return new VerifyResponse(ReturnCode.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
