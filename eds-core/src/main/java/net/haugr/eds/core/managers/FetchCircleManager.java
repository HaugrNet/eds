/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.dtos.Circle;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Business Logic implementation for the EDS FetchCircle request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FetchCircleManager extends AbstractManager<CommonDao, FetchCircleResponse, FetchCircleRequest> {

    public FetchCircleManager(final Settings settings, final EntityManager entityManager) {
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
            response.setCircles(fetchMemberCircles());
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

    private List<Circle> fetchMemberCircles() {
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
