package io.javadog.cws.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries(@NamedQuery(
        name = "findByName",
        query = "select m " +
                "from MemberEntity m" +
                " where name = :name"
))
@Table(name = "members")
public class MemberEntity extends CWSEntity {

    @Id
    @SequenceGenerator(name = "memberSequence", sequenceName = "member_sequence")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "memberSequence")
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private Long id;

    @Column(name = "credential", unique = true, nullable = false)
    private String credential;

    @Column(name = "name")
    private String name;

    @Column(name = "armored_public_key", nullable = false)
    private String armoredPublicKey;

    @Column(name = "armored_encrypted_private_key", nullable = false)
    private String armoredEncryptedPrivateKey;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setCredential(final String identifier) {
        this.credential = identifier;
    }

    public String getCredential() {
        return credential;
    }

    public void setName(final String commonName) {
        this.name = commonName;
    }

    public String getName() {
        return name;
    }

    public void setArmoredPublicKey(final String armoredPublicKey) {
        this.armoredPublicKey = armoredPublicKey;
    }

    public String getArmoredPublicKey() {
        return armoredPublicKey;
    }

    public void setArmoredEncryptedPrivateKey(final String armoredEncryptedPrivateKey) {
        this.armoredEncryptedPrivateKey = armoredEncryptedPrivateKey;
    }

    public String getArmoredEncryptedPrivateKey() {
        return armoredEncryptedPrivateKey;
    }
}
