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
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * <p>A Session of a Member (PLAN.md, Crypto §2a and "Several sessions"). A
 * Member may have several, one per login: the keyed hash of the Session
 * token for the lookup, the Session copy of the Member's private keys, and
 * the expiry, which is part of the AADs of that copy.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Entity
@NamedQuery(name = "session.findByChecksum",
        query = "select s " +
                "from SessionEntity s " +
                "where s.checksum = :checksum")
@NamedQuery(name = "session.findByMember",
        query = "select s " +
                "from SessionEntity s " +
                "where s.member = :member " +
                "order by s.expire asc, s.id asc")
@NamedQuery(name = "session.removeByMember",
        query = "delete from SessionEntity s " +
                "where s.member = :member")
@NamedQuery(name = "session.removeAll",
        query = "delete from SessionEntity s")
@NamedQuery(name = "session.removeExpired",
        query = "delete from SessionEntity s " +
                "where s.expire <= current_timestamp")
@Table(name = "eds_sessions")
public class SessionEntity extends EDSEntity {

    @ManyToOne(targetEntity = MemberEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "member_id", referencedColumnName = "id", nullable = false, updatable = false)
    private MemberEntity member = null;

    @Column(name = "checksum", unique = true, nullable = false, updatable = false, length = 256)
    private String checksum = null;

    @Column(name = "crypto", nullable = false, updatable = false, length = 16384)
    private String crypto = null;

    @Column(name = "expire", nullable = false, updatable = false)
    private LocalDateTime expire = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setMember(final MemberEntity member) {
        this.member = member;
    }

    public MemberEntity getMember() {
        return member;
    }

    public void setChecksum(final String checksum) {
        this.checksum = checksum;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setCrypto(final String crypto) {
        this.crypto = crypto;
    }

    public String getCrypto() {
        return crypto;
    }

    public void setExpire(final LocalDateTime expire) {
        this.expire = expire;
    }

    public LocalDateTime getExpire() {
        return expire;
    }
}
