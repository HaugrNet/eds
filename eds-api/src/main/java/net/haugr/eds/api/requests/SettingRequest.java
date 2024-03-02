/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

import net.haugr.eds.api.common.Constants;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>This request Object may only be used by the System Administrator, to alter
 * or add/delete custom settings. Existing settings cannot be deleted, and some
 * may only be altered before Members exists. This is to prevent that changing
 * of values that is used to derive the internal Keys can still be properly
 * extracted.</p>
 *
 * <p>The Settings field is mandatory, but can be left empty.</p>
 *
 * <p>Please see {@link Authentication} for information about the account and
 * credentials information.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder(Constants.FIELD_SETTINGS)
public final class SettingRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Map of Settings. */
    @JsonbProperty(value = Constants.FIELD_SETTINGS, nillable = true)
    private HashMap<String, String> settings = new HashMap<>(0);

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public SettingRequest() {
    }

    /**
     * Set the Map of Settings.
     *
     * @param settings Map of Settings
     */
    public void setSettings(final Map<String, String> settings) {
        this.settings = new HashMap<>(settings);
    }

    /**
     * Retrieves the Map of Settings.
     *
     * @return Map of Settings
     */
    public Map<String, String> getSettings() {
        return new HashMap<>(settings);
    }
}
