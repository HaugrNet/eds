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
import io.javadog.cws.api.dtos.DataType;

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
@XmlType(name = "fetchDataRequest", namespace = "api.cws.javadog.io", propOrder = { "dataType", "circleId", "dataId" })
public final class FetchDataRequest extends Authentication {

    /**
     * {@link Constants#SERIAL_VERSION_UID}.
     */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_DATA_TYPE = "dataType";
    private static final String FIELD_CIRCLE_ID = "circleId";
    private static final String FIELD_DATA_ID = "dataId";

    @XmlElement(name = FIELD_DATA_TYPE, required = true, nillable = true)
    private DataType dataType = null;

    @XmlElement(name = FIELD_CIRCLE_ID, required = true, nillable = true)
    private String circleId = null;

    @XmlElement(name = FIELD_DATA_ID, required = true, nillable = true)
    private String dataId = null;

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    public void setDataType(final DataType dataType) {
        ensureVerifiable(FIELD_DATA_TYPE, dataType);
        this.dataType = dataType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setCircleId(final String circleId) {
        ensureValidId(FIELD_CIRCLE_ID, circleId);
        this.circleId = circleId;
    }

    public String getCircleId() {
        return circleId;
    }

    public void setDataId(final String dataId) {
        ensureValidId(FIELD_DATA_ID, dataId);
        this.dataId = dataId;
    }

    public String getDataId() {
        return dataId;
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

        if ((circleId == null) && (dataId == null)) {
            errors.put(FIELD_CIRCLE_ID, "Either the CircleId or an Object Data Id must be provided.");
        } else {
            if (circleId != null) {
                checkPattern(errors, FIELD_CIRCLE_ID, circleId, Constants.ID_PATTERN_REGEX, "The Circle Id is invalid.");
                if (dataType != null) {
                    errors.putAll(dataType.validate());
                }
            } else {
                checkPattern(errors, FIELD_DATA_ID, dataId, Constants.ID_PATTERN_REGEX, "The Object Data Id is invalid.");
            }
        }

        return errors;
    }
}
