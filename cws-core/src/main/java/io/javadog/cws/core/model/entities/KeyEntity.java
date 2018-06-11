/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model.entities;

import static io.javadog.cws.api.common.Utilities.copy;

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

    public final void setAlgorithm(final KeyAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public final KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public final void setStatus(final Status status) {
        this.status = status;
    }

    public final Status getStatus() {
        return status;
    }

    public final void setExpires(final Date expires) {
        this.expires = copy(expires);
    }

    public final Date getExpires() {
        return copy(expires);
    }

    public final void setGracePeriod(final Integer gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    public final Integer getGracePeriod() {
        return gracePeriod;
    }
}
