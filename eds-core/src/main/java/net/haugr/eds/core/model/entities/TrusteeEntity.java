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

import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
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
 * <p>EDS Trustee Entity maps the Trustee table from the Database.</p>
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
                "order by lower(t.member.name) asc")
@NamedQuery(name = "trustee.findByCircleAndMember",
        query = "select t " +
                "from TrusteeEntity t " +
                "where t.circle.externalId = :ecid" +
                "  and t.member.externalId = :emid")
@NamedQuery(name = "trustee.findByExternalMemberId",
        query = "select t " +
                "from TrusteeEntity t " +
                "where t.member.externalId = :externalMemberId " +
                "order by lower(t.circle.name) asc")
@NamedQuery(name = "trustee.findByExternalCircleId",
        query = "select t " +
                "from TrusteeEntity t " +
                "where t.circle.externalId = :externalCircleId " +
                "order by lower(t.member.name) asc")
@NamedQuery(name = "trustee.findCirclesByMember",
        query = "select t.circle " +
                "from TrusteeEntity t " +
                "where t.member = :member " +
                "order by lower(t.circle.name) asc")
@NamedQuery(name = "trustee.findSharedCircles",
        query = "select t1 " +
                "from TrusteeEntity t1," +
                "     TrusteeEntity t2 " +
                "where t1.circle.id = t2.circle.id" +
                "  and t1.member = :member" +
                "  and t2.member = :requested " +
                "order by lower(t1.circle.name) asc")
@NamedQuery(name = "trustee.findCircleNamesOfLastTrustee",
        query = "select t.circle.name " +
                "from TrusteeEntity t " +
                "where t.member = :member" +
                "  and not exists (select o.id" +
                "                  from TrusteeEntity o" +
                "                  where o.circle = t.circle" +
                "                    and o.member <> t.member) " +
                "order by lower(t.circle.name) asc")
@NamedQuery(name = "trustee.findByCircle",
        query = "select t " +
                "from TrusteeEntity t " +
                "where t.circle = :circle " +
                "order by lower(t.member.name) asc")
@Table(name = "eds_trustees")
public class TrusteeEntity extends EDSEntity {

    @ManyToOne(targetEntity = MemberEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "member_id", referencedColumnName = "id", nullable = false, updatable = false)
    private MemberEntity member = null;

    @ManyToOne(targetEntity = CircleEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "circle_id", referencedColumnName = "id", nullable = false, updatable = false)
    private CircleEntity circle = null;

    @ManyToOne(targetEntity = KeyEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "key_id", referencedColumnName = "id", nullable = false)
    private KeyEntity key = null;

    @Column(name = "trust_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private TrustLevel trustLevel = null;

    @Column(name = "circle_key", nullable = false)
    private String circleKey = null;

    /**
     * The secret of the Member in the Circle (E2E.md): text of a client,
     * which EDS doesn't look into, sealed with a subkey of the Circle Secret.
     */
    @Column(name = "external_key")
    private byte[] externalKey = null;

    /**
     * The retiring Circle Secret, while it is rotated (PLAN.md, Crypto C6):
     * the objects which aren't re-encrypted yet are still on it.
     */
    @ManyToOne(targetEntity = KeyEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "previous_key_id", referencedColumnName = "id")
    private KeyEntity previousKey = null;

    /** The retiring Circle Secret, wrapped as {@link #circleKey}. */
    @Column(name = "previous_circle_key")
    private String previousCircleKey = null;

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

    public void setPreviousKey(final KeyEntity previousKey) {
        this.previousKey = previousKey;
    }

    public KeyEntity getPreviousKey() {
        return previousKey;
    }

    public void setPreviousCircleKey(final String previousCircleKey) {
        this.previousCircleKey = previousCircleKey;
    }

    public String getPreviousCircleKey() {
        return previousCircleKey;
    }

    public void setExternalKey(final byte[] externalKey) {
        this.externalKey = Utilities.copy(externalKey);
    }

    public byte[] getExternalKey() {
        return Utilities.copy(externalKey);
    }
}
