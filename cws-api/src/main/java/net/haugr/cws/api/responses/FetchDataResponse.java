/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.api.responses;

import net.haugr.cws.api.common.ByteArrayAdapter;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.common.Utilities;
import net.haugr.cws.api.dtos.Metadata;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTypeAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>If the request was made generally, i.e. without a Data Id, then a list of
 * Metadata Objects is being returned. The list is limited to the number which
 * was requested - and the records field will then contain the total amount of
 * entries which was found, so it is possible to perform the lookup with
 * pagination information.</p>
 *
 * <p>If the request was made for a specific Data Id, and the Object exists in
 * the database, then the list of Metadata will return a single entry, and the
 * data field will be set with the unencrypted data.</p>
 *
 * <p>Please see {@link CwsResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_METADATA,
        Constants.FIELD_RECORDS,
        Constants.FIELD_DATA })
public final class FetchDataResponse extends CwsResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @JsonbProperty(Constants.FIELD_METADATA)
    private final List<Metadata> metadata = new ArrayList<>(0);

    @JsonbProperty(Constants.FIELD_RECORDS)
    private long records = 0;

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
     * @param returnCode    The CWS Return Code
     * @param returnMessage The CWS Return Message
     */
    public FetchDataResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    public void setMetadata(final List<Metadata> metadata) {
        this.metadata.addAll(metadata);
    }

    public List<Metadata> getMetadata() {
        return Collections.unmodifiableList(metadata);
    }

    public void setRecords(final long records) {
        this.records = records;
    }

    public long getRecords() {
        return records;
    }

    public void setData(final byte[] data) {
        this.data = Utilities.copy(data);
    }

    public byte[] getData() {
        return Utilities.copy(data);
    }
}
