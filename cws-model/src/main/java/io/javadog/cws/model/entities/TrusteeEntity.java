/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.entities;

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
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "trust.findByMemberId",
                query = "select t " +
                        "from TrusteeEntity t " +
                        "where t.member.id = :id"),
        @NamedQuery(name = "trust.findByMemberIdAndExternalCircleId",
                query = "select t " +
                        "from TrusteeEntity t " +
                        "where t.member.id = :id" +
                        "  and t.circle.externalId = :externalCircleId"),
        @NamedQuery(name = "trustee.findByExternalCircleId",
                query = "select t " +
                        "from TrusteeEntity t " +
                        "where t.circle.externalId = :externalCircleId"),
        @NamedQuery(name = "trustee.findCirclesByMember",
                query = "select t.circle " +
                        "from TrusteeEntity t " +
                        "where t.member.id = :memberId " +
                        "order by t.circle.name asc"),
        @NamedQuery(name = "trustee.findSharedCircles",
                query = "select c " +
                        "from CircleEntity c" +
                        "  join TrusteeEntity t1 on c.id = t1.circle.id" +
                        "  join TrusteeEntity t2 on c.id = t2.circle.id " +
                        "where t1.member.id = :member" +
                        "  and t2.member.id = :requested " +
                        "order by c.name asc"),
        @NamedQuery(name = "trustee.findByCircleId",
                query = "select t " +
                        "from TrusteeEntity t " +
                        "where t.circle.id = :circleId")
})
@Table(name = "trustees")
public class TrusteeEntity extends Externable {

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
