/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;

import javax.persistence.EntityManager;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchSignatureService extends Serviceable<FetchSignatureResponse, FetchSignatureRequest> {

    public FetchSignatureService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchSignatureResponse perform(final FetchSignatureRequest request) {
        verifyRequest(request, Permission.FETCH_SIGNATURES);

        throw new CWSException(ReturnCode.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
