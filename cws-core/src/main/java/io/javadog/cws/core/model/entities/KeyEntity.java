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
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.Status;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * <p>CWS Key Entity, maps the Key table from the Database.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@Table(name = "cws_keys")
public class KeyEntity extends CWSEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm", nullable = false, updatable = false, length = 10)
    private KeyAlgorithm algorithm = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expires", updatable = false)
    private Date expires = null;

    @Column(name = "grace_period", updatable = false)
    private Integer gracePeriod = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setAlgorithm(final KeyAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setExpires(final Date expires) {
        this.expires = Utilities.copy(expires);
    }

    public Date getExpires() {
        return Utilities.copy(expires);
    }

    public void setGracePeriod(final Integer gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    public Integer getGracePeriod() {
        return gracePeriod;
    }
}
