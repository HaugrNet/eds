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

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Contains a map of current settings in this EDS instance, some settings
 * are System specific, others are custom for the client. Of the System
 * specific, not all may be updated once the EDS instance contains Members.</p>
 *
 * <p>Please see {@link EDSResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_SETTINGS, Constants.FIELD_ALLOWED_VALUES })
@Schema(description = "The current settings.")
public final class SettingResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Map of Settings. */
    @Schema(description = "All settings, as key-value pairs.")
    @JsonbProperty(Constants.FIELD_SETTINGS)
    private final Map<String, String> settings = new HashMap<>();

    /** The allowed values of the settings with a fixed choice. */
    @Schema(description = "The values EDS accepts for the settings with a fixed choice (the supported algorithms), by setting key; other settings aren't listed.")
    @JsonbProperty(Constants.FIELD_ALLOWED_VALUES)
    private final Map<String, List<String>> allowedValues = new HashMap<>();

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public SettingResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public SettingResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * Set the Map of Settings.
     *
     * @param settings Map of Settings
     */
    public void setSettings(final Map<String, String> settings) {
        this.settings.putAll(settings);
    }

    /**
     * Retrieves the Map of Settings.
     *
     * @return Map of Settings
     */
    public Map<String, String> getSettings() {
        return new HashMap<>(settings);
    }

    /**
     * Set the allowed values of the settings with a fixed choice.
     *
     * @param allowedValues Setting keys with their allowed values
     */
    public void setAllowedValues(final Map<String, List<String>> allowedValues) {
        allowedValues.forEach((key, values) -> this.allowedValues.put(key, new ArrayList<>(values)));
    }

    /**
     * Retrieves the allowed values of the settings with a fixed choice.
     *
     * @return Setting keys with their allowed values
     */
    public Map<String, List<String>> getAllowedValues() {
        final Map<String, List<String>> copy = new HashMap<>();
        allowedValues.forEach((key, values) -> copy.put(key, new ArrayList<>(values)));
        return copy;
    }
}
