/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Verifiable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dataType", propOrder = { "name", "type" })
public final class DataType extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final int MAX_LENGTH = 256;
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "type";

    @XmlElement(name = FIELD_NAME, required = true)
    private String name = null;

    @XmlElement(name = FIELD_TYPE, required = true)
    private String type = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    @NotNull
    @Size(min = 1, max = MAX_LENGTH)
    public void setName(final String name) {
        ensureNotNull(FIELD_NAME, name);
        ensureNotEmptyOrTooLong(FIELD_NAME, name, MAX_LENGTH);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @NotNull
    @Size(min = 1, max = MAX_LENGTH)
    public void setType(final String type) {
        ensureNotNull(FIELD_TYPE, type);
        ensureNotEmptyOrTooLong(FIELD_TYPE, type, MAX_LENGTH);
        this.type = type;
    }

    public String getType() {
        return type;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = new ConcurrentHashMap<>();

        checkNotNull(errors, FIELD_NAME, name, "The Name is not defined.");
        checkNotEmpty(errors, FIELD_NAME, name, "The Name may not be empty.");
        checkNotTooLong(errors, FIELD_NAME, name, MAX_LENGTH, "The Name is longer than the allowed " + MAX_LENGTH + " characters.");
        checkNotNull(errors, FIELD_TYPE, type, "The Type is not defined.");
        checkNotEmpty(errors, FIELD_TYPE, type, "The Type may not be empty.");
        checkNotTooLong(errors, FIELD_TYPE, type, MAX_LENGTH, "The Type is longer than the allowed " + MAX_LENGTH + " characters.");

        return errors;
    }
}
