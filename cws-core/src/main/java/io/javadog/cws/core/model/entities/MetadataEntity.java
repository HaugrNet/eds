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

import io.javadog.cws.api.common.Constants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * <p>CWS Metadata Entity, maps the Metadata table from the Database.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "metadata.findByMemberAndExternalId",
                query = "select m " +
                        "from MetadataEntity m," +
                        "     TrusteeEntity t " +
                        "where m.circle.id = t.circle.id" +
                        "  and t.member.id = :mid" +
                        "  and m.externalId = :eid " +
                        "order by m.id desc"),
        @NamedQuery(name = "metadata.findByMemberAndFolder",
                query = "select m " +
                        "from MetadataEntity m," +
                        "     TrusteeEntity t " +
                        "where m.circle.id = t.circle.id" +
                        "  and t.member = :member" +
                        "  and m.parentId = :parentId " +
                        "order by m.id desc"),
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
                        "order by m.id desc"),
        @NamedQuery(name = "metadata.findInFolder",
                query = "select m " +
                        "from MetadataEntity m," +
                        "     TrusteeEntity t " +
                        "where m.circle.id = t.circle.id" +
                        "  and t.member = :member" +
                        "  and m.parentId = :parentId" +
                        "  and lower(m.name) = lower(:name)"),
        @NamedQuery(name = "metadata.findByNameAndFolder",
                query = "select m " +
                        "from MetadataEntity m " +
                        "where m.id <> :id" +
                        "  and m.name = :name" +
                        "  and m.parentId = :parentId"),
        @NamedQuery(name = "metadata.countFolderContent",
                query = "select count(m.id) " +
                        "from MetadataEntity m " +
                        "where m.parentId = :parentId")
})
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
