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
@XmlType(name = "objectData", propOrder = { "id", "data", "objectType", "added" })
public final class ObjectData extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_ID = "id";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_OBJECT_TYPE = "objectType";
    private static final String FIELD_ADDED = "added";

    @XmlElement(name = FIELD_ID, required = true, nillable = true)
    private String id = null;

    @XmlElement(name = FIELD_DATA, required = true, nillable = true)
    private byte[] data = null;

    @XmlElement(name = FIELD_OBJECT_TYPE, required = true)
    private ObjectType objectType = null;

    @XmlElement(name = FIELD_ADDED)
    private Date added = null;

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

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    @NotNull
    public void setObjectType(ObjectType objectType) {
        ensureNotNull(FIELD_OBJECT_TYPE, objectType);
        ensureVerifiable(FIELD_OBJECT_TYPE, objectType);
        this.objectType = objectType;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public void setAdded(Date added) {
        this.added = added;
    }

    public Date getAdded() {
        return added;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = new HashMap<>();

        checkPattern(errors, FIELD_ID, id, Constants.ID_PATTERN_REGEX, "The Circle Id is invalid.");
        checkNotNull(errors, FIELD_OBJECT_TYPE, objectType, "The ObjectType is undefined.");
        if (objectType != null) {
            errors.putAll(objectType.validate());
        }

        return errors;
    }
}
