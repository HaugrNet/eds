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
import io.javadog.cws.api.common.Utilities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * <p>CWS Signature Entity, maps the Signature table from the Database.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "signature.readAll",
                    query = "select s from SignatureEntity s"),
        @NamedQuery(name = "signature.findByChecksum",
                    query = "select s from SignatureEntity s " +
                            "where s.checksum = :checksum"),
        @NamedQuery(name = "signature.findByMember",
                    query = "select s from SignatureEntity s " +
                            "where s.member = :member " +
                            "order by s.id desc")
})
@Table(name = "cws_signatures")
public class SignatureEntity extends CWSEntity {

    @ManyToOne(targetEntity = MemberEntity.class, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", referencedColumnName = "id", nullable = false, updatable = false)
    private MemberEntity member = null;

    @Column(name = "public_key", nullable = false, length = 3072)
    private String publicKey = null;

    @Column(name = "checksum", updatable = false, length = Constants.MAX_STRING_LENGTH)
    private String checksum = null;

    @Column(name = "verifications")
    private Long verifications = 0L;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expires", updatable = false)
    private Date expires = null;

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

    public void setExpires(final Date expires) {
        this.expires = Utilities.copy(expires);
    }

    public Date getExpires() {
        return Utilities.copy(expires);
    }
}
