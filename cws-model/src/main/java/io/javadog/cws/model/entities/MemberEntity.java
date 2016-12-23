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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

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
public class MemberEntity {

    @Id
    @SequenceGenerator(name = "memberSequence", sequenceName = "member_sequence")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "memberSequence")
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false, updatable = false)
    private String externalId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "armored_public_key", nullable = false)
    private String armoredPublicKey;

    @Column(name = "armored_encrypted_private_key", nullable = false)
    private String armoredEncryptedPrivateKey;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modified", nullable = false)
    private Date modified;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created", nullable = false, updatable = false)
    private Date created;

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
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

    public void setModified(final Date modified) {
        this.modified = modified;
    }

    public Date getModified() {
        return modified;
    }

    public void setCreated(final Date created) {
        this.created = created;
    }

    public Date getCreated() {
        return created;
    }
}
