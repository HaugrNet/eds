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
import net.haugr.eds.api.common.Constants;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>A feature which a stone offers (FEATURES.md): its name, and the declaration
 * with the kinds of its entries and their rules.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_NAME,
        Constants.FIELD_VERSION,
        Constants.FIELD_DECLARATION })
@Schema(description = "A feature which the stone offers: a declaration, with the kinds of its entries and the rules for them. EDS stores and guards the entries, without knowing what they are.")
public final class Feature implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The name of the feature. */
    @Schema(description = "The name of the feature: a lower case letter, then lower case letters, digits or hyphens. Its folder in a Circle is a dot and this name.",
            maxLength = Constants.MAX_FEATURE_NAME_LENGTH, examples = { "calendar" })
    @JsonbProperty(value = Constants.FIELD_NAME)
    @JsonbNillable
    private String name = null;

    /** The version of the declaration. */
    @Schema(description = "The version of the declaration, as given in it.", examples = { "1" })
    @JsonbProperty(value = Constants.FIELD_VERSION)
    @JsonbNillable
    private Integer version = null;

    /** The declaration of the feature. */
    @Schema(description = "The declaration: a JSON object as text, see the Features operations for its form.", maxLength = Constants.MAX_DECLARATION_LENGTH)
    @JsonbProperty(value = Constants.FIELD_DECLARATION)
    @JsonbNillable
    private String declaration = null;

    /**
     * Default Constructor.
     */
    public Feature() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Sets the name of the feature.
     *
     * @param name The name of the feature
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Retrieves the name of the feature.
     *
     * @return The name of the feature
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the version of the declaration.
     *
     * @param version The version of the declaration
     */
    public void setVersion(final Integer version) {
        this.version = version;
    }

    /**
     * Retrieves the version of the declaration.
     *
     * @return The version of the declaration
     */
    public Integer getVersion() {
        return version;
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
