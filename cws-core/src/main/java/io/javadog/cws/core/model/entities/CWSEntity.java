/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
 * mailto: cws AT JavaDog DOT io
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
package io.javadog.cws.core.model.entities;

import io.javadog.cws.api.common.Utilities;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * <p>CWS Entities contain some commonalities, this Class acts as a Super Class
 * for other Entities.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@MappedSuperclass
public class CWSEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private Long id = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "altered", nullable = false)
    private Date altered = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "added", nullable = false, updatable = false)
    private Date added = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setAltered(final Date modified) {
        this.altered = Utilities.copy(modified);
    }

    public Date getAltered() {
        return Utilities.copy(altered);
    }

    public void setAdded(final Date created) {
        this.added = Utilities.copy(created);
    }

    public Date getAdded() {
        return Utilities.copy(added);
    }
}
