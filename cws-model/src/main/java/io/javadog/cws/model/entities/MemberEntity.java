package io.javadog.cws.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries(@NamedQuery(
        name = "findByCredential",
        query = "select m " +
                "from MemberEntity m" +
                " where credential = :credential"
))
@Table(name = "members")
public class MemberEntity extends Externable {

    @Column(name = "credential", length = 256, unique = true, nullable = false)
    private String credential = null;

    @Column(name = "public_key", length = 256, nullable = false)
    private String publicKey = null;

    @Column(name = "private_key", length = 256, nullable = false)
    private String privateKey = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setCredential(final String identifier) {
        this.credential = identifier;
    }

    public String getCredential() {
        return credential;
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
