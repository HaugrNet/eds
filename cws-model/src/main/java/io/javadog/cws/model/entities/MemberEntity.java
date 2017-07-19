/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.entities;

import io.javadog.cws.common.CWSKey;
import io.javadog.cws.common.enums.KeyAlgorithm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "member.findAll",
                query = "select m " +
                        "from MemberEntity m " +
                        "order by name asc"),
        @NamedQuery(name = "member.findByExternalId",
                query = "select m " +
                        "from MemberEntity m " +
                        "where m.externalId = :externalId " +
                        "order by name asc"),
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
@Table(name = "members")
public class MemberEntity extends Externable {

    @Column(name = "name", unique = true, nullable = false, length = 256)
    private String name = null;

    @Column(name = "salt", unique = true, nullable = false, length = 36)
    private String salt = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm", nullable = false, length = 10)
    private KeyAlgorithm algorithm;

    @Column(name = "public_key", nullable = false, length = 1024)
    private String publicKey = null;

    @Column(name = "private_key", nullable = false, length = 8192)
    private String privateKey = null;

    @Transient
    private CWSKey key = null;

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

    public void setAlgorithm(final KeyAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public KeyAlgorithm getAlgorithm() {
        return algorithm;
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

    public void setKey(final CWSKey key) {
        this.key = key;
    }

    public CWSKey getKey() {
        return key;
    }
}
