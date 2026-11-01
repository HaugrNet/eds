/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.SchemaManager;
import net.haugr.eds.core.model.SchemaManager.Outcome;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.SettingEntity;
import net.haugr.eds.core.model.entities.VersionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Startup Bean for EDS, which prepares and verifies the database, loads
 * the settings, and, if requested, runs an initial EDS Sanity check. The
 * regular (hourly) Sanity checks are scheduled by the runtime.</p>
 *
 * <p>If a {@link SchemaManager} is given, the startup first creates the EDS
 * schema, if it is missing, so an empty database (with only the database,
 * user and password prepared) is filled automatically. Without one, the
 * schema must already exist, as is the case for the tests.</p>
 *
 * <p>The bean does not start itself; the runtime invokes {@link #startup()},
 * and may repeat it while the outcome is {@link Outcome#UNREACHABLE}. The
 * runtime must also provide the {@link SchemaManager} for the database.</p>
 *
 * <p>The startup is deliberately not transactional, as the SchemaManager
 * commits the schema itself, and the verification only reads.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@ApplicationScoped
public class StartupBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupBean.class);
    private static final int DB_VERSION = 4;

    private final EntityManager entityManager;
    private final SanitizerBean sanitizerBean;
    private final SchemaManager schemaManager;
    private final Settings settings;

    public StartupBean() {
        this(null, null);
    }

    public StartupBean(final EntityManager entityManager, final SanitizerBean sanitizerBean) {
        this(entityManager, sanitizerBean, Settings.getInstance());
    }

    public StartupBean(final EntityManager entityManager, final SanitizerBean sanitizerBean, final Settings settings) {
        this(entityManager, sanitizerBean, null, settings);
    }

    /**
     * CDI Constructor.
     *
     * @param entityManager EDS EntityManager instance
     * @param sanitizerBean EDS Sanitizer Bean instance
     * @param schemaManager Creates the schema if missing, provided by the
     *                      runtime for the selected database
     */
    @Inject
    public StartupBean(final EntityManager entityManager, final SanitizerBean sanitizerBean, final SchemaManager schemaManager) {
        this(entityManager, sanitizerBean, schemaManager, Settings.getInstance());
    }

    /**
     * Full Constructor.
     *
     * @param entityManager EDS EntityManager instance
     * @param sanitizerBean EDS Sanitizer Bean instance
     * @param schemaManager Creates the schema if missing, or null if the
     *                      schema is created otherwise
     * @param settings      EDS Settings
     */
    public StartupBean(final EntityManager entityManager, final SanitizerBean sanitizerBean, final SchemaManager schemaManager, final Settings settings) {
        this.entityManager = entityManager;
        this.sanitizerBean = sanitizerBean;
        this.schemaManager = schemaManager;
        this.settings = settings;
    }

    /**
     * <p>Starts EDS: prepares the database (if a SchemaManager is given),
     * verifies it, loads the Settings, and starts the Sanity checks.</p>
     *
     * <p>EDS is only reported ready, if the outcome is {@link Outcome#READY}.
     * The outcome {@link Outcome#UNREACHABLE} means that the database could
     * not be reached, and that the startup may be repeated. A database which
     * is reachable, but cannot be prepared, or has a different schema
     * version, is reported as {@link Outcome#FAILED}.</p>
     *
     * @return The outcome of the startup
     */
    public Outcome startup() {
        if (schemaManager != null) {
            final Outcome prepared = schemaManager.prepare();
            if (prepared != Outcome.READY) {
                settings.set(StandardSetting.IS_READY.getKey(), "false");
                return prepared;
            }
        }

        LOGGER.info("Check if Database is up-to-date.");
        if (!checkDatabase()) {
            return Outcome.FAILED;
        }

        LOGGER.info("Initialize the Settings.");
        initializeSettings();

        // If requested, run an initial Sanity check now. The regular checks
        // are scheduled by the runtime (see SanitizerSchedule in eds-quarkus).
        if (settings.hasSanityStartup()) {
            runSanitizing();
        }

        return Outcome.READY;
    }

    /**
     * Returns the message of the last failed connection attempt, if the
     * database could not be reached.
     *
     * @return Message of the last connection failure, or null
     */
    public String getLastConnectionError() {
        return (schemaManager != null) ? schemaManager.getLastConnectionError() : null;
    }

    private boolean checkDatabase() {
        boolean ready = false;

        try {
            final Query query = entityManager.createNamedQuery("version.findAll");
            final List<VersionEntity> result = CommonDao.findList(query);

            // If the database is invalid, then no data could be found, meaning
            // that the database was not properly initialized; this should
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

    public void runSanitizing() {
        LOGGER.info("Starting initial Sanitizing check.");
        sanitizerBean.sanitize();
    }
}
