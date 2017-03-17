/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fetchObjectRequest", propOrder = "objectId")
public final class FetchObjectRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_OBJECT_ID = "objectId";

    @XmlElement(name = FIELD_OBJECT_ID, required = true, nillable = true)
    private String objectId = null;

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    public void setObjectId(final String objectId) {
        ensureValidId(FIELD_OBJECT_ID, objectId);
        this.objectId = objectId;
    }

    public String getObjectId() {
        return objectId;
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

        if (objectId != null) {
            checkPattern(errors, FIELD_OBJECT_ID, objectId, Constants.ID_PATTERN_REGEX, "The Object Id is invalid.");
        }

        return errors;
    }
}
