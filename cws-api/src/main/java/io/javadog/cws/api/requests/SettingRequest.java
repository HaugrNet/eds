/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Constants.FIELD_SETTINGS;

import io.javadog.cws.api.common.Constants;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "settingRequest", propOrder = FIELD_SETTINGS)
public final class SettingRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @NotNull
    @XmlElement(name = FIELD_SETTINGS, required = true)
    private HashMap<String, String> settings = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setSettings(final Map<String, String> settings) {
        this.settings = new HashMap<>(settings);
    }

    public Map<String, String> getSettings() {
        return (settings == null) ? new HashMap<>(0) : new HashMap<>(settings);
    }
}
