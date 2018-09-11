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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

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
