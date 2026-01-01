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

import net.haugr.eds.api.common.ByteArrayAdapter;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.dtos.Metadata;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>If the request was made generally, i.e., without a Data Id, then a list of
 * Metadata Objects is being returned. The list is limited to the number which
 * was requested - and the records field will then contain the total number of
 * entries which were found, so it is possible to perform the lookup with
 * pagination information.</p>
 *
 * <p>If the request was made for a specific Data Id, and the Object exists in
 * the database, then the list of Metadata will return a single entry, and the
 * data field will be set with the unencrypted data.</p>
 *
 * <p>Please see {@link EDSResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_METADATA,
        Constants.FIELD_RECORDS,
        Constants.FIELD_DATA })
public final class FetchDataResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Metadata. */
    @JsonbProperty(Constants.FIELD_METADATA)
    private final List<Metadata> metadata = new ArrayList<>(0);

    /** The Number of Records. */
    @JsonbProperty(Constants.FIELD_RECORDS)
    private long records = 0;

    /** The Data. */
    @JsonbProperty(Constants.FIELD_DATA)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    private byte[] data = null;

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public FetchDataResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public FetchDataResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    /**
     * Set the Metadata.
     *
     * @param metadata Metadata
     */
    public void setMetadata(final List<Metadata> metadata) {
        this.metadata.addAll(metadata);
    }

    /**
     * Retrieves the Metadata.
     *
     * @return Metadata
     */
    public List<Metadata> getMetadata() {
        return Collections.unmodifiableList(metadata);
    }

    /**
     * Set the Number of Records.
     *
     * @param records Number of Records
     */
    public void setRecords(final long records) {
        this.records = records;
    }

    /**
     * Retrieves the Number of Records.
     *
     * @return Number of Records
     */
    public long getRecords() {
        return records;
    }

    /**
     * Set the Data.
     *
     * @param data Data
     */
    public void setData(final byte[] data) {
        this.data = Utilities.copy(data);
    }

    /**
     * Retrieves the Data.
     *
     * @return Data
     */
    public byte[] getData() {
        return Utilities.copy(data);
    }
}
