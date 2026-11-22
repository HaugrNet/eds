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
package net.haugr.eds.core.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * <p>EDS MasterKey Entity, maps the MasterKey table. Only the version and
 * the key check value (KCV) of a MasterKey are stored, never the key or its
 * source (PLAN.md, Crypto §1). The row with the highest version is the current
 * MasterKey.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Entity
@NamedQuery(name = "masterkey.findAllByVersion",
        query = "select m from MasterKeyEntity m " +
                "order by m.version desc")
@Table(name = "eds_masterkey")
public class MasterKeyEntity extends EDSEntity {

    @Column(name = "version", nullable = false, unique = true)
    private Integer version = null;

    @Column(name = "kcv", nullable = false, length = 256)
    private String kcv = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public Integer getVersion() {
        return version;
    }

    public void setKcv(final String kcv) {
        this.kcv = kcv;
    }

    public String getKcv() {
        return kcv;
    }
}
