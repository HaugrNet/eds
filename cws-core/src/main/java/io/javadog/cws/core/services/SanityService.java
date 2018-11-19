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

import io.javadog.cws.api.common.Utilities;
import io.javadog.cws.api.dtos.Sanity;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.core.enums.MemberRole;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.SanityDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.DataEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * <p>Business Logic implementation for the CWS Sanity request.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SanityService extends Serviceable<SanityDao, SanityResponse, SanityRequest> {

    public SanityService(final Settings settings, final EntityManager entityManager) {
        super(settings, new SanityDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SanityResponse perform(final SanityRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.SANITY);
        Arrays.fill(request.getCredential(), (byte) 0);

        final List<DataEntity> found = findRecords(request);
        final List<Sanity> sanities = convertRecords(found);
        final SanityResponse response = new SanityResponse();
        response.setSanities(sanities);

        return response;
    }

    private List<DataEntity> findRecords(final SanityRequest request) {
        final Date since = (request.getSince() == null) ? Utilities.newDate(0L) : request.getSince();
        final List<DataEntity> found;

        if (request.getCircleId() != null) {
            // Find for specific Circle
            found = dao.findFailedRecords(request.getCircleId(),since);
        } else if (member.getMemberRole() == MemberRole.ADMIN) {
            // The System Administrator is allowed to retrieve all records for
            // all Circles.
            found = dao.findFailedRecords(since);
        } else {
            // Find for specific Member, which will retrieve all records which
            // the member is Administrator for
            found = dao.findFailedRecords(member, since);
        }

        return found;
    }

    private static List<Sanity> convertRecords(final List<DataEntity> found) {
        final List<Sanity> sanities = new ArrayList<>(found.size());

        for (final DataEntity entity : found) {
            final Sanity sanity = new Sanity();
            sanity.setDataId(entity.getMetadata().getExternalId());
            sanity.setChanged(entity.getSanityChecked());
            sanities.add(sanity);
        }

        return sanities;
    }
}
