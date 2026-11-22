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
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.Utilities;

/**
 * <p>What the stone notes about an entry of a feature (FEATURES.md), beside the
 * Data Object which holds its content: the kind, the place in the order of
 * its feature, the entry it is about, whether it is closed, and its author.</p>
 *
 * <p>The author isn't readable in the database: the Member's ID is sealed
 * with a subkey of the Circle Secret, and the owner tag, by which the stone
 * finds a Member's own entries, is a keyed value of the same secret.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Entity
@Table(name = "eds_entries")
public class EntryEntity extends EDSEntity {

    @ManyToOne(targetEntity = MetadataEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "metadata_id", referencedColumnName = "id", nullable = false, updatable = false)
    private MetadataEntity metadata = null;

    @ManyToOne(targetEntity = CircleFeatureEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "circle_feature_id", referencedColumnName = "id", nullable = false, updatable = false)
    private CircleFeatureEntity circleFeature = null;

    @Column(name = "kind", nullable = false, updatable = false, length = Constants.MAX_FEATURE_NAME_LENGTH)
    private String kind = null;

    @Column(name = "position", nullable = false)
    private Long position = null;

    /** The internal ID of the entry which this one is about, or null. */
    @Column(name = "about_id", updatable = false)
    private Long aboutId = null;

    @Column(name = "owner_tag", nullable = false, length = 64)
    private String ownerTag = null;

    @Column(name = "author", nullable = false)
    private byte[] author = null;

    // 0 or 1: not every database has a BOOLEAN, so the column is a number.
    @Column(name = "closed", nullable = false)
    private Integer closed = 0;

    /** When the content or the name was last changed, or null if never. */
    @Column(name = "changed")
    private java.time.LocalDateTime changed = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setMetadata(final MetadataEntity metadata) {
        this.metadata = metadata;
    }

    public MetadataEntity getMetadata() {
        return metadata;
    }

    public void setCircleFeature(final CircleFeatureEntity circleFeature) {
        this.circleFeature = circleFeature;
    }

    public CircleFeatureEntity getCircleFeature() {
        return circleFeature;
    }

    public void setKind(final String kind) {
        this.kind = kind;
    }

    public String getKind() {
        return kind;
    }

    public void setPosition(final Long position) {
        this.position = position;
    }

    public Long getPosition() {
        return position;
    }

    public void setAboutId(final Long aboutId) {
        this.aboutId = aboutId;
    }

    public Long getAboutId() {
        return aboutId;
    }

    public void setOwnerTag(final String ownerTag) {
        this.ownerTag = ownerTag;
    }

    public String getOwnerTag() {
        return ownerTag;
    }

    public void setAuthor(final byte[] author) {
        this.author = Utilities.copy(author);
    }

    public byte[] getAuthor() {
        return Utilities.copy(author);
    }

    public void setClosed(final Boolean closed) {
        this.closed = Boolean.TRUE.equals(closed) ? 1 : 0;
    }

    public Boolean getClosed() {
        return (closed != null) && (closed != 0);
    }

    public void setChanged(final java.time.LocalDateTime changed) {
        this.changed = changed;
    }

    public java.time.LocalDateTime getChanged() {
        return changed;
    }
}
