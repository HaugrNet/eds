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

import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * <p>EDS Key Entity, maps the Key table from the Database: a Circle Secret
 * of a Circle. The secret itself is only kept wrapped, with the Trustees; a
 * Circle has one ACTIVE secret, and while it is rotated also the RETIRING
 * one (PLAN.md, Crypto C6).</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Entity
@NamedQuery(name = "key.findByCircleAndStatus",
        query = "select k " +
                "from KeyEntity k " +
                "where k.circle = :circle" +
                "  and k.status = :status " +
                "order by k.id desc")
@NamedQuery(name = "key.findForCircles",
        query = "select k " +
                "from KeyEntity k " +
                "where k.circle in :circles" +
                "  and k.status = :status")
@Table(name = "eds_keys")
public class KeyEntity extends EDSEntity {

    @ManyToOne(targetEntity = CircleEntity.class, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "circle_id", referencedColumnName = "id", nullable = false, updatable = false)
    private CircleEntity circle = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm", nullable = false, updatable = false, length = 10)
    private KeyAlgorithm algorithm = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setCircle(final CircleEntity circle) {
        this.circle = circle;
    }

    public CircleEntity getCircle() {
        return circle;
    }

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

}
