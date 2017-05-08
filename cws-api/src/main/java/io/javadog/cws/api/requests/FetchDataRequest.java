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
@XmlType(name = "fetchObjectRequest", propOrder = { "dataType", "dataId" })
public final class FetchDataRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_DATA_TYPE = "dataType";
    private static final String FIELD_DATA_ID = "dataId";

    @XmlElement(name = FIELD_DATA_TYPE, required = true, nillable = true)
    private DataType dataType = null;

    @XmlElement(name = FIELD_DATA_ID, required = true, nillable = true)
    private String dataId = null;

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    public void setDataType(DataType dataType) {
        ensureVerifiable(FIELD_DATA_TYPE, dataType);
        this.dataType = dataType;
    }

    public DataType getDataType() {
        return dataType;
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

        if (dataType != null) {
            errors.putAll(dataType.validate());
        }
        if (dataId != null) {
            checkPattern(errors, FIELD_DATA_ID, dataId, Constants.ID_PATTERN_REGEX, "The Object Data Id is invalid.");
        }

        return errors;
    }
}
