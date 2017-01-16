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
@NamedQueries(
        @NamedQuery(name = "member.findByCredential",
        query = "select m " +
                "from MemberEntity m" +
                " where name = :credential"
))
@Table(name = "members")
public class MemberEntity extends Externable {

    @Column(name = "name", length = 256, unique = true, nullable = false)
    private String name = null;

    @Column(name = "salt", length = 36, unique = true, nullable = false)
    private String salt = null;

    @Column(name = "public_key", length = 256, nullable = false)
    private String publicKey = null;

    @Column(name = "private_key", length = 256, nullable = false)
    private String privateKey = null;

    @Column(name = "algorithm", length = 10, nullable = false)
    private String algorithm = null;

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

    public void setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setKeyPair(final KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }
}
