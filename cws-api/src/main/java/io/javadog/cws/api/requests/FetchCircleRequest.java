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

import javax.validation.constraints.Pattern;
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
@XmlType(name = "fetchCircleRequest", propOrder = "circleId")
public final class FetchCircleRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_CIRCLE_ID = "circleId";

    @XmlElement(name = FIELD_CIRCLE_ID, nillable = true, required = true)
    private String circleId = null;

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    public void setCircleId(final String circleId) {
        ensureValidId(FIELD_CIRCLE_ID, circleId);
        this.circleId = circleId;
    }

    public String getCircleId() {
        return circleId;
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

        if (circleId != null) {
            checkPattern(errors, FIELD_CIRCLE_ID, circleId, Constants.ID_PATTERN_REGEX, "The Circle Id is invalid.");
        }

        return errors;
    }
}
