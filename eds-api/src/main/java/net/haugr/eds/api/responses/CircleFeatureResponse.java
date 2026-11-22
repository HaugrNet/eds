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
package net.haugr.eds.api.responses;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.CircleFeature;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>The features of a Circle (FEATURES.md).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_FEATURES })
@Schema(description = "The features of a Circle, with their state.")
public final class CircleFeatureResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The features of the Circle. */
    @Schema(description = "The features of the Circle.")
    @JsonbProperty(value = Constants.FIELD_FEATURES)
    @JsonbNillable
    private List<CircleFeature> features = new ArrayList<>(0);

    /**
     * Empty Constructor, required for WebServices.
     */
    public CircleFeatureResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Constructor for a successful request, with a message.
     *
     * @param returnMessage The EDS Return Message
     */
    public CircleFeatureResponse(final String returnMessage) {
        super(returnMessage);
    }

    /**
     * Error Constructor.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public CircleFeatureResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    /**
     * Sets the features of the Circle.
     *
     * @param features The features of the Circle
     */
    public void setFeatures(final List<CircleFeature> features) {
        this.features = (features == null) ? null : new ArrayList<>(features);
    }

    /**
     * Retrieves the features of the Circle.
     *
     * @return The features of the Circle
     */
    public List<CircleFeature> getFeatures() {
        return (features == null) ? null : Collections.unmodifiableList(features);
    }

    /**
     * Adds a feature of the Circle.
     *
     * @param feature The feature to add
     */
    public void addFeature(final CircleFeature feature) {
        features.add(feature);
    }
}
