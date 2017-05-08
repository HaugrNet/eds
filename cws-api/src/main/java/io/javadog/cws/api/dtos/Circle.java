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
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "circle", propOrder = { "id", "name", "created" })
public final class Circle extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_CREATED = "created";
    private static final int NAME_MIN_LENGTH = 1;
    private static final int NAME_MAX_LENGTH = 75;

    @XmlElement(name = FIELD_ID, required = true)
    private String id = null;

    @XmlElement(name = FIELD_NAME, required = true)
    private String name = null;

    @XmlElement(name = FIELD_CREATED)
    private Date created = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    public void setId(final String id) {
        ensurePattern(FIELD_ID, id, Constants.ID_PATTERN_REGEX);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @NotNull
    @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
    public void setName(final String name) {
        ensureNotNull(FIELD_NAME, name);
        ensureLength(FIELD_NAME, name, NAME_MIN_LENGTH, NAME_MAX_LENGTH);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCreated(final Date created) {
        this.created = new Date(created.getTime());
    }

    public Date getCreated() {
        return new Date(created.getTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = new HashMap<>();

        checkPattern(errors, FIELD_ID, id, Constants.ID_PATTERN_REGEX, "The Circle Id is invalid.");
        checkNotNull(errors, FIELD_NAME, name, "The Name is missing, null or invalid.");
        checkNotEmpty(errors, FIELD_NAME, name, "The Name may not be empty.");
        checkNotTooLong(errors, FIELD_NAME, name, NAME_MAX_LENGTH, "The Name is exceeding the maximum allowed length " + NAME_MAX_LENGTH + '.');

        return errors;
    }
}
