/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;

import javax.persistence.EntityManager;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchObjectService extends Serviceable<FetchDataResponse, FetchDataRequest> {

    public FetchObjectService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataResponse perform(final FetchDataRequest request) {
        verifyRequest(request, Permission.FETCH_OBJECT);

        throw new CWSException(ReturnCode.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
