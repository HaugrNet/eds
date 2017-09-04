/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "metadata.findByMemberAndExternalId",
                query = "select m " +
                        "from MetadataEntity m " +
                        "inner join TrusteeEntity t on m.circle.id = t.circle.id " +
                        "where t.member.id = :mid" +
                        "  and m.externalId = :eid " +
                        "order by m.name asc, m.id asc"),
        @NamedQuery(name = "metadata.findByMemberAndFolder",
                query = "select m " +
                        "from MetadataEntity m " +
                        "inner join TrusteeEntity t on m.circle.id = t.circle.id " +
                        "where t.member.id = :mid" +
                        "  and m.parentId = :parentId " +
                        "order by m.name asc, m.id asc"),
        @NamedQuery(name = "metadata.findByMemberAndFolderAndType",
                query = "select m " +
                        "from MetadataEntity m " +
                        "inner join TrusteeEntity t on m.circle.id = t.circle.id " +
                        "where t.member.id = :mid" +
                        "  and m.parentId = :parentId" +
                        "  and m.type.name =:typename " +
                        "order by m.name asc, m.id asc"),
        @NamedQuery(name = "metadata.findRootByMemberAndCircle",
                query = "select m " +
                        "from MetadataEntity m " +
                        "inner join TrusteeEntity t on m.circle.id = t.circle.id " +
                        "where t.member.id = :mid" +
                        "  and m.type.name = 'folder' " +
                        "  and m.name = '/' " +
                        "order by m.id asc")
})
@Table(name = "metadata")
public class MetadataEntity extends Externable {

    @Column(name = "parent_id")
    private Long parentId = null;

    @ManyToOne(targetEntity = CircleEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "circle_id",  referencedColumnName = "id", nullable = false, updatable = false)
    private CircleEntity circle = null;

    @ManyToOne(targetEntity = DataTypeEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "datatype_id",  referencedColumnName = "id", nullable = false, updatable = false)
    private DataTypeEntity type = null;

    @Column(name = "name", nullable = false)
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
