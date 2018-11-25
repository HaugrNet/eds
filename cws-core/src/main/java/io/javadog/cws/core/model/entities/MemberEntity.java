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
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.MemberRole;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * <p>CWS Member Entity, maps the Member table from the Database.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "member.countMembers",
                query = "select count(m.id) " +
                        "from MemberEntity m"),
        @NamedQuery(name = "member.findByName",
                query = "select m " +
                        "from MemberEntity m " +
                        "where m.name = :name"),
        @NamedQuery(name = "member.findByChecksum",
                query = "select m " +
                        "from MemberEntity m " +
                        "where m.sessionChecksum = :checksum"),
        @NamedQuery(name = "member.findByRole",
                query = "select m " +
                        "from MemberEntity m " +
                        "where m.memberRole = :role " +
                        "order by m.id asc"),
        @NamedQuery(name = "member.findByNameAndCircle",
                query = "select e.member " +
                        "from TrusteeEntity e " +
                        "where e.member.name = :name" +
                        "  and e.circle.externalId = :externalCircleId"),
        @NamedQuery(name = "member.removeExpiredSessions",
                query = "update MemberEntity set" +
                        "  sessionChecksum = null," +
                        "  sessionCrypto = null," +
                        "  sessionExpire = null " +
                        "where sessionExpire > current_timestamp")
})
@Table(name = "cws_members")
public class MemberEntity extends Externable {

    @Column(name = "name", unique = true, nullable = false, length = Constants.MAX_NAME_LENGTH)
    private String name = null;

    @Column(name = "salt", unique = true, nullable = false, length = 36)
    private String salt = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "pbe_algorithm", nullable = false, length = 10)
    private KeyAlgorithm pbeAlgorithm = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "rsa_algorithm", nullable = false, length = 10)
    private KeyAlgorithm rsaAlgorithm = null;

    @Column(name = "external_key")
    private String memberKey = null;

    @Column(name = "public_key", nullable = false, length = 3072)
    private String publicKey = null;

    @Column(name = "private_key", nullable = false, length = 16384)
    private String privateKey = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 10)
    private MemberRole memberRole = null;

    @Column(name = "session_checksum", length = 256)
    private String sessionChecksum = null;

    @Column(name = "session_crypto", length = 16384)
    private String sessionCrypto = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "session_expire")
    private Date sessionExpire = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setName(final String identifier) {
        this.name = identifier;
    }

    public String getName() {
        return name;
    }

    public void setSalt(final String salt) {
        this.salt = salt;
    }

    public String getSalt() {
        return salt;
    }

    public void setPbeAlgorithm(final KeyAlgorithm pbeAlgorithm) {
        this.pbeAlgorithm = pbeAlgorithm;
    }

    public KeyAlgorithm getPbeAlgorithm() {
        return pbeAlgorithm;
    }

    public void setRsaAlgorithm(final KeyAlgorithm algorithm) {
        this.rsaAlgorithm = algorithm;
    }

    public KeyAlgorithm getRsaAlgorithm() {
        return rsaAlgorithm;
    }

    public void setMemberKey(final String memberKey) {
        this.memberKey = memberKey;
    }

    public String getMemberKey() {
        return memberKey;
    }

    public void setPublicKey(final String armoredPublicKey) {
        this.publicKey = armoredPublicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPrivateKey(final String armoredEncryptedPrivateKey) {
        this.privateKey = armoredEncryptedPrivateKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setMemberRole(final MemberRole memberRole) {
        this.memberRole = memberRole;
    }

    public MemberRole getMemberRole() {
        return memberRole;
    }

    public void setSessionChecksum(final String sessionChecksum) {
        this.sessionChecksum = sessionChecksum;
    }

    public String getSessionChecksum() {
        return sessionChecksum;
    }

    public void setSessionCrypto(final String sessionCrypto) {
        this.sessionCrypto = sessionCrypto;
    }

    public String getSessionCrypto() {
        return sessionCrypto;
    }

    public void setSessionExpire(final Date sessionExpire) {
        this.sessionExpire = Utilities.copy(sessionExpire);
    }

    public Date getSessionExpire() {
        return Utilities.copy(sessionExpire);
    }
}
