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
        Constants.FIELD_AVATAR,
        Constants.FIELD_CIRCLE_KEY,
        Constants.FIELD_ADDED,
        Constants.FIELD_SECRET_ROTATED,
        Constants.FIELD_ROTATION_DUE,
        Constants.FIELD_ROTATION })
@Schema(description = "A Circle: a group of Members sharing data, which is encrypted with the Circle's key.")
public final class Circle implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** CircleId. */
    @Schema(description = "ID of the Circle.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** Circle Name. */
    @Schema(description = "Name of the Circle, unique in EDS.", maxLength = Constants.MAX_NAME_LENGTH, examples = { "Project Phoenix" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_NAME)
    @JsonbNillable
    private String circleName = null;

    /** The Avatar. */
    @Schema(description = "The Circle's avatar: the ID of a picture the clients know (e.g. `longship`), or null for none.",
            pattern = Constants.AVATAR_PATTERN_REGEX, maxLength = Constants.MAX_AVATAR_LENGTH, examples = { "longship" })
    @JsonbProperty(value = Constants.FIELD_AVATAR)
    @JsonbNillable
    private String avatar = null;

    // The Circle Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything. For the same reason,
    // it is not used as part of the Standard Object methods, #equals(),
    // #hashCode() and #toString().
    /** Circle Key. */
    @Schema(description = "Optional external key a client can store with the Circle, e.g. for its own encryption. EDS stores it encrypted and returns it only to the Circle's Trustees.")
    @JsonbProperty(value = Constants.FIELD_CIRCLE_KEY)
    @JsonbNillable
    private String circleKey = null;

    /** Created Timestamp. */
    @Schema(description = "When the record was created. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_ADDED)
    @JsonbDateFormat
    @JsonbNillable
    private LocalDateTime added = null;

    /** When the current Circle Secret was made. */
    @Schema(description = "When the current Circle Secret was made, by the creation of the Circle or the last rotation (PLAN.md, Crypto C6). "
            + "In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_SECRET_ROTATED)
    @JsonbDateFormat
    @JsonbNillable
    // JSON-B leaves out transient fields, so it can't be one; like the other
    // times of the DTOs, it is written as text.
    @SuppressWarnings("java:S3437")
    private LocalDateTime secretRotated = null;

    /** When the Circle Secret is to be rotated next. */
    @Schema(description = "When the Circle Secret is to be rotated next: a period after the last rotation, or the time a Trustee left or was "
            + "removed. Once it has passed, the rotation starts at the next request of a Trustee which reads the Circle's secret. "
            + "Null if no rotation is planned. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2027-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_ROTATION_DUE)
    @JsonbDateFormat
    @JsonbNillable
    // As secretRotated.
    @SuppressWarnings("java:S3437")
    private LocalDateTime rotationDue = null;

    /** The rotation under way. */
    @Schema(description = "The rotation of the Circle Secret under way; null when none is.")
    @JsonbProperty(value = Constants.FIELD_ROTATION)
    @JsonbNillable
    private CircleRotation rotation = null;

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

    public void setSecretRotated(final LocalDateTime secretRotated) {
        this.secretRotated = secretRotated;
    }

    public LocalDateTime getSecretRotated() {
        return secretRotated;
    }

    public void setRotationDue(final LocalDateTime rotationDue) {
        this.rotationDue = rotationDue;
    }

    public LocalDateTime getRotationDue() {
        return rotationDue;
    }

    public void setRotation(final CircleRotation rotation) {
        this.rotation = rotation;
    }

    public CircleRotation getRotation() {
        return rotation;
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

    /**
     * The Avatar
     *
     * @param avatar The Avatar
     */
    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    /**
     * The Avatar
     *
     * @return The Avatar
     */
    public String getAvatar() {
        return avatar;
    }
}
