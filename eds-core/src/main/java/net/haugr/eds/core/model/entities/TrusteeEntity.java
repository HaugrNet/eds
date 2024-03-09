/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

import net.haugr.eds.api.common.TrustLevel;
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
 * <p>EDS Trustee Entity, maps the Trustee table from the Database.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Entity
@NamedQuery(name = "trust.findByMember",
        query = "select t " +
                "from TrusteeEntity t " +
                "where t.member = :member" +
                "  and t.trustLevel in :permissions " +
                "order by t.id asc")
@NamedQuery(name = "trust.findByMemberAndExternalCircleId",
        query = "select t " +
                "from TrusteeEntity t " +
                "where t.member = :member" +
                "  and t.circle.externalId = :externalCircleId " +
                "  and t.trustLevel in :permissions " +
                "order by t.member.name asc")
@NamedQuery(name = "trustee.findByCircleAndMember",
        query = "select t " +
                "from TrusteeEntity t " +
                "where t.circle.externalId = :ecid" +
                "  and t.member.externalId = :emid")
@NamedQuery(name = "trustee.findByExternalMemberId",
        query = "select t " +
                "from TrusteeEntity t " +
                "where t.member.externalId = :externalMemberId " +
                "order by t.circle.name asc")
@NamedQuery(name = "trustee.findByExternalCircleId",
        query = "select t " +
                "from TrusteeEntity t " +
                "where t.circle.externalId = :externalCircleId " +
                "order by t.member.name asc")
@NamedQuery(name = "trustee.findCirclesByMember",
        query = "select t.circle " +
                "from TrusteeEntity t " +
                "where t.member = :member " +
                "order by t.circle.name asc")
@NamedQuery(name = "trustee.findSharedCircles",
        query = "select t1 " +
                "from TrusteeEntity t1," +
                "     TrusteeEntity t2 " +
                "where t1.circle.id = t2.circle.id" +
                "  and t1.member = :member" +
                "  and t2.member = :requested " +
                "order by t1.circle.name asc")
@NamedQuery(name = "trustee.findByCircle",
        query = "select t " +
                "from TrusteeEntity t " +
                "where t.circle = :circle " +
                "order by t.member.name asc")
@Table(name = "eds_trustees")
public class TrusteeEntity extends EDSEntity {

    @ManyToOne(targetEntity = MemberEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "member_id", referencedColumnName = "id", nullable = false, updatable = false)
    private MemberEntity member = null;

    @ManyToOne(targetEntity = CircleEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "circle_id", referencedColumnName = "id", nullable = false, updatable = false)
    private CircleEntity circle = null;

    @ManyToOne(targetEntity = KeyEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "key_id", referencedColumnName = "id", nullable = false, updatable = false)
    private KeyEntity key = null;

    @Column(name = "trust_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private TrustLevel trustLevel = null;

    @Column(name = "circle_key", nullable = false)
    private String circleKey = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setMember(final MemberEntity member) {
        this.member = member;
    }

    public MemberEntity getMember() {
        return member;
    }

    public void setCircle(final CircleEntity circle) {
        this.circle = circle;
    }

    public CircleEntity getCircle() {
        return circle;
    }

    public void setTrustLevel(final TrustLevel trustLevel) {
        this.trustLevel = trustLevel;
    }

    public TrustLevel getTrustLevel() {
        return trustLevel;
    }

    public void setKey(final KeyEntity key) {
        this.key = key;
    }

    public KeyEntity getKey() {
        return key;
    }

    public void setCircleKey(final String armoredKey) {
        this.circleKey = armoredKey;
    }

    public String getCircleKey() {
        return circleKey;
    }
}
