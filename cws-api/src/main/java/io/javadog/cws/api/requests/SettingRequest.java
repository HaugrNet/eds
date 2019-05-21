/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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
 * @since CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "settingRequest")
@XmlType(name = "settingRequest", propOrder = Constants.FIELD_SETTINGS)
public final class SettingRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @NotNull
    @XmlElement(name = Constants.FIELD_SETTINGS, required = true)
    private HashMap<String, String> settings = new HashMap<>(0);

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setSettings(final Map<String, String> settings) {
        this.settings = new HashMap<>(settings);
    }

    public Map<String, String> getSettings() {
        return new HashMap<>(settings);
    }
}
