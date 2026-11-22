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
 * <p>The rotation of a Circle Secret under way (PLAN.md, Crypto C6): the
 * Circle has a new secret, and its Data Objects are re-encrypted with it in
 * the background.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_STARTED,
        Constants.FIELD_OBJECTS,
        Constants.FIELD_OBJECTS_DONE })
@Schema(description = "The rotation of the Circle Secret under way: the Circle has a new secret, and its Data Objects are re-encrypted with it.")
public final class CircleRotation implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}, as all DTOs of the API have. */
    @Serial
    @SuppressWarnings("java:S4926")
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** When it started. */
    @Schema(description = "When the rotation started. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_STARTED)
    @JsonbDateFormat
    @JsonbNillable
    // JSON-B leaves out transient fields, so it can't be one.
    @SuppressWarnings("java:S3437")
    private LocalDateTime started = null;

    /** The Data Objects of the Circle. */
    @Schema(description = "The Data Objects of the Circle with content, including thumbnails and entries of features.", examples = { "1200" })
    @JsonbProperty(value = Constants.FIELD_OBJECTS)
    private long objects = 0;

    /** Those on the new secret. */
    @Schema(description = "Of them, those on the new secret already.", examples = { "800" })
    @JsonbProperty(value = Constants.FIELD_OBJECTS_DONE)
    private long objectsDone = 0;

    /**
     * Default Constructor.
     */
    public CircleRotation() {
        // Empty Constructor
    }

    public void setStarted(final LocalDateTime started) {
        this.started = started;
    }

    public LocalDateTime getStarted() {
        return started;
    }

    public void setObjects(final long objects) {
        this.objects = objects;
    }

    public long getObjects() {
        return objects;
    }

    public void setObjectsDone(final long objectsDone) {
        this.objectsDone = objectsDone;
    }

    public long getObjectsDone() {
        return objectsDone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "CircleRotation{started=" + started + ", objects=" + objects + ", objectsDone=" + objectsDone + '}';
    }
}
