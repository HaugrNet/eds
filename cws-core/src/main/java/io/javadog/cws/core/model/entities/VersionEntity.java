/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model.entities;

import static io.javadog.cws.api.common.Utilities.copy;

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
                            "order by id desc")
)
@Table(name = "cws_versions")
public class VersionEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "schema_version", insertable = false, updatable = false)
    private Integer schemaVersion;

    @Column(name = "cws_version", unique = true, nullable = false, insertable = false, updatable = false, length = 10)
    private String cwsVersion;

    @Column(name = "db_vendor", nullable = false, insertable = false, updatable = false, length = 25)
    private String dbVendor;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "installed", nullable = false, insertable = false, updatable = false)
    private Date installed;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public final void setId(final Long id) {
        this.id = id;
    }

    public final Long getId() {
        return id;
    }

    public final void setSchemaVersion(final Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public final Integer getSchemaVersion() {
        return schemaVersion;
    }

    public final void setCwsVersion(final String cwsVersion) {
        this.cwsVersion = cwsVersion;
    }

    public final String getCwsVersion() {
        return cwsVersion;
    }

    public final void setDbVendor(final String dbVendor) {
        this.dbVendor = dbVendor;
    }

    public final String getDbVendor() {
        return dbVendor;
    }

    public final void setInstalled(final Date installed) {
        this.installed = copy(installed);
    }

    public final Date getInstalled() {
        return copy(installed);
    }
}
