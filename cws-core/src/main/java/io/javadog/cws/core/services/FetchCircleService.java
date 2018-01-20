/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchCircleService extends Serviceable<FetchCircleResponse, FetchCircleRequest> {

    public FetchCircleService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse perform(final FetchCircleRequest request) {
        verifyRequest(request, Permission.FETCH_CIRCLE);
        final FetchCircleResponse response = new FetchCircleResponse();

        if (Constants.ADMIN_ACCOUNT.equals(member.getName()) || settings.getShowAllCircles()) {
            final List<CircleEntity> circles = dao.findAllAscending(CircleEntity.class, "name");
            response.setCircles(convertCircles(circles));
        } else {
            final List<Circle> circles = new ArrayList<>(trustees.size());
            for (final TrusteeEntity trustee : trustees) {
                circles.add(convert(trustee.getCircle()));
            }
            response.setCircles(circles);
        }

        return response;
    }
}
