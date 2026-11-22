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
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>The status keys of a stone, or a new one (STATUS.md).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_KEYS,
        Constants.FIELD_KEY })
@Schema(description = "The labels of the status keys of the stone, and a key which was just created.")
public final class StatusKeyResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The labels of the status keys. */
    @Schema(description = "The labels of the status keys of the stone.")
    @JsonbProperty(value = Constants.FIELD_KEYS)
    @JsonbNillable
    private List<String> keys = new ArrayList<>(0);

    /** The new status key. */
    @Schema(description = "When creating: the new key, Base64 (URL-safe), 256 bits. It is returned once; EDS keeps only its hash.")
    @JsonbProperty(value = Constants.FIELD_KEY)
    @JsonbNillable
    private String key = null;

    /**
     * Empty Constructor, required for WebServices.
     */
    public StatusKeyResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Constructor for a successful request, with a message.
     *
     * @param returnMessage The EDS Return Message
     */
    public StatusKeyResponse(final String returnMessage) {
        super(returnMessage);
    }

    /**
     * Error Constructor.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public StatusKeyResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    /**
     * Sets the labels of the status keys.
     *
     * @param keys The labels of the status keys
     */
    public void setKeys(final List<String> keys) {
        this.keys = (keys == null) ? null : new ArrayList<>(keys);
    }

    /**
     * Retrieves the labels of the status keys.
     *
     * @return The labels of the status keys
     */
    public List<String> getKeys() {
        return (keys == null) ? null : Collections.unmodifiableList(keys);
    }

    /**
     * Sets the new status key.
     *
     * @param key The new status key
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * Retrieves the new status key.
     *
     * @return The new status key
     */
    public String getKey() {
        return key;
    }
}
