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
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the CWS FetchCircle request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
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

        if ((member.getMemberRole() == MemberRole.ADMIN) || settings.hasShowAllCircles()) {
            response.setCircles(fetchCirclesForAdmin());
        } else {
            response.setCircles(fetchFirclesForMember());
        }

        return response;
    }

    private List<Circle> fetchCirclesForAdmin() {
        final List<CircleEntity> entities = dao.findAllAscending(CircleEntity.class, "name");
        final List<Circle> circles = new ArrayList<>(entities.size());

        for (final CircleEntity entity : entities) {
            circles.add(convert(entity, extractExternalCircleKey(entity)));
        }

        return circles;
    }

    private List<Circle> fetchFirclesForMember() {
        final List<Circle> circles = new ArrayList<>(trustees.size());

        for (final TrusteeEntity trustee : trustees) {
            circles.add(convert(trustee.getCircle(), decryptExternalKey(trustee)));
        }

        return circles;
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
