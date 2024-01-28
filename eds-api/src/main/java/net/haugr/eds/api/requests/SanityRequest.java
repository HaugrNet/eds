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

import net.haugr.eds.api.common.Constants;
import java.time.LocalDateTime;
import java.util.Map;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;

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
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The CircleId. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID, nillable = true)
    private String circleId = null;

    /** The Date since last check. */
    @JsonbProperty(value = Constants.FIELD_SINCE, nillable = true)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
    private LocalDateTime since = null;

    // =========================================================================
    // Setters & Getters
    // =========================================================================

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
