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

import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.common.Constants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * <p>Circles are part of the core functionality of EDS, as all data is assigned
 * or belongs to a Circle. Circles can be considered as a Group or Collection of
 * known and trusted members who may be granted access to the data.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_CIRCLE_NAME,
        Constants.FIELD_CIRCLE_KEY,
        Constants.FIELD_ADDED })
public final class Circle implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** CircleId. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** Circle Name. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_NAME)
    @JsonbNillable
    private String circleName = null;

    // The Circle Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything. For the same reason,
    // it is not used as part of the Standard Object methods, #equals(),
    // #hashCode() and #toString().
    /** Circle Key. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_KEY)
    @JsonbNillable
    private String circleKey = null;

    /** Created Timestamp. */
    @JsonbProperty(value = Constants.FIELD_ADDED)
    @JsonbDateFormat
    @JsonbNillable
    private LocalDateTime added = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public Circle() {
        // Empty Constructor
    }

    /**
     * Sets the CircleId.
     *
     * @param circleId CircleId
     */
    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    /**
     * Retrieves the CircleId.
     *
     * @return CircleId
     */
    public String getCircleId() {
        return circleId;
    }

    /**
     * Sets the CircleName.
     *
     * @param circleName CircleName
     */
    public void setCircleName(final String circleName) {
        this.circleName = circleName;
    }

    /**
     * Retrieves the CircleName.
     *
     * @return CircleName
     */
    public String getCircleName() {
        return circleName;
    }

    /**
     * Sets the CircleKey.
     *
     * @param circleKey CircleKey
     */
    public void setCircleKey(final String circleKey) {
        this.circleKey = circleKey;
    }

    /**
     * Retrieves the CircleKey.
     *
     * @return CircleKey
     */
    public String getCircleKey() {
        return circleKey;
    }

    /**
     * Sets the Created Timestamp.
     *
     * @param added Created Timestamp
     */
    public void setAdded(final LocalDateTime added) {
        this.added = added;
    }

    /**
     * Retrieves the Created Timestamp
     *
     * @return Created Timestamp
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
        return "Circle{" +
                "circleId='" + circleId + '\'' +
                ", circleName='" + circleName + '\'' +
                ", circleKey='" + circleKey + '\'' +
                ", added=" + added +
                '}';
    }
}
