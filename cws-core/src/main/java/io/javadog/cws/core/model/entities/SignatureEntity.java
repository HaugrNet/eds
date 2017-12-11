/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model.entities;

import static io.javadog.cws.api.common.Constants.MAX_STRING_LENGTH;

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

    @ManyToOne(targetEntity = MemberEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "member_id", referencedColumnName = "id", nullable = false, updatable = false)
    private MemberEntity member = null;

    @Column(name = "checksum", updatable = false, length = MAX_STRING_LENGTH)
    private String checksum = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expires", updatable = false)
    private Date expires = null;

    @Column(name = "verifications")
    private Long verifications = 0L;

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

    public void setExpires(final Date expires) {
        this.expires = (expires != null) ? new Date(expires.getTime()) : null;
    }

    public Date getExpires() {
        return (expires != null) ? new Date(expires.getTime()) : null;
    }

    public void setVerifications(final Long verifications) {
        this.verifications = verifications;
    }

    public Long getVerifications() {
        return verifications;
    }
}
