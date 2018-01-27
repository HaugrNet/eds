/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model.entities;

import static io.javadog.cws.api.common.Constants.MAX_NAME_LENGTH;

import io.javadog.cws.core.enums.KeyAlgorithm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
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
                        "where lower(name) = lower(:name)"),
        @NamedQuery(name = "member.findByNameAndCircle",
                query = "select e.member " +
                        "from TrusteeEntity e " +
                        "where e.member.name = :name" +
                        "  and e.circle.externalId = :externalCircleId")
})
@Table(name = "cws_members")
public class MemberEntity extends Externable {

    @Column(name = "name", unique = true, nullable = false, length = MAX_NAME_LENGTH)
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
}
