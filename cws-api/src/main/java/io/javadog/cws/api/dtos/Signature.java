/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

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
 * This Object contain information about a Signature in CWS, such as when it
 * expires, verification attempts, when it was added and when it was last used.
 * It also contain the checksum for identification of it.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = Constants.FIELD_SIGNATURE, propOrder = { Constants.FIELD_CHECKSUM, Constants.FIELD_EXPIRES, Constants.FIELD_VERIFICATIONS, Constants.FIELD_LAST_VERIFICATION, Constants.FIELD_ADDED })
public final class Signature implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_CHECKSUM)
    private String checksum = null;

    @XmlElement(name = Constants.FIELD_EXPIRES)
    private Date expires = null;

    @XmlElement(name = Constants.FIELD_VERIFICATIONS)
    private Long verifications = null;

    @XmlElement(name = Constants.FIELD_LAST_VERIFICATION)
    private Date lastVerification = null;

    @XmlElement(name = Constants.FIELD_ADDED)
    private Date added = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setChecksum(final String checksum) {
        this.checksum = checksum;
    }

    public String getChecksum() {
        return checksum;
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

    public void setAdded(final Date added) {
        this.added = copy(added);
    }

    public Date getAdded() {
        return copy(added);
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
        return Objects.equals(checksum, that.checksum) &&
                Objects.equals(expires, that.expires) &&
                Objects.equals(verifications, that.verifications) &&
                Objects.equals(lastVerification, that.lastVerification) &&
                Objects.equals(added, that.added);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checksum, expires, verifications, lastVerification, added);
    }

    @Override
    public String toString() {
        return "Signature{" +
                "checksum='" + checksum + '\'' +
                ", expires=" + expires +
                ", verifications=" + verifications +
                ", lastVerification=" + lastVerification +
                ", added=" + added +
                '}';
    }
}
