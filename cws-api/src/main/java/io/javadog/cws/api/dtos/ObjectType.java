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
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "member", propOrder = { "name", "FIELD_TYPE" })
public final class ObjectType extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final int maxLength = 256;
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "type";

    @XmlElement(required = true) private String name = null;
    @XmlElement(required = true) private String type = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    @NotNull
    @Size(min = 1, max = maxLength)
    public void setName(final String name) {
        ensureNotNull(FIELD_NAME, name);
        ensureLength(FIELD_NAME, name, 1, maxLength);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @NotNull
    @Size(min = 1, max = maxLength)
    public void setType(final String type) {
        ensureNotNull(FIELD_TYPE, type);
        ensureLength(FIELD_TYPE, type, 1, maxLength);
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
        final Map<String, String> errors = new HashMap<>();

        checkNotNull(errors, FIELD_NAME, name, "The Name is not defined.");
        checkNotEmpty(errors, FIELD_NAME, name, "The Name may not be empty.");
        checkNotTooLong(errors, FIELD_NAME, name, maxLength, "The Name is longer than the allowed " + maxLength + " characters.");
        checkNotNull(errors, FIELD_TYPE, type, "The Type is not defined.");
        checkNotEmpty(errors, FIELD_TYPE, type, "The Type may not be empty.");
        checkNotTooLong(errors, FIELD_TYPE, type, maxLength, "The Type is longer than the allowed " + maxLength + " characters.");

        return errors;
    }
}
