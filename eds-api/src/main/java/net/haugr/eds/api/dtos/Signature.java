/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.api.dtos;

import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.common.Constants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * This Object contains information about a Signature in EDS, such as when it
 * expires, verification attempts, when it was added and when it was last used.
 * It also contains the checksum for identification of it.
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_CHECKSUM,
        Constants.FIELD_EXPIRES,
        Constants.FIELD_VERIFICATIONS,
        Constants.FIELD_LAST_VERIFICATION,
        Constants.FIELD_ADDED })
public final class Signature implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** CheckSum. */
    @JsonbProperty(value = Constants.FIELD_CHECKSUM)
    @JsonbNillable
    private String checksum = null;

    /** Expiration Timestamp. */
    @JsonbProperty(value = Constants.FIELD_EXPIRES)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
    @JsonbNillable
    private LocalDateTime expires = null;

    /** Number of Verifications. */
    @JsonbProperty(value = Constants.FIELD_VERIFICATIONS)
    @JsonbNillable
    private Long verifications = null;

    /** Last Verification Timestamp. */
    @JsonbProperty(value = Constants.FIELD_LAST_VERIFICATION)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
    @JsonbNillable
    private LocalDateTime lastVerification = null;

    /** Added Timestamp. */
    @JsonbProperty(value = Constants.FIELD_ADDED)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
    @JsonbNillable
    private LocalDateTime added = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public Signature() {
        // Empty Constructor
    }

    /**
     * Set the Checksum.
     *
     * @param checksum Checksum
     */
    public void setChecksum(final String checksum) {
        this.checksum = checksum;
    }

    /**
     * Retrieves the Checksum.
     *
     * @return Checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Set the Expiration Timestamp.
     *
     * @param expires Expiration Timestamp
     */
    public void setExpires(final LocalDateTime expires) {
        this.expires = expires;
    }

    /**
     * Retrieves the Expiration timestamp.
     *
     * @return Expiration Timestamp
     */
    public LocalDateTime getExpires() {
        return expires;
    }

    /**
     * Set the Number of Verifications.
     *
     * @param verifications Number of Verifications
     */
    public void setVerifications(final Long verifications) {
        this.verifications = verifications;
    }

    /**
     * Retrieves the Number of Verifications.
     *
     * @return Number of Verifications
     */
    public Long getVerifications() {
        return verifications;
    }

    /**
     * Sets the Last Verification Timestamp.
     *
     * @param lastVerification Last Verification Timestamp
     */
    public void setLastVerification(final LocalDateTime lastVerification) {
        this.lastVerification = lastVerification;
    }

    /**
     * Retrieves the Last Verification Timestamp.
     *
     * @return Last Verification Timestamp
     */
    public LocalDateTime getLastVerification() {
        return lastVerification;
    }

    /**
     * Sets the Added Timestamp.
     *
     * @param added Added Timestamp
     */
    public void setAdded(final LocalDateTime added) {
        this.added = added;
    }

    /**
     * Retrieves the Added Timestamp.
     *
     * @return Added Timestamp
     */
    public LocalDateTime getAdded() {
        return added;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
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
