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

import io.javadog.cws.api.common.TrustLevel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * <p>CWS Trustee Entity, maps the Trustee table from the Database.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "trust.findByMember",
                query = "select t " +
                        "from TrusteeEntity t " +
                        "where t.member = :member" +
                        "  and t.trustLevel in :permissions " +
                        "order by t.id asc"),
        @NamedQuery(name = "trust.findByMemberAndExternalCircleId",
                query = "select t " +
                        "from TrusteeEntity t " +
                        "where t.member = :member" +
                        "  and t.circle.externalId = :externalCircleId " +
                        "  and t.trustLevel in :permissions " +
                        "order by t.member.name asc"),
        @NamedQuery(name = "trustee.findByCircleAndMember",
                query = "select t " +
                        "from TrusteeEntity t " +
                        "where t.circle.externalId = :ecid" +
                        "  and t.member.externalId = :emid"),
        @NamedQuery(name = "trustee.findByExternalCircleId",
                query = "select t " +
                        "from TrusteeEntity t " +
                        "where t.circle.externalId = :externalCircleId"),
        @NamedQuery(name = "trustee.findCirclesByMember",
                query = "select t.circle " +
                        "from TrusteeEntity t " +
                        "where t.member = :member " +
                        "order by t.circle.name asc"),
        @NamedQuery(name = "trustee.findSharedCircles",
                query = "select t1 " +
                        "from TrusteeEntity t1," +
                        "     TrusteeEntity t2 " +
                        "where t1.circle.id = t2.circle.id" +
                        "  and t1.member = :member" +
                        "  and t2.member = :requested " +
                        "order by t1.circle.name asc"),
        @NamedQuery(name = "trustee.findByCircle",
                query = "select t " +
                        "from TrusteeEntity t " +
                        "where t.circle = :circle " +
                        "order by t.member.name asc")
})
@Table(name = "cws_trustees")
public class TrusteeEntity extends CWSEntity {

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
