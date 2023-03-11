/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * <p>EDS Signature Entity, maps the Signature table from the Database.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Entity
@NamedQuery(name = "signature.readAll",
        query = "select s from SignatureEntity s")
@NamedQuery(name = "signature.findByChecksum",
        query = "select s from SignatureEntity s " +
                "where s.checksum = :checksum")
@NamedQuery(name = "signature.findByMember",
        query = "select s from SignatureEntity s " +
                "where s.member = :member " +
                "order by s.id desc")
@Table(name = "eds_signatures")
public class SignatureEntity extends EDSEntity {

    @ManyToOne(targetEntity = MemberEntity.class, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", referencedColumnName = "id", nullable = false, updatable = false)
    private MemberEntity member = null;

    @Column(name = "public_key", nullable = false, length = 3072)
    private String publicKey = null;

    @Column(name = "checksum", updatable = false, length = Constants.MAX_STRING_LENGTH)
    private String checksum = null;

    @Column(name = "verifications")
    private Long verifications = 0L;

    @Column(name = "expires", updatable = false)
    private LocalDateTime expires = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setPublicKey(final String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

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

    public void setVerifications(final Long verifications) {
        this.verifications = verifications;
    }

    public Long getVerifications() {
        return verifications;
    }

    public void setExpires(final LocalDateTime expires) {
        this.expires = expires;
    }

    public LocalDateTime getExpires() {
        return expires;
    }
}
