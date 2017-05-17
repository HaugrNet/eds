/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;

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
@XmlType(name = "settingRequest", propOrder = "settings")
public final class SettingRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = "settings", required = true)
    private final HashMap<String, String> settings = new HashMap<>();

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    @NotNull
    public void setSettings(final Map<String, String> settings) {
        ensureNotNull("settings", settings);
        this.settings.putAll(settings);
    }

    public Map<String, String> getSettings() {
        return new HashMap<>(settings);
    }


    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = super.validate();

        if (settings == null) {
            errors.put("settings", "The Settings may not be null.");
        }

        return errors;
    }
}
