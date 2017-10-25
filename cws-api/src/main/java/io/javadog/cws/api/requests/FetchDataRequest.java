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
@XmlType(name = "fetchDataRequest", propOrder = { "circleId", "dataId" })
public final class FetchDataRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_CIRCLE_ID = "circleId";
    private static final String FIELD_DATA_ID = "dataId";
    private static final String FIELD_PAGE_NUMBER = "pageNumber";
    private static final String FIELD_PAGE_SIZE = "pageSize";
    private static final int MAX_PAGE_SIZE = 100;

    @XmlElement(name = FIELD_CIRCLE_ID, nillable = true, required = true)
    private String circleId = null;

    @XmlElement(name = FIELD_DATA_ID, nillable = true, required = true)
    private String dataId = null;

    @XmlElement(name = FIELD_PAGE_NUMBER, nillable = true)
    private int pageNumber = 1;

    @XmlElement(name = FIELD_PAGE_SIZE, nillable = true)
    private int pageSize = MAX_PAGE_SIZE;

    // =========================================================================
    // Setters & Getters
    // =========================================================================

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

    public void setPageNumber(final int pageNumber) {
        ensurePositiveNumber(FIELD_PAGE_NUMBER, pageNumber);
        this.pageNumber = pageNumber;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageSize(final int pageSize) {
        ensureValidRange(FIELD_PAGE_SIZE, pageSize, 1, MAX_PAGE_SIZE);
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
            errors.put(FIELD_CIRCLE_ID, "Either the CircleId or an Object Data Id must be provided.");
        } else {
            if (circleId != null) {
                checkPattern(errors, FIELD_CIRCLE_ID, circleId, Constants.ID_PATTERN_REGEX, "The Circle Id is invalid.");
            } else {
                checkPattern(errors, FIELD_DATA_ID, dataId, Constants.ID_PATTERN_REGEX, "The Data Id is invalid.");
            }
            if (pageNumber <= 0) {
                errors.put(FIELD_PAGE_NUMBER, "Cannot fetch a negative page number");
            }
            if ((pageSize <= 0) || (pageSize > MAX_PAGE_SIZE)) {
                errors.put(FIELD_PAGE_SIZE, "The size of the page must be between 1 and " + MAX_PAGE_SIZE + '.');
            }
        }

        return errors;
    }
}
