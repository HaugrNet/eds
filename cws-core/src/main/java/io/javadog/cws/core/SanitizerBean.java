/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import io.javadog.cws.core.enums.SanityStatus;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.jce.Crypto;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.DataEntity;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Asynchronous
@Transactional
public final class SanitizerBean {

    private static final Logger log = Logger.getLogger(SanitizerBean.class.getName());
    private static final int BLOCK = 100;

    @PersistenceContext(unitName = "cwsDS")
    private EntityManager entityManager;

    @Inject
    private SettingBean settingBean;

    private Crypto crypto = null;

    @PostConstruct
    public void init() {
        crypto = new Crypto(settingBean.getSettings());
    }

    @Asynchronous
    public void sanitize() {
        List<Long> ids = findNextBatch(BLOCK);
        int count = 0;
        int flawed = 0;

        while (!ids.isEmpty()) {
            for (final Long id : ids) {
                final SanityStatus status = processEntity(id);
                if (status != SanityStatus.UNKNOWN) {
                    count++;
                }

                if (status == SanityStatus.FAILED) {
                    flawed++;
                }
            }

            ids = findNextBatch(BLOCK);
        }

        final int[] args = { flawed, count };
        log.log(Settings.INFO, "Completed Sanity check, found {0} out of {1} Data Objects.", args);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public SanityStatus processEntity(final Long id) {
        SanityStatus status = SanityStatus.UNKNOWN;

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
        } catch (PessimisticLockException | LockTimeoutException e) {
            // If we receive this, it can only be because a different process
            // is currently also attempting to sanitize this record - hence we
            // will just log it and otherwise ignore it.
            log.log(Settings.DEBUG, e.getMessage(), e);
        } catch (PersistenceException e) {
            // If this exception is caught, then the lock is unsupported. If
            // that is the case, a more thorough analysis of the underlying
            // cause must be made and therefore it is logged with level ERROR.
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return status;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Long> findNextBatch(final int maxResults) {
        List<Long> ids;

        try {
            // JPA support for Java 8 Date/Time API is not supported
            // before JavaEE8, which is still very early in adoption.
            final int days = settingBean.getSettings().getSanityInterval();
            final Date date = java.sql.Date.valueOf(LocalDate.now().minusDays(days));

            final Query query = entityManager.createNamedQuery("data.findIdsForSanityCheck");
            query.setParameter("status", SanityStatus.OK);
            query.setParameter("date", date);

            query.setMaxResults(maxResults);
            query.setLockMode(LockModeType.PESSIMISTIC_READ);

            ids = CommonDao.findList(query);
        } catch (CWSException e) {
            log.log(Settings.ERROR, "It was not possible to extract the list of Ids for Objects to run a sanity check on: " + e.getMessage(), e);
            ids = new ArrayList<>(0);
        }

        return ids;
    }
}
