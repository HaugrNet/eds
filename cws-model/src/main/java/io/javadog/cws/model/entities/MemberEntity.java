/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.security.KeyPair;

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

    @Column(name = "name", length = 256, unique = true, nullable = false)
    private String name = null;

    @Column(name = "salt", length = 36, unique = true, nullable = false)
    private String salt = null;

    @Column(name = "public_key", length = 1024, nullable = false)
    private String publicKey = null;

    @Column(name = "private_key", length = 8192, nullable = false)
    private String privateKey = null;

    @Transient
    private KeyPair keyPair = null;

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

    public void setKeyPair(final KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }
}
