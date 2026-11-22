/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.common.Constants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * The Sanity Object contains information about a Data record, which has failed
 * the sanity check, i.e., the encrypted bytes have been changed, so it is no
 * longer possible to decrypt the Object. The information returned includes the
 * ID of the Data Object which failed the sanity check, and the date at which it
 * failed it.
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_DATA_ID, Constants.FIELD_CHANGED })
@Schema(description = "A Data Object that failed the sanity check: its stored bytes have changed, so it can no longer be decrypted. Any entry means the storage can't be trusted.")
public final class Sanity implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** DataId. */
    @Schema(description = "ID of the Data Object.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_DATA_ID)
    @JsonbNillable
    private String dataId = null;

    /** Changed. */
    @Schema(description = "When the Data Object failed the sanity check. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_CHANGED)
    @JsonbDateFormat
    @JsonbNillable
    private LocalDateTime changed = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public Sanity() {
        // Empty Constructor
    }

    /**
     * Set the DataId.
     *
     * @param dataId DataId
     */
    public void setDataId(final String dataId) {
        this.dataId = dataId;
    }

    /**
     * Retrieves the DataId.
     *
     * @return DataId
     */
    public String getDataId() {
        return dataId;
    }

    /**
     * Sets the Changed Timestamp.
     *
     * @param changed Changed Timestamp
     */
    public void setChanged(final LocalDateTime changed) {
        this.changed = changed;
    }

    /**
     * Retrieves the Changed Timestamp.
     *
     * @return Changed Timestamp
     */
    public LocalDateTime getChanged() {
        return changed;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Sanity{" +
                "dataId='" + dataId + '\'' +
                ", changed=" + changed +
                '}';
    }
}
