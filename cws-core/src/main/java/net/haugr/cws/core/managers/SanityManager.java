/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.core.managers;

import net.haugr.cws.api.common.MemberRole;
import net.haugr.cws.api.common.Utilities;
import net.haugr.cws.api.dtos.Sanity;
import net.haugr.cws.api.requests.SanityRequest;
import net.haugr.cws.api.responses.SanityResponse;
import net.haugr.cws.core.enums.Permission;
import net.haugr.cws.core.model.SanityDao;
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.model.entities.DataEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the CWS Sanity request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
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
        final var response = new SanityResponse();
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
            final var sanity = new Sanity();
            sanity.setDataId(entity.getMetadata().getExternalId());
            sanity.setChanged(entity.getSanityChecked());
            sanities.add(sanity);
        }

        return sanities;
    }
}
