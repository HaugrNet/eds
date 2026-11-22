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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import net.haugr.eds.api.common.FeatureState;

/**
 * <p>A feature as it is used in a Circle (FEATURES.md): its state, the folder its
 * entries are kept in, and the position the next entry gets.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Entity
@Table(name = "eds_circle_features")
public class CircleFeatureEntity extends EDSEntity {

    @ManyToOne(targetEntity = CircleEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "circle_id", referencedColumnName = "id", nullable = false, updatable = false)
    private CircleEntity circle = null;

    @ManyToOne(targetEntity = FeatureEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "feature_id", referencedColumnName = "id", nullable = false, updatable = false)
    private FeatureEntity feature = null;

    @Column(name = "state", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private FeatureState state = null;

    /** The internal ID of the folder with the entries, a Metadata record. */
    @Column(name = "folder_id", nullable = false, updatable = false)
    private Long folderId = null;

    @Column(name = "next_position", nullable = false)
    private Long nextPosition = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setCircle(final CircleEntity circle) {
        this.circle = circle;
    }

    public CircleEntity getCircle() {
        return circle;
    }

    public void setFeature(final FeatureEntity feature) {
        this.feature = feature;
    }

    public FeatureEntity getFeature() {
        return feature;
    }

    public void setState(final FeatureState state) {
        this.state = state;
    }

    public FeatureState getState() {
        return state;
    }

    public void setFolderId(final Long folderId) {
        this.folderId = folderId;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setNextPosition(final Long nextPosition) {
        this.nextPosition = nextPosition;
    }

    public Long getNextPosition() {
        return nextPosition;
    }
}
