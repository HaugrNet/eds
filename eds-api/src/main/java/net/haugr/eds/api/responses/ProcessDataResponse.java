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
package net.haugr.eds.api.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;

/**
 * <p>The Response Object contains the new Data Id, when an Object was
 * successfully uploaded.</p>
 *
 * <p>Please see {@link EDSResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_DATA_ID, Constants.FIELD_ETAG })
@Schema(description = "Result of processing a Data Object.")
public final class ProcessDataResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The DataId. */
    @Schema(description = "ID of the Data Object; for an added or copied Data Object, its new ID.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(Constants.FIELD_DATA_ID)
    private String dataId = null;

    /** The ETag of the content. */
    @Schema(description = "ETag of the stored content, after a content upload. It changes with every write of the content.", examples = { "\"q3V0b2dlbmVyYXRlZA\"" })
    @JsonbProperty(Constants.FIELD_ETAG)
    private String etag = null;

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public ProcessDataResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Constructor for more detailed responses.
     *
     * @param returnMessage The EDS Return Message
     */
    public ProcessDataResponse(final String returnMessage) {
        super(returnMessage);
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public ProcessDataResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

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
     * Set the ETag of the stored content.
     *
     * @param etag ETag
     */
    public void setEtag(final String etag) {
        this.etag = etag;
    }

    /**
     * Retrieves the ETag of the stored content.
     *
     * @return ETag
     */
    public String getEtag() {
        return etag;
    }
}
