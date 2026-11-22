/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.Share;
import net.haugr.eds.api.common.Constants;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.Map;

/**
 * <p>The Request Object must be filled with either a DataId or a CircleId and
 * pagination information. The pagination includes the page size which must be
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
        Constants.FIELD_DATA_NAME,
        Constants.FIELD_INCLUDE_HIDDEN })
@Schema(description = "Request for Data Objects: one Data Object (by dataId or dataName) including its content, or a page of the metadata in a Circle (by circleId).")
public final class FetchDataRequest extends Authentication implements CircleIdRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** CircleId. */
    @Schema(description = "ID of the Circle to list, when fetching a page of metadata.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** DataId. */
    @Schema(description = "ID of the Data Object to fetch, including its content.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_DATA_ID)
    @JsonbNillable
    private String dataId = null;

    /** Page Number. */
    @Schema(description = "Page to return, starting at 1. A page beyond the last record returns an empty list.", minimum = "1", defaultValue = "1", examples = { "1" })
    @JsonbProperty(value = Constants.FIELD_PAGE_NUMBER)
    @JsonbNillable
    private Integer pageNumber = 1;

    /** Page Size. */
    @Schema(description = "Number of records per page, from 1 to 100.", minimum = "1", maximum = "100", defaultValue = "100", examples = { "20" })
    @JsonbProperty(value = Constants.FIELD_PAGE_SIZE)
    @JsonbNillable
    private Integer pageSize = Constants.MAX_PAGE_SIZE;

    /** DataName. */
    @Schema(description = "Name of the Data Object to fetch, if no dataId is given.", maxLength = Constants.MAX_DATA_NAME_LENGTH, examples = { "report.pdf" })
    @JsonbProperty(value = Constants.FIELD_DATA_NAME)
    @JsonbNillable
    private String dataName = null;

    @Schema(description = "Also list the hidden entries of a folder, whose names start with a dot, like the folder of a feature with its entries; default false.",
            defaultValue = "false", examples = { "false" })
    @JsonbProperty(value = Constants.FIELD_INCLUDE_HIDDEN)
    @JsonbNillable
    private Boolean includeHidden = false;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public FetchDataRequest() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
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

    public void setIncludeHidden(final Boolean includeHidden) {
        this.includeHidden = includeHidden;
    }

    public Boolean getIncludeHidden() {
        return includeHidden;
    }

    /**
     * Whether a folder listing includes the hidden entries.
     *
     * @return True if the hidden entries are listed
     */
    public boolean includesHidden() {
        return Boolean.TRUE.equals(includeHidden);
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
