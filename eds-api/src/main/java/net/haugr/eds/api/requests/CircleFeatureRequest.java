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
package net.haugr.eds.api.requests;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.io.Serializable;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.FeatureState;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>The body of a request which sets the state of a feature in a Circle
 * (FEATURES.md).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_STATE })
@Schema(description = "The state to give a feature in a Circle.")
public final class CircleFeatureRequest implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The state of the feature in the Circle. */
    @Schema(description = "The state: ON, READONLY or OFF.")
    @JsonbProperty(value = Constants.FIELD_STATE)
    @JsonbNillable
    private FeatureState state = null;

    /**
     * Default Constructor.
     */
    public CircleFeatureRequest() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Sets the state of the feature in the Circle.
     *
     * @param state The state of the feature in the Circle
     */
    public void setState(final FeatureState state) {
        this.state = state;
    }

    /**
     * Retrieves the state of the feature in the Circle.
     *
     * @return The state of the feature in the Circle
     */
    public FeatureState getState() {
        return state;
    }
}
