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
package net.haugr.eds.core;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.DataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Generally, the database is always trustworthy, it is a system designed
 * to be entrusted with data - meaning that the bits and bytes going in
 * should match those coming out. However, if certain parts of the data are
 * not accessed or used in a long time, the disc used to persist the data on
 * may develop problems which can corrupt data over time.</p>
 *
 * <p>The simplest solution is to make sure that data is used frequently and
 * as long as the data extracted matches the one that was persisted -
 * everything is fine. If a problem occurs over time, then a flag is set which
 * will mark the data invalid. This way, the corrupted record can be either
 * removed or replaced with a valid record from a backup.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Stateless
@Transactional
public class SanitizerBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SanitizerBean.class);
    private static final int BLOCK = 100;

    private final EntityManager entityManager;
    private final Settings settings;

    public SanitizerBean() {
        this(null);
    }

    @Inject
    public SanitizerBean(final EntityManager entityManager) {
        this(entityManager, Settings.getInstance());
    }

    public SanitizerBean(final EntityManager entityManager, final Settings settings) {
        this.entityManager = entityManager;
        this.settings = settings;
    }

    @Transactional(Transactional.TxType.REQUIRED)
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
            final DataEntity entity = entityManager.find(DataEntity.class, id, LockModeType.NONE);
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
            entityManager.persist(entity);
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
        final Query query = entityManager.createNamedQuery("member.removeExpiredSessions");
        LOGGER.debug("expired {} sessions.", query.executeUpdate());
    }

    public List<Long> findNextBatch(final int maxResults) {
        final int days = settings.getSanityInterval();
        final LocalDateTime date = Utilities.newDate().minusDays(days);

        final Query query = entityManager
                .createNamedQuery("data.findIdsForSanityCheck")
                .setParameter("status", SanityStatus.OK)
                .setParameter("date", date)
                .setMaxResults(maxResults);

        return CommonDao.findList(query);
    }
}
