/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Utilities;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serializable;
import java.util.Date;

/**
 * This Object contain information about a Signature in CWS, such as when it
 * expires, verification attempts, when it was added and when it was last used.
 * It also contain the checksum for identification of it.
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_CHECKSUM,
        Constants.FIELD_EXPIRES,
        Constants.FIELD_VERIFICATIONS,
        Constants.FIELD_LAST_VERIFICATION,
        Constants.FIELD_ADDED })
public final class Signature implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @JsonbProperty(value = Constants.FIELD_CHECKSUM, nillable = true)
    private String checksum = null;

    @JsonbProperty(value = Constants.FIELD_EXPIRES, nillable = true)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
    private Date expires = null;

    @JsonbProperty(value = Constants.FIELD_VERIFICATIONS, nillable = true)
    private Long verifications = null;

    @JsonbProperty(value = Constants.FIELD_LAST_VERIFICATION, nillable = true)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
    private Date lastVerification = null;

    @JsonbProperty(value = Constants.FIELD_ADDED, nillable = true)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
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
        this.expires = Utilities.copy(expires);
    }

    public Date getExpires() {
        return Utilities.copy(expires);
    }

    public void setVerifications(final Long verifications) {
        this.verifications = verifications;
    }

    public Long getVerifications() {
        return verifications;
    }

    public void setLastVerification(final Date lastVerification) {
        this.lastVerification = Utilities.copy(lastVerification);
    }

    public Date getLastVerification() {
        return Utilities.copy(lastVerification);
    }

    public void setAdded(final Date added) {
        this.added = Utilities.copy(added);
    }

    public Date getAdded() {
        return Utilities.copy(added);
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
