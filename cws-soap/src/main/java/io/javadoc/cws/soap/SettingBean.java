/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-soap)
 * =============================================================================
 */
package io.javadoc.cws.soap;

import io.javadog.cws.common.Settings;
import io.javadog.cws.model.CommonDao;
import io.javadog.cws.model.entities.SettingEntity;
import io.javadog.cws.model.jpa.CommonJpaDao;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Stateless
public class SettingBean {

    @PersistenceContext(unitName = "cwsDS")
    private EntityManager entityManager;

    private final Settings settings = new Settings();

    @PostConstruct
    public void init() {
        final CommonDao dao = new CommonJpaDao(entityManager);
        final List<SettingEntity> found = dao.readSettings();

        for (final SettingEntity entity : found) {
            settings.set(entity.getName(), entity.getSetting());
        }
    }

    public Settings getSettings() {
        return settings;
    }
}
