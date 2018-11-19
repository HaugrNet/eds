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

import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.SettingEntity;
import io.javadog.cws.core.model.entities.VersionEntity;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.logging.Logger;

/**
 * <p>Startup Bean for CWS, it is a singleton, which handles loading of the
 * settings. It also runs the CWS Sanity checks.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Startup
@Singleton
@Asynchronous
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class StartupBean {

    private static final Logger LOG = Logger.getLogger(StartupBean.class.getName());
    private static final int DB_VERSION = 2;

    @PersistenceContext private EntityManager entityManager;
    @Inject private SanitizerBean sanitizerBean;
    @Resource private TimerService timerService;
    private final Settings settings = Settings.getInstance();

    @PostConstruct
    public void startup() {
        LOG.info("Check if Database is up-to-date.");
        if (checkDatabase()) {

            LOG.info("Initialize the Settings.");
            initializeSettings();

            LOG.info("Initializing the CWS Sanitizer Service.");

            // If requested, then simply start the sanitize as a background job
            // now. The job will process small blocks of code and save these.
            if (settings.getSanityStartup()) {
                runSanitizing();
            }

            // Registering the Timer Service. This will ensure that the Scheduler
            // is invoked at frequent intervals.
            final TimerConfig timerConfig = new TimerConfig();
            timerConfig.setInfo("CWS Sanitizer");

            // Once started, the next run should always occur as planned, regardless
            // of restarts, as it is not guaranteed that the sanitizing is performed
            // at startup.
            timerConfig.setPersistent(true);

            // Starting the Timer Service every hour.
            final ScheduleExpression expression = new ScheduleExpression();
            expression.hour("*");
            timerService.createCalendarTimer(expression, timerConfig);
        }
    }

    private boolean checkDatabase() {
        boolean ready = false;

        try {
            final Query query = entityManager.createNamedQuery("version.findAll");
            final List<VersionEntity> result = CommonDao.findList(query);

            // If the database is invalid, then no data could be found, meaning
            // that the database was not properly initialized, this should
            // result in an error while reading.
            if (!result.isEmpty() && (result.get(0).getSchemaVersion() == DB_VERSION)) {
                ready = true;
            }
        } catch (CWSException e) {
            LOG.log(Settings.ERROR, "Problem with DB: " + e.getMessage(), e);
        }

        settings.set(StandardSetting.IS_READY.getKey(), String.valueOf(ready));
        return ready;
    }

    private void initializeSettings() {
        final CommonDao dao = new CommonDao(entityManager);
        final List<SettingEntity> found = dao.findAllAscending(SettingEntity.class, "id");

        for (final SettingEntity entity : found) {
            settings.set(entity.getName(), entity.getSetting());
        }
    }

    @Asynchronous
    public void runSanitizing() {
        LOG.log(Settings.INFO, "Starting initial Sanitizing check.");
        sanitizerBean.sanitize();
    }

    @Timeout
    public void runSanitizing(final Timer timer) {
        LOG.log(Settings.INFO, "Starting Timed Sanitizing check.");
        sanitizerBean.sanitize();
        LOG.log(Settings.INFO, "Next Sanitizing check will begin at: " + timer.getNextTimeout());
    }
}
