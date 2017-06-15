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
@XmlType(name = "data", namespace = "api.cws.javadog.io", propOrder = { "id", "circleId", "folderId", "name", "typeName", "bytes", "added" })
public final class Data extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final int MAX_LENGTH = 256;
    private static final String FIELD_ID = "id";
    private static final String FIELD_CIRCLE_ID = "circleId";
    private static final String FIELD_FOLDER_ID = "folderId";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPENAME = "typeName";
    private static final String FIELD_BYTES = "bytes";
    private static final String FIELD_ADDED = "added";

    @XmlElement(name = FIELD_ID, required = true, nillable = true)
    private String id = null;

    @XmlElement(name = FIELD_CIRCLE_ID, required = true, nillable = true)
    private String circleId = null;

    @XmlElement(name = FIELD_FOLDER_ID, required = true, nillable = true)
    private String folderId = null;

    @XmlElement(name = FIELD_NAME, required = true, nillable = true)
    private String name = null;

    @XmlElement(name = FIELD_TYPENAME, required = true)
    private String typeName = null;

    @XmlElement(name = FIELD_BYTES, required = true, nillable = true)
    private byte[] bytes = null;

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

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    public void setCircleId(final String circleId) {
        ensurePattern(FIELD_CIRCLE_ID, circleId, Constants.ID_PATTERN_REGEX);
        this.circleId = circleId;
    }

    public String getCircleId() {
        return circleId;
    }

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    public void setFolderId(final String folderId) {
        ensurePattern(FIELD_FOLDER_ID, folderId, Constants.ID_PATTERN_REGEX);
        this.folderId = folderId;
    }

    public String getFolderId() {
        return folderId;
    }

    @Size(max = MAX_LENGTH)
    public void setName(final String name) {
        ensureNotNullOrTooLong(FIELD_NAME, name, MAX_LENGTH);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Size(min = 1, max = MAX_LENGTH)
    public void setTypeName(final String typeName) {
        ensureNotNullOrTooLong(FIELD_TYPENAME, typeName, MAX_LENGTH);
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setBytes(final byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setAdded(final Date added) {
        this.added = new Date(added.getTime());
    }

    public Date getAdded() {
        return new Date(added.getTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = new HashMap<>();

        checkPattern(errors, FIELD_ID, id, Constants.ID_PATTERN_REGEX, "The Data Id is invalid.");

        if (id == null) {
            checkNotNull(errors, FIELD_CIRCLE_ID, circleId, "The Circle Id is required for new Data Objects.");
            checkPattern(errors, FIELD_CIRCLE_ID, circleId, Constants.ID_PATTERN_REGEX, "The Circle Id is invalid.");
            checkNotNull(errors, FIELD_TYPENAME, typeName, "The DataType Name is required for new Data Objects.");
            checkNotTooLong(errors, FIELD_TYPENAME, typeName, MAX_LENGTH, "The name of the DataType may not exceed " + MAX_LENGTH + " characters.");
        }

        checkNotTooLong(errors, FIELD_NAME, name, MAX_LENGTH, "The name of the Data Object may not exceed " + MAX_LENGTH + " characters.");
        checkPattern(errors, FIELD_FOLDER_ID, folderId, Constants.ID_PATTERN_REGEX, "The Folder Id is invalid.");

        return errors;
    }
}
