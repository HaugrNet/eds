/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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

import io.javadog.cws.api.common.Constants;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * <p>CWS Metadata Entity, maps the Metadata table from the Database.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
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
@NamedQuery(name = "metadata.findInFolder",
        query = "select m " +
                "from MetadataEntity m," +
                "     TrusteeEntity t " +
                "where m.circle.id = t.circle.id" +
                "  and t.member = :member" +
                "  and m.parentId = :parentId" +
                "  and lower(m.name) = lower(:name)")
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
@NamedQuery(name = "metadata.readInventoryRecords",
        query = "select m " +
                "from MetadataEntity m " +
                "where m.type.name <> 'folder' " +
                "order by m.id desc")
@NamedQuery(name = "metadata.countInventoryRecords",
        query = "select count(m.id) " +
                "from MetadataEntity m " +
                "where m.type.name <> 'folder'")
@Table(name = "cws_metadata")
public class MetadataEntity extends Externable {

    @Column(name = "parent_id")
    private Long parentId = null;

    @ManyToOne(targetEntity = CircleEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "circle_id", referencedColumnName = "id", nullable = false, updatable = false)
    private CircleEntity circle = null;

    @ManyToOne(targetEntity = DataTypeEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "datatype_id", referencedColumnName = "id", nullable = false, updatable = false)
    private DataTypeEntity type = null;

    @Column(name = "name", length = Constants.MAX_NAME_LENGTH)
    private String name = null;

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
}
