/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.dtos.Sanity;
import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.api.responses.SanityResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.model.SanityDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.DataEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the EDS Sanity request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class SanityManager extends AbstractManager<SanityDao, SanityResponse, SanityRequest> {

    public SanityManager(final Settings settings, final EntityManager entityManager) {
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
        final LocalDateTime since = (request.getSince() == null) ? Utilities.newDate(0L) : request.getSince();
        final List<DataEntity> found;

        if (request.getCircleId() != null) {
            // Find for specific Circle
            found = dao.findFailedRecords(request.getCircleId(), since);
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

    private static List<Sanity> convertRecords(final Collection<DataEntity> found) {
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
