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

import net.haugr.eds.api.common.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * <p>EDS Metadata Entity, maps the Metadata table from the Database.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Entity
@NamedQuery(name = "metadata.findByMemberAndExternalId",
        query = "select m " +
                "from MetadataEntity m," +
                "     TrusteeEntity t " +
                "where m.circle.id = t.circle.id" +
                "  and t.member.id = :mid" +
                "  and m.externalId = :eid " +
                "order by m.id desc")
@NamedQuery(name = "metadata.findByMemberAndName",
        query = "select m " +
                "from MetadataEntity m," +
                "     TrusteeEntity t " +
                "where m.circle.id = t.circle.id" +
                "  and t.member.id = :mid" +
                "  and m.name = :name " +
                "order by m.id desc")
@NamedQuery(name = "metadata.findByMemberAndFolder",
        query = "select m " +
                "from MetadataEntity m," +
                "     TrusteeEntity t " +
                "where m.circle.id = t.circle.id" +
                "  and t.member = :member" +
                "  and m.parentId = :parentId " +
                "order by m.id desc")
@NamedQuery(name = "metadata.findRootByMemberAndCircle",
        query = "select m " +
                "from MetadataEntity m," +
                "     TrusteeEntity t " +
                "where m.circle.id = t.circle.id" +
                "  and t.member.id = :mid" +
                "  and m.circle.externalId = :cid" +
                "  and m.type.name = 'folder'" +
                "  and m.name = '/'" +
                "  and m.parentId = 0 " +
                "order by m.id desc")
@NamedQuery(name = "metadata.findVisibleByMemberAndFolder",
        query = "select m " +
                "from MetadataEntity m," +
                "     TrusteeEntity t " +
                "where m.circle.id = t.circle.id" +
                "  and t.member = :member" +
                "  and m.parentId = :parentId" +
                "  and m.name not like '.%' " +
                "order by m.id desc")
@NamedQuery(name = "metadata.countVisibleFolderContent",
        query = "select count(m.id) " +
                "from MetadataEntity m " +
                "where m.parentId = :parentId" +
                "  and m.name not like '.%'")
@NamedQuery(name = "metadata.findThumbnails",
        query = "select m " +
                "from MetadataEntity m " +
                "where m.parentId in :parentIds" +
                "  and m.type.name = 'thumbnail'")
@NamedQuery(name = "metadata.findInFolder",
        query = "select m " +
                "from MetadataEntity m," +
                "     TrusteeEntity t " +
                "where m.circle.id = t.circle.id" +
                "  and t.member = :member" +
                "  and m.parentId = :parentId" +
                "  and lower(m.name) = lower(:name)")
@NamedQuery(name = "metadata.findByNameKeyInFolder",
        query = "select m " +
                "from MetadataEntity m " +
                "where m.id <> :id" +
                "  and m.nameKey = :nameKey" +
                "  and m.parentId = :parentId")
@NamedQuery(name = "metadata.findByNameAndFolder",
        query = "select m " +
                "from MetadataEntity m " +
                "where m.id <> :id" +
                "  and m.name = :name" +
                "  and m.parentId = :parentId")
@NamedQuery(name = "metadata.countFolderContent",
        query = "select count(m.id) " +
                "from MetadataEntity m " +
                "where m.parentId = :parentId")
@Table(name = "eds_metadata")
public class MetadataEntity extends Externable {

    @Column(name = "parent_id")
    private Long parentId = null;

    @ManyToOne(targetEntity = CircleEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "circle_id", referencedColumnName = "id", nullable = false, updatable = false)
    private CircleEntity circle = null;

    @ManyToOne(targetEntity = DataTypeEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "datatype_id", referencedColumnName = "id", nullable = false, updatable = false)
    private DataTypeEntity type = null;

    @Column(name = "name", length = Constants.MAX_DATA_NAME_LENGTH)
    private String name = null;

    /**
     * The keyed hash of the name a client of a chamber sends (E2E.md), as the
     * name itself is ciphertext which differs each time. Unique within the
     * folder, like the name; null where none was sent.
     */
    @Column(name = "name_key", length = Constants.MAX_NAME_KEY_LENGTH)
    private String nameKey = null;

    @Column(name = "media_type")
    private String mediaType = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setCircle(final CircleEntity circle) {
        this.circle = circle;
    }

    public CircleEntity getCircle() {
        return circle;
    }

    public void setType(final DataTypeEntity type) {
        this.type = type;
    }

    public DataTypeEntity getType() {
        return type;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setNameKey(final String nameKey) {
        this.nameKey = nameKey;
    }

    public String getNameKey() {
        return nameKey;
    }

    public void setMediaType(final String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return mediaType;
    }
}
