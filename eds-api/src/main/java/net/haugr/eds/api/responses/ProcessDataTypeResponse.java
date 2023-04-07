/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
package net.haugr.eds.api.responses;

import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.DataType;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;

/**
 * <p>When a DataType has been processed, it is not having a an Id, thus the
 * entire DataType Object is simple returned, if the request completed
 * successfully.</p>
 *
 * <p>Please see {@link EDSResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder(Constants.FIELD_DATATYPE)
public final class ProcessDataTypeResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The DataType. */
    @JsonbProperty(Constants.FIELD_DATATYPE)
    private DataType dataType = null;

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public ProcessDataTypeResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Constructor for more detailed responses.
     *
     * @param returnMessage The EDS Return Message
     */
    public ProcessDataTypeResponse(final String returnMessage) {
        super(returnMessage);
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public ProcessDataTypeResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * Set the DataType.
     *
     * @param dataType DataType
     */
    public void setDataType(final DataType dataType) {
        this.dataType = dataType;
    }

    /**
     * Retrieves the DataType.
     *
     * @return DataType
     */
    public DataType getDataType() {
        return dataType;
    }
}
