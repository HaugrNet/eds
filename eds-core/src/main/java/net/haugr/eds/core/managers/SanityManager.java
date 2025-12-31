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

import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.dtos.Sanity;
import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.api.responses.SanityResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.SanityDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.DataEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Business Logic implementation for the EDS Sanity request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class SanityManager extends AbstractManager<SanityDao, SanityResponse, SanityRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SanityManager.class);
    private static final int BLOCK = 100;

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

    public void sanitize() {
        final Crypto crypto = new Crypto(settings);
        clearExpireSessions();
        List<Long> ids = findNextBatch(BLOCK);
        long count = 0;
        long flawed = 0;

        while (!ids.isEmpty()) {
            for (final Long id : ids) {
                final SanityStatus status = processEntity(crypto, id);
                if (status == SanityStatus.FAILED) {
                    flawed++;
                }
                count++;
            }

            ids = findNextBatch(BLOCK);
        }

        LOGGER.info("Completed Sanity check, found {} flaws out of {} checked Data Objects.", flawed, count);
    }

    public List<Long> findNextBatch(final int maxResults) {
        final int days = settings.getSanityInterval();
        final LocalDateTime date = Utilities.newDate().minusDays(days);

        final Query query = dao.createNamedQuery("data.findIdsForSanityCheck")
                .setParameter("status", SanityStatus.OK)
                .setParameter("date", date)
                .setMaxResults(maxResults);

        return CommonDao.findList(query);
    }

    public SanityStatus processEntity(final Crypto crypto, final Long id) {
        SanityStatus status;

        try {
            // When updating, it would be preferable to use Pessimistic
            // locking to prevent that other processes accidentally update
            // also. Pessimistic locking is made at the DB level, whereas
            // Optimistic locking is handled by the ORM Vendor.
            //   Even if two different EDS instances perform the same update on
            // an Object, it should not have any other consequences than wasted
            // CPU and DB updates.
            final DataEntity entity = dao.find(DataEntity.class, id, LockModeType.NONE);
            final String checksum = crypto.generateChecksum(entity.getData());

            if (!Objects.equals(checksum, entity.getChecksum())) {
                // Let's update the DB with the information that the data is
                //  invalid and return the error.
                entity.setSanityStatus(SanityStatus.FAILED);
                entity.setAltered(Utilities.newDate());
            }

            // Regardless, we will always set the check date, so this
            // record will not be attempted again for a while.
            entity.setSanityChecked(Utilities.newDate());
            dao.save(entity);
            status = entity.getSanityStatus();
        } catch (RuntimeException e) {
            // There are 2 (3) potential problems that may be caught here:
            //   1. A different process (EDS instance) may be processing the
            //      record, hence it is perfectly legitimate, and we can
            //      actually ignore the error. However, it is still being
            //      logged.
            //   2. The underlying database does not support locking, so it is
            //      not possible to continue. If this is the case, it should be
            //      reported to the EDS developers.
            //   3. Unlikely - NPE caused by a missing EntityManager instance
            LOGGER.error(e.getMessage(), e);
            status = SanityStatus.BLOCKED;
        }

        return status;
    }

    private void clearExpireSessions() {
        final Query query = dao.createNamedQuery("member.removeExpiredSessions");
        LOGGER.debug("expired {} sessions.", query.executeUpdate());
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
            // Find for a specific Member, which will retrieve all records which
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
