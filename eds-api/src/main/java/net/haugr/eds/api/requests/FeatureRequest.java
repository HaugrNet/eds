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
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>The body of a request which declares a feature (FEATURES.md).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_DECLARATION })
@Schema(description = "The declaration of a feature, to add or replace it.")
public final class FeatureRequest implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The declaration of the feature. */
    @Schema(description = "The declaration: a JSON object as text, see the Features operations for its form.", maxLength = Constants.MAX_DECLARATION_LENGTH)
    @JsonbProperty(value = Constants.FIELD_DECLARATION)
    @JsonbNillable
    private String declaration = null;

    /**
     * Default Constructor.
     */
    public FeatureRequest() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Sets the declaration of the feature.
     *
     * @param declaration The declaration of the feature
     */
    public void setDeclaration(final String declaration) {
        this.declaration = declaration;
    }

    /**
     * Retrieves the declaration of the feature.
     *
     * @return The declaration of the feature
     */
    public String getDeclaration() {
        return declaration;
    }
}
