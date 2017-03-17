/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries(
        @NamedQuery(name = "setting.readAll",
                    query = "select s from SettingEntity s")
)
@Table(name = "settings")
public class SettingEntity extends CWSEntity {

    @Column(name = "name", nullable = false)
    private String name = null;

    @Column(name = "setting")
    private String setting = null;

    @Column(name = "modifiable", nullable = false)
    private Boolean modifiable = null;

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

    public void setModifiable(final Boolean modifiable) {
        this.modifiable = modifiable;
    }

    public Boolean getModifiable() {
        return modifiable;
    }
}
