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
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.Utilities;
import io.javadog.cws.api.dtos.Metadata;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
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
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "fetchDataResult")
@XmlType(name = "fetchDataResult", propOrder = { Constants.FIELD_METADATA, Constants.FIELD_RECORDS, Constants.FIELD_DATA })
public final class FetchDataResponse extends CwsResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_METADATA, required = true)
    private final List<Metadata> metadata = new ArrayList<>(0);

    @XmlElement(name = Constants.FIELD_RECORDS, required = true)
    private long records = 0;

    @XmlElement(name = Constants.FIELD_DATA, required = true)
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
