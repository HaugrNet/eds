/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.dtos.Sanity;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.DataEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SanityService extends Serviceable<SanityResponse, SanityRequest> {

    public SanityService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SanityResponse perform(final SanityRequest request) {
        verifyRequest(request, Permission.SANITY);
        final List<DataEntity> found = dao.findFailedRecords();

        final List<Sanity> sanities = new ArrayList<>(found.size());
        for (final DataEntity entity : found) {
            final Sanity sanity = new Sanity();
            sanity.setDataId(entity.getMetadata().getExternalId());
            sanity.setChanged(entity.getAltered());
            sanities.add(sanity);
        }

        final SanityResponse response = new SanityResponse();
        response.setSanities(sanities);
        return response;
    }
}
