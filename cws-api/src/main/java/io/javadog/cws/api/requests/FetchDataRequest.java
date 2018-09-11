/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;

/**
 * <p>The Request Object must be filled with either a DataId or a CircleId and
 * pagination information. The pagination include the page size which must be
 * at least 1 and maximum 100. The page number starts with 1 for the first page,
 * and any positive number. If the number exceeds the number of records, it will
 * simply result in an empty list of Objects being returned.</p>
 *
 * <p>For more details, please see the 'fetchData' request in the Share
 * interface: {@link io.javadog.cws.api.Share#fetchData(FetchDataRequest)}</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "fetchDataRequest")
@XmlType(name = "fetchDataRequest", propOrder = { Constants.FIELD_CIRCLE_ID, Constants.FIELD_DATA_ID, Constants.FIELD_PAGE_NUMBER, Constants.FIELD_PAGE_SIZE })
public final class FetchDataRequest extends Authentication implements CircleIdRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_CIRCLE_ID)
    private String circleId = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_DATA_ID)
    private String dataId = null;

    @NotNull
    @Size(min = 1)
    @XmlElement(name = Constants.FIELD_PAGE_NUMBER)
    private Integer pageNumber = 1;

    @NotNull
    @Size(min = 1, max = Constants.MAX_PAGE_SIZE)
    @XmlElement(name = Constants.FIELD_PAGE_SIZE)
    private Integer pageSize = Constants.MAX_PAGE_SIZE;

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

    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageSize() {
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
            errors.put(Constants.FIELD_IDS, "Either a Circle or Data Id must be provided.");
        }

        checkValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "The Circle Id is invalid.");
        checkValidId(errors, Constants.FIELD_DATA_ID, dataId, "The Data Id is invalid.");
        checkIntegerWithMax(errors, Constants.FIELD_PAGE_NUMBER, pageNumber, Integer.MAX_VALUE, "The Page Number must be a positive number, starting with 1.");
        checkIntegerWithMax(errors, Constants.FIELD_PAGE_SIZE, pageSize, Constants.MAX_PAGE_SIZE, "The Page Size must be a positive number, starting with 1.");

        return errors;
    }
}
