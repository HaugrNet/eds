/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.core.enums.MemberRole;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Business Logic implementation for the CWS FetchCircle request.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchCircleService extends Serviceable<CommonDao, FetchCircleResponse, FetchCircleRequest> {

    public FetchCircleService(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse perform(final FetchCircleRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.FETCH_CIRCLE);
        Arrays.fill(request.getCredential(), (byte) 0);

        final FetchCircleResponse response = new FetchCircleResponse();
        final List<Circle> circles;

        if ((member.getMemberRole() == MemberRole.ADMIN) || settings.getShowAllCircles()) {
            final List<CircleEntity> entities = dao.findAllAscending(CircleEntity.class, "name");
            circles = new ArrayList<>(entities.size());
            for (final CircleEntity entity : entities) {
                circles.add(convert(entity, extractExternalCircleKey(entity)));
            }
        } else {
            circles = new ArrayList<>(trustees.size());
            for (final TrusteeEntity trustee : trustees) {
                circles.add(convert(trustee.getCircle(), decryptExternalKey(trustee)));
            }
        }

        response.setCircles(circles);
        return response;
    }

    private String extractExternalCircleKey(final CircleEntity entity) {
        String externalKey = null;

        for (final TrusteeEntity trustee : trustees) {
            if (Objects.equals(trustee.getCircle().getId(), entity.getId())) {
                externalKey = decryptExternalKey(trustee);
            }
        }

        return externalKey;
    }
}
