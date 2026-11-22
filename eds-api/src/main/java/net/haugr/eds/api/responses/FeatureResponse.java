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
import net.haugr.eds.api.dtos.Feature;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>The features which a stone offers (FEATURES.md).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_FEATURES })
@Schema(description = "The features which the stone offers.")
public final class FeatureResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The features. */
    @Schema(description = "The features.")
    @JsonbProperty(value = Constants.FIELD_FEATURES)
    @JsonbNillable
    private List<Feature> features = new ArrayList<>(0);

    /**
     * Empty Constructor, required for WebServices.
     */
    public FeatureResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Constructor for a successful request, with a message.
     *
     * @param returnMessage The EDS Return Message
     */
    public FeatureResponse(final String returnMessage) {
        super(returnMessage);
    }

    /**
     * Error Constructor.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public FeatureResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    /**
     * Sets the features.
     *
     * @param features The features
     */
    public void setFeatures(final List<Feature> features) {
        this.features = (features == null) ? null : new ArrayList<>(features);
    }

    /**
     * Retrieves the features.
     *
     * @return The features
     */
    public List<Feature> getFeatures() {
        return (features == null) ? null : Collections.unmodifiableList(features);
    }

    /**
     * Adds a feature.
     *
     * @param feature The feature to add
     */
    public void addFeature(final Feature feature) {
        features.add(feature);
    }
}
