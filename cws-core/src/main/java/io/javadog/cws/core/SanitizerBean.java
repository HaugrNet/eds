/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

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
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Stateless
@Transactional
public class SanitizerBean {

    private static final Logger log = Logger.getLogger(SanitizerBean.class.getName());
    private static final int BLOCK = 100;

    @PersistenceContext(unitName = "cwsDS")
    private EntityManager entityManager;
    private final Settings settings = Settings.getInstance();
    private final Crypto crypto = new Crypto(settings);

    public void sanitize() {
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
        log.log(Settings.INFO, "Completed Sanity check, found {0} flaws out of {1} checked Data Objects.", args);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public SanityStatus processEntity(final Long id) {
        SanityStatus status = SanityStatus.OK;

        try {
            // We're reading out the Entity using a Pessimistic lock,
            // as it can be that a separate process is also trying to
            // perform this operation, so to prevent that two processes
            // are working on the same Entity, we're using a lock which
            // is added on the database level.
            final DataEntity entity = entityManager.find(DataEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
            final String checksum = crypto.generateChecksum(entity.getData());

            if (!Objects.equals(checksum, entity.getChecksum())) {
                // Let's update the DB with the information that the data is
                // invalid, and return the error.
                entity.setSanityStatus(SanityStatus.FAILED);
                entity.setAltered(new Date());
            }

            // Regardless, we will always set the check date, so this
            // record will not be attempted again for a while.
            entity.setSanityChecked(new Date());
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
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return status;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
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
