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
package net.haugr.eds.api.requests;

import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.common.Constants;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Map;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * <p>The Sanity Request Object is needed to perform the Sanity EDS Request,
 * which is a management request to see if any Data Object have been corrupted
 * and thus rendered useless. The CircleId is an optional field, serves a way
 * to limit the scope of the Objects retrieved.</p>
 *
 * <p>Please see {@link Authentication} for information about the account and
 * credentials information.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_CIRCLE_ID, Constants.FIELD_SINCE })
public final class SanityRequest extends Authentication implements CircleIdRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The CircleId. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** The Date since last check. */
    @JsonbProperty(value = Constants.FIELD_SINCE)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
    @JsonbNillable
    private LocalDateTime since = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public SanityRequest() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCircleId() {
        return circleId;
    }

    /**
     * Set the Date since last check.
     *
     * @param since Data since last check
     */
    public void setSince(final LocalDateTime since) {
        this.since = since;
    }

    /**
     * Retrieves the Date since last check.
     *
     * @return Date since last check
     */
    public LocalDateTime getSince() {
        return since;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = super.validate();

        checkValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "The Circle Id is invalid.");

        return errors;
    }
}
