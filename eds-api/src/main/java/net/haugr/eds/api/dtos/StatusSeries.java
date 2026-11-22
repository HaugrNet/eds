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
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import net.haugr.eds.api.common.Constants;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>A series of samples of a stone (STATUS.md).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_INTERVAL,
        Constants.FIELD_SAMPLES })
@Schema(description = "A series of samples of a stone, the oldest first.")
public final class StatusSeries implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The seconds between two samples. */
    @Schema(description = "The seconds between two samples of the series; 0 if the gathering is switched off.", examples = { "5" })
    @JsonbProperty(value = Constants.FIELD_INTERVAL)
    @JsonbNillable
    private Long interval = null;

    /** The samples. */
    @Schema(description = "The samples, the oldest first.")
    @JsonbProperty(value = Constants.FIELD_SAMPLES)
    @JsonbNillable
    private List<StatusSample> samples = new ArrayList<>(0);

    /**
     * Default Constructor.
     */
    public StatusSeries() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Sets the seconds between two samples.
     *
     * @param interval The seconds between two samples
     */
    public void setInterval(final Long interval) {
        this.interval = interval;
    }

    /**
     * Retrieves the seconds between two samples.
     *
     * @return The seconds between two samples
     */
    public Long getInterval() {
        return interval;
    }

    /**
     * Sets the samples.
     *
     * @param samples The samples
     */
    public void setSamples(final List<StatusSample> samples) {
        this.samples = (samples == null) ? null : new ArrayList<>(samples);
    }

    /**
     * Retrieves the samples.
     *
     * @return The samples
     */
    public List<StatusSample> getSamples() {
        return (samples == null) ? null : new ArrayList<>(samples);
    }
}
