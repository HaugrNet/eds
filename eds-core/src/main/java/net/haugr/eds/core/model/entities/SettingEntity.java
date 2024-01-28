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
package net.haugr.eds.core.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * <p>EDS Setting Entity, maps the Setting table from the Database.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Entity
@NamedQuery(name = "setting.readAll",
        query = "select s from SettingEntity s " +
                "order by s.name asc")
@NamedQuery(name = "setting.findByName",
        query = "select s from SettingEntity s " +
                "where s.name = :name")
@Table(name = "eds_settings")
public class SettingEntity extends EDSEntity {

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
