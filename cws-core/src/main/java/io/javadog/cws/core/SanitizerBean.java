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
package io.javadog.cws.core;

import io.javadog.cws.api.common.Utilities;
import io.javadog.cws.core.enums.SanityStatus;
import io.javadog.cws.core.jce.Crypto;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.DataEntity;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * <p>Generally, the database is always trustworthy, it is a system which is
 * designed to be entrusted with data - meaning that the bits and bytes going
 * in should match those coming out. However, if certain parts of the data is
 * not accessed or used in a long time, the disc used to persist the data on
 * may develop problems which can corrupt data over time.</p>
 *
 * <p>Simplest solution is to make sure that data is used frequently and as
 * long as the data extracted matches the one that was persisted - everything
 * is fine. If a problem occurs over time, then a flag is set which will mark
 * the data invalid. This way, the corrupted record can be either removed or
 * replaced with a valid record from a backup.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Stateless
@Transactional
public class SanitizerBean {

    private static final Logger LOG = Logger.getLogger(SanitizerBean.class.getName());
    private static final int BLOCK = 100;

    @PersistenceContext private EntityManager entityManager;
    private final Settings settings = Settings.getInstance();
    private final Crypto crypto = new Crypto(settings);

    @Transactional(Transactional.TxType.REQUIRED)
    public void sanitize() {
        clearExpireSessions();
        List<Long> ids = findNextBatch(BLOCK);
        int count = 0;
        int flawed = 0;

        while (!ids.isEmpty()) {
            for (final Long id : ids) {
                final SanityStatus status = processEntity(id);
                if (status == SanityStatus.FAILED) {
                    flawed++;
                }
                count++;
            }

            ids = findNextBatch(BLOCK);
        }

        final String[] args = { String.valueOf(flawed), String.valueOf(count) };
        LOG.log(Settings.INFO, "Completed Sanity check, found {0} flaws out of {1} checked Data Objects.", args);
    }

    private SanityStatus processEntity(final Long id) {
        SanityStatus status = SanityStatus.OK;

        try {
            // When trying to run the updates, it would be good if it could be
            // done using a Pessimistic locking, to prevent that other processes
            // accidentally also perform the update, as the Pessimistic locking
            // is made at te DB level, whereas the Optimistic locking is handled
            // by the ORM Vendor. However, due to problems with the stability of
            // the Travis-CI builds, the locking has been removed.
            //   Even if two different CWS instances perform the same update on
            // an Object, it should not have any other consequences than wasted
            // CPU and DB updates.
            final DataEntity entity = entityManager.find(DataEntity.class, id, LockModeType.NONE);
            final String checksum = crypto.generateChecksum(entity.getData());

            if (!Objects.equals(checksum, entity.getChecksum())) {
                // Let's update the DB with the information that the data is
                // invalid, and return the error.
                entity.setSanityStatus(SanityStatus.FAILED);
                entity.setAltered(Utilities.newDate());
            }

            // Regardless, we will always set the check date, so this
            // record will not be attempted again for a while.
            entity.setSanityChecked(Utilities.newDate());
            entityManager.persist(entity);
            status = entity.getSanityStatus();
        } catch (PersistenceException e) {
            // There is 2 potential problems which may be caught here:
            //   1. A different process (CWS instance) may be processing the
            //      record, hence it is perfectly legitimate and we can actually
            //      ignore the error. However, it is still being logged.
            //   2. The underlying database does not support locking, so it is
            //      not possible to continue. If this is the case, it should be
            //      reported to the CWS developers.
            LOG.log(Settings.ERROR, e.getMessage(), e);
        }

        return status;
    }

    private void clearExpireSessions() {
        final Query query = entityManager.createNamedQuery("member.removeExpiredSessions");
        final String logMessage = "expired " + query.executeUpdate() + " sessions.";
        LOG.log(Settings.DEBUG, logMessage);
    }

    public List<Long> findNextBatch(final int maxResults) {
        // JPA support for Java 8 Date/Time API is not supported
        // before JavaEE8, which is still very early in adoption.
        final int days = settings.getSanityInterval();
        final Date date = java.sql.Date.valueOf(LocalDate.now().minusDays(days));

        final Query query = entityManager.createNamedQuery("data.findIdsForSanityCheck");
        query.setParameter("status", SanityStatus.OK);
        query.setParameter("date", date);
        query.setMaxResults(maxResults);

        return CommonDao.findList(query);
    }
}
