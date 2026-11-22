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
 * <p>The body of a request which creates a status key (STATUS.md).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_LABEL })
@Schema(description = "The label of a status key to create.")
public final class StatusKeyRequest implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The label of the key. */
    @Schema(description = "A label for the key, so it can be told from others and withdrawn: 1 to 75 characters, unique in the stone.",
            maxLength = Constants.MAX_NAME_LENGTH, examples = { "overview at home" })
    @JsonbProperty(value = Constants.FIELD_LABEL)
    @JsonbNillable
    private String label = null;

    /**
     * Default Constructor.
     */
    public StatusKeyRequest() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Sets the label of the key.
     *
     * @param label The label of the key
     */
    public void setLabel(final String label) {
        this.label = label;
    }

    /**
     * Retrieves the label of the key.
     *
     * @return The label of the key
     */
    public String getLabel() {
        return label;
    }
}
