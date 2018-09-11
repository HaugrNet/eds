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
package io.javadog.cws.core.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * <p>CWS Setting Entity, maps the Setting table from the Database.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries(
        @NamedQuery(name = "setting.readAll",
                    query = "select s from SettingEntity s " +
                            "order by s.name asc")
)
@Table(name = "cws_settings")
public class SettingEntity extends CWSEntity {

    @Column(name = "name", nullable = false)
    private String name = null;

    @Column(name = "setting")
    private String setting = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSetting(final String setting) {
        this.setting = setting;
    }

    public String getSetting() {
        return setting;
    }
}
