package io.javadog.cws.model.entities;

import io.javadog.cws.common.enums.Status;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@Table(name = "keys")
public class KeyEntity extends CWSEntity {

    @Column(name = "algorithm", length = 10, nullable = false, updatable = false)
    private String algorithm = null;

    @Column(name = "cipher_mode", length = 10, nullable = false, updatable = false)
    private String cipherMode = null;

    @Column(name = "padding", length = 32, nullable = false, updatable = false)
    private String padding = null;

    @Column(name = "initial_vector", length = 256, nullable = false, updatable = false)
    private String initialVector = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expires", updatable = false)
    private Date expires = null;

    @Column(name = "grace_period", updatable = false)
    private Integer gracePeriod = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setCipherMode(final String cipherMode) {
        this.cipherMode = cipherMode;
    }

    public String getCipherMode() {
        return cipherMode;
    }

    public void setPadding(final String padding) {
        this.padding = padding;
    }

    public String getPadding() {
        return padding;
    }

    public void setInitialVector(final String initialVector) {
        this.initialVector = initialVector;
    }

    public String getInitialVector() {
        return initialVector;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setExpires(final Date expires) {
        this.expires = expires;
    }

    public Date getExpires() {
        return expires;
    }

    public void setGracePeriod(final Integer gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    public Integer getGracePeriod() {
        return gracePeriod;
    }
}
