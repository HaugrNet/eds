/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.dtos.Circle;
import net.haugr.eds.api.dtos.CircleRotation;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.enums.Status;
import net.haugr.eds.core.model.RotationDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.KeyEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Business Logic implementation for the EDS FetchCircle request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FetchCircleManager extends AbstractManager<RotationDao, FetchCircleResponse, FetchCircleRequest> {

    public FetchCircleManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new RotationDao(entityManager));
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
        addRotations(entities, circles);

        return circles;
    }

    private List<Circle> fetchMemberCircles() {
        final List<CircleEntity> entities = new ArrayList<>(trustees.size());
        final List<Circle> circles = new ArrayList<>(trustees.size());

        for (final TrusteeEntity trustee : trustees) {
            entities.add(trustee.getCircle());
            circles.add(convert(trustee.getCircle(), decryptExternalKey(trustee)));
        }
        addRotations(entities, circles);
        // By name, like the list of all Circles, regardless of the case.
        circles.sort(java.util.Comparator.comparing((Circle circle) -> circle.getCircleName().toLowerCase(java.util.Locale.ROOT)));

        return circles;
    }

    /**
     * Adds what the Circles tell about the rotation of their Circle Secrets
     * (PLAN.md, Crypto C6): when the current secret was made, when the next
     * rotation is due, and the rotation under way.
     *
     * @param entities The Circles
     * @param circles  Their DTOs, in the same order
     */
    private void addRotations(final List<CircleEntity> entities, final List<Circle> circles) {
        final Map<Long, KeyEntity> active = byCircle(dao.findKeys(entities, Status.ACTIVE));
        final Map<Long, KeyEntity> retiring = byCircle(dao.findKeys(entities, Status.RETIRING));
        for (int i = 0; i < entities.size(); i++) {
            final CircleEntity entity = entities.get(i);
            final Circle circle = circles.get(i);
            final KeyEntity current = active.get(entity.getId());
            circle.setSecretRotated((current != null) ? current.getAdded() : null);
            circle.setRotationDue(entity.getRotateCircleKeyDue());
            final KeyEntity previous = retiring.get(entity.getId());
            if (previous != null) {
                final CircleRotation rotation = new CircleRotation();
                // While the secret is rotated, all content of the Circle is on
                // the previous or the current one; counted on the keys, by
                // their index, not by a join through all Metadata of the
                // Circle (the load run of 2026-11-23).
                final long done = (current != null) ? dao.countDataOnKey(current) : 0;
                rotation.setStarted(circle.getSecretRotated());
                rotation.setObjects(done + dao.countDataOnKey(previous));
                rotation.setObjectsDone(done);
                circle.setRotation(rotation);
            }
        }
    }

    private static Map<Long, KeyEntity> byCircle(final List<KeyEntity> keys) {
        final Map<Long, KeyEntity> map = HashMap.newHashMap(keys.size());
        for (final KeyEntity key : keys) {
            map.put(key.getCircle().getId(), key);
        }
        return map;
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
