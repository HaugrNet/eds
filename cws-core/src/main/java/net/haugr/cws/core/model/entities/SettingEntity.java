/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.core.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * <p>CWS Setting Entity, maps the Setting table from the Database.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Entity
@NamedQuery(name = "setting.readAll",
        query = "select s from SettingEntity s " +
                "order by s.name asc")
@NamedQuery(name = "setting.findByName",
        query = "select s from SettingEntity s " +
                "where s.name = :name")
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
