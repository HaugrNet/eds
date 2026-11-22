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

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import net.haugr.eds.api.common.Constants;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>The numbers of a stone at one time (STATUS.md), by their names. A
 * client shows the numbers it knows and passes over the others, so a
 * further number is an addition, not a change.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_SEQUENCE,
        Constants.FIELD_TIME,
        Constants.FIELD_VALUES })
@Schema(description = "The numbers of a stone at one time, by their names. Numbers which a client doesn't know are passed over.")
public final class StatusSample implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The sequence of the sample. */
    @Schema(description = "The place of the sample in its series; it grows with every sample.", examples = { "1234" })
    @JsonbProperty(value = Constants.FIELD_SEQUENCE)
    @JsonbNillable
    private Long sequence = null;

    /** The time of the sample. */
    @Schema(description = "When the sample was taken, or for a step of the day when the step began. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_TIME)
    @JsonbDateFormat
    @JsonbNillable
    // A false positive: LocalDateTime is written by value (a serialization proxy), so its
    // identity is never relied on; transient would drop the field from JSON-B and OpenAPI.
    @SuppressWarnings("java:S3437")
    private LocalDateTime time = null;

    /** The numbers by their names. */
    @Schema(description = "The numbers by their names, see the Status operations.")
    @JsonbProperty(value = Constants.FIELD_VALUES)
    @JsonbNillable
    private Map<String, Double> values = null;

    /**
     * Default Constructor.
     */
    public StatusSample() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Sets the sequence of the sample.
     *
     * @param sequence The sequence of the sample
     */
    public void setSequence(final Long sequence) {
        this.sequence = sequence;
    }

    /**
     * Retrieves the sequence of the sample.
     *
     * @return The sequence of the sample
     */
    public Long getSequence() {
        return sequence;
    }

    /**
     * Sets the time of the sample.
     *
     * @param time The time of the sample
     */
    public void setTime(final LocalDateTime time) {
        this.time = time;
    }

    /**
     * Retrieves the time of the sample.
     *
     * @return The time of the sample
     */
    public LocalDateTime getTime() {
        return time;
    }

    /**
     * Sets the numbers by their names.
     *
     * @param values The numbers by their names
     */
    public void setValues(final Map<String, Double> values) {
        this.values = (values == null) ? null : new LinkedHashMap<>(values);
    }

    /**
     * Retrieves the numbers by their names.
     *
     * @return The numbers by their names
     */
    public Map<String, Double> getValues() {
        return (values == null) ? null : new LinkedHashMap<>(values);
    }
}
