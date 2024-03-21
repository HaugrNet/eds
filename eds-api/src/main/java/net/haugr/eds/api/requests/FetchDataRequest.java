/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.api.requests;

import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.Share;
import net.haugr.eds.api.common.Constants;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.Map;

/**
 * <p>The Request Object must be filled with either a DataId or a CircleId and
 * pagination information. The pagination include the page size which must be
 * at least 1 and maximum 100. The page number starts with 1 for the first page,
 * and any positive number. If the number exceeds the number of records, it will
 * simply result in an empty list of Objects being returned.</p>
 *
 * <p>For more details, please see the 'fetchData' request in the Share
 * interface: {@link Share#fetchData(FetchDataRequest)}</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_DATA_ID,
        Constants.FIELD_PAGE_NUMBER,
        Constants.FIELD_PAGE_SIZE,
        Constants.FIELD_DATA_NAME })
public final class FetchDataRequest extends Authentication implements CircleIdRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** CircleId. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** DataId. */
    @JsonbProperty(value = Constants.FIELD_DATA_ID)
    @JsonbNillable
    private String dataId = null;

    /** Page Number. */
    @JsonbProperty(value = Constants.FIELD_PAGE_NUMBER)
    @JsonbNillable
    private Integer pageNumber = 1;

    /** Page Size. */
    @JsonbProperty(value = Constants.FIELD_PAGE_SIZE)
    @JsonbNillable
    private Integer pageSize = Constants.MAX_PAGE_SIZE;

    /** DataName. */
    @JsonbProperty(value = Constants.FIELD_DATA_NAME)
    @JsonbNillable
    private String dataName = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public FetchDataRequest() {
        // Empty Constructor
    }

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

    /**
     * Set the DataId.
     *
     * @param dataId DataId
     */
    public void setDataId(final String dataId) {
        this.dataId = dataId;
    }

    /**
     * Retrieves the DataId.
     *
     * @return DataId
     */
    public String getDataId() {
        return dataId;
    }

    /**
     * Set the Page Number.
     *
     * @param pageNumber Page Number
     */
    public void setPageNumber(final int pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * Retrieves the Page Number.
     *
     * @return Page Number
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Set the Page Size.
     *
     * @param pageSize Page Size
     */
    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Retrieves the Page Size.
     *
     * @return Page Size
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Set the DataName.
     *
     * @param dataName DataName
     */
    public void setDataName(final String dataName) {
        this.dataName = dataName;
    }

    /**
     * Retrieves the DataName.
     *
     * @return DataName
     */
    public String getDataName() {
        return dataName;
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

        if ((circleId == null) && (dataId == null) && (dataName == null)) {
            errors.put(Constants.FIELD_IDS, "Either a Circle Id, Data Id, or Data Name must be provided.");
        }

        checkValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "The Circle Id is invalid.");
        checkValidId(errors, Constants.FIELD_DATA_ID, dataId, "The Data Id is invalid.");
        checkIntegerWithMax(errors, Constants.FIELD_PAGE_NUMBER, pageNumber, Integer.MAX_VALUE, "The Page Number must be a positive number, starting with 1.");
        checkIntegerWithMax(errors, Constants.FIELD_PAGE_SIZE, pageSize, Constants.MAX_PAGE_SIZE, "The Page Size must be a positive number, starting with 1.");

        return errors;
    }
}
