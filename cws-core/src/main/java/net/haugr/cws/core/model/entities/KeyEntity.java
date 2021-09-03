/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.core.model.entities;

import net.haugr.cws.core.enums.KeyAlgorithm;
import net.haugr.cws.core.enums.Status;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * <p>CWS Key Entity, maps the Key table from the Database.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
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

    @Column(name = "expires", updatable = false)
    private LocalDateTime expires = null;

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

    public void setExpires(final LocalDateTime expires) {
        this.expires = expires;
    }

    public LocalDateTime getExpires() {
        return expires;
    }

    public void setGracePeriod(final Integer gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    public Integer getGracePeriod() {
        return gracePeriod;
    }
}
