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

import io.javadog.cws.api.common.Utilities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * <p>CWS Version Entity, maps the Version table from the Database.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries(
        @NamedQuery(name = "version.findAll",
                    query = "select v from VersionEntity v " +
                            "order by v.id desc")
)
@Table(name = "cws_versions")
public class VersionEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private Long id = null;

    @Column(name = "schema_version", updatable = false)
    private Integer schemaVersion = null;

    @Column(name = "cws_version", unique = true, nullable = false, updatable = false, length = 10)
    private String cwsVersion = null;

    @Column(name = "db_vendor", nullable = false, updatable = false, length = 25)
    private String dbVendor = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "installed", nullable = false, updatable = false)
    private Date installed = null;

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

    public void setCwsVersion(final String cwsVersion) {
        this.cwsVersion = cwsVersion;
    }

    public String getCwsVersion() {
        return cwsVersion;
    }

    public void setDbVendor(final String dbVendor) {
        this.dbVendor = dbVendor;
    }

    public String getDbVendor() {
        return dbVendor;
    }

    public void setInstalled(final Date installed) {
        this.installed = Utilities.copy(installed);
    }

    public Date getInstalled() {
        return Utilities.copy(installed);
    }
}
