/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static io.javadog.cws.api.common.Constants.FIELD_CREATED;
import static io.javadog.cws.api.common.Constants.FIELD_EXPIRES;
import static io.javadog.cws.api.common.Constants.FIELD_LAST_VERIFICATION;
import static io.javadog.cws.api.common.Constants.FIELD_SIGNATURE;
import static io.javadog.cws.api.common.Constants.FIELD_VERIFICATIONS;
import static io.javadog.cws.api.common.Utilities.copy;

import io.javadog.cws.api.common.Constants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "signature", propOrder = { FIELD_SIGNATURE, FIELD_EXPIRES, FIELD_VERIFICATIONS, FIELD_LAST_VERIFICATION, FIELD_CREATED })
public final class Signature implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = FIELD_SIGNATURE)
    private String theSignature = null;

    @XmlElement(name = FIELD_EXPIRES)
    private Date expires = null;

    @XmlElement(name = FIELD_VERIFICATIONS)
    private Long verifications = null;

    @XmlElement(name = FIELD_LAST_VERIFICATION)
    private Date lastVerification = null;

    @XmlElement(name = FIELD_CREATED)
    private Date created = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================


    public void setSignature(final String signature) {
        this.theSignature = signature;
    }

    public String getSignature() {
        return theSignature;
    }

    public void setExpires(final Date expires) {
        this.expires = copy(expires);
    }

    public Date getExpires() {
        return copy(expires);
    }

    public void setVerifications(final Long verifications) {
        this.verifications = verifications;
    }

    public Long getVerifications() {
        return verifications;
    }

    public void setLastVerification(final Date lastVerification) {
        this.lastVerification = copy(lastVerification);
    }

    public Date getLastVerification() {
        return copy(lastVerification);
    }

    public void setCreated(final Date created) {
        this.created = copy(created);
    }

    public Date getCreated() {
        return copy(created);
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Signature)) {
            return false;
        }

        final Signature that = (Signature) obj;
        return Objects.equals(theSignature, that.theSignature) &&
                Objects.equals(expires, that.expires) &&
                Objects.equals(verifications, that.verifications) &&
                Objects.equals(lastVerification, that.lastVerification) &&
                Objects.equals(created, that.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(theSignature, expires, verifications, lastVerification, created);
    }

    @Override
    public String toString() {
        return "Signature{" +
                "signature='" + theSignature + '\'' +
                ", expires=" + expires +
                ", verifications=" + verifications +
                ", lastVerification=" + lastVerification +
                ", created=" + created +
                '}';
    }
}
