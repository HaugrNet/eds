/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Constants.FIELD_CIRCLE_ID;
import static io.javadog.cws.api.common.Constants.FIELD_DATA_ID;
import static io.javadog.cws.api.common.Constants.FIELD_PAGE_NUMBER;
import static io.javadog.cws.api.common.Constants.FIELD_PAGE_SIZE;
import static io.javadog.cws.api.common.Constants.MAX_PAGE_SIZE;

import io.javadog.cws.api.common.Constants;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
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
@XmlType(name = "fetchDataRequest", propOrder = { FIELD_CIRCLE_ID, FIELD_DATA_ID, FIELD_PAGE_NUMBER, FIELD_PAGE_SIZE })
public final class FetchDataRequest extends Authentication implements CircleIdRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = FIELD_CIRCLE_ID, nillable = true, required = true)
    private String circleId = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = FIELD_DATA_ID, nillable = true, required = true)
    private String dataId = null;

    @Size(min = 1)
    @XmlElement(name = FIELD_PAGE_NUMBER, nillable = true)
    private int pageNumber = 1;

    @Size(min = 1, max = MAX_PAGE_SIZE)
    @XmlElement(name = FIELD_PAGE_SIZE, nillable = true)
    private int pageSize = MAX_PAGE_SIZE;

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCircleId() {
        return circleId;
    }

    public void setDataId(final String dataId) {
        this.dataId = dataId;
    }

    public String getDataId() {
        return dataId;
    }

    public void setPageNumber(final int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageSize() {
        return pageSize;
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
            errors.put(FIELD_CIRCLE_ID, "Either a Circle or Data Id must be provided.");
        }
        checkValidId(errors, FIELD_CIRCLE_ID, circleId, "The Circle Id is invalid.");
        checkValidId(errors, FIELD_DATA_ID, dataId, "The Data Id is invalid.");
        checkIntegerWithMax(errors, FIELD_PAGE_NUMBER, pageNumber, Integer.MAX_VALUE, "The Page Number must be a positive number, starting with 1.");
        checkIntegerWithMax(errors, FIELD_PAGE_SIZE, pageSize, MAX_PAGE_SIZE, "The Page Size must be a positive number, starting with 1.");

        return errors;
    }
}
