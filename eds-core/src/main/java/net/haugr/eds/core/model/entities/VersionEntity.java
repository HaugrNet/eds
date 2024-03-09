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

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * <p>EDS Version Entity, maps the Version table from the Database.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Entity
@NamedQuery(name = "version.findAll",
        query = "select v from VersionEntity v " +
                "order by v.id desc")
@Table(name = "eds_versions")
public class VersionEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private Long id = null;

    @Column(name = "schema_version", updatable = false)
    private Integer schemaVersion = null;

    @Column(name = "eds_version", unique = true, nullable = false, updatable = false, length = 10)
    private String edsVersion = null;

    @Column(name = "db_vendor", nullable = false, updatable = false, length = 25)
    private String dbVendor = null;

    @Column(name = "installed", nullable = false, updatable = false)
    private LocalDateTime installed = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setSchemaVersion(final Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setEDSVersion(final String edsVersion) {
        this.edsVersion = edsVersion;
    }

    public String getEDSVersion() {
        return edsVersion;
    }

    public void setDBVendor(final String dbVendor) {
        this.dbVendor = dbVendor;
    }

    public String getDBVendor() {
        return dbVendor;
    }

    public void setInstalled(final LocalDateTime installed) {
        this.installed = installed;
    }

    public LocalDateTime getInstalled() {
        return installed;
    }
}
