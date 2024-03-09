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

import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.SettingEntity;
import net.haugr.eds.core.model.entities.VersionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Startup Bean for EDS, it is a singleton, which handles loading of the
 * settings. It also runs the EDS Sanity checks.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Startup
@Singleton
@Asynchronous
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class StartupBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupBean.class);
    private static final int DB_VERSION = 4;

    @PersistenceContext
    private EntityManager entityManager;
    @Inject
    private SanitizerBean sanitizerBean;
    @Resource
    private TimerService timerService;
    private final Settings settings = Settings.getInstance();

    @PostConstruct
    public void startup() {
        LOGGER.info("Check if Database is up-to-date.");
        if (checkDatabase()) {

            LOGGER.info("Initialize the Settings.");
            initializeSettings();

            LOGGER.info("Initializing the EDS Sanitizer Service.");

            // If requested, then simply start sanitize as a background job
            // now. The job will process small blocks of code and save these.
            if (settings.hasSanityStartup()) {
                runSanitizing();
            }

            // Registering the Timer Service. This will ensure that the
            // Scheduler is invoked at frequent intervals.
            final TimerConfig timerConfig = new TimerConfig();
            timerConfig.setInfo("EDS Sanitizer");

            // To prevent starting multiple Timers, it is started in
            // a non-persisted way, meaning that it will be cancelled once
            // the Container is stopped.
            timerConfig.setPersistent(false);

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
            if (!result.isEmpty() && (result.getFirst().getSchemaVersion() == DB_VERSION)) {
                ready = true;
            }
        } catch (EDSException e) {
            LOGGER.error("Problem with DB: {}", e.getMessage(), e);
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
        LOGGER.info("Starting initial Sanitizing check.");
        sanitizerBean.sanitize();
    }

    @Timeout
    public void runSanitizing(final Timer timer) {
        LOGGER.info("Starting Timed Sanitizing check.");
        sanitizerBean.sanitize();
        LOGGER.info("Next Sanitizing check will begin at: {}", timer.getNextTimeout());
    }
}
