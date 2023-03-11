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
package net.haugr.eds.api.dtos;

import net.haugr.eds.api.common.Constants;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;

/**
 * <p>The Metadata is the information which is needed as part of storing or
 * reading Data. This includes the Id of the data, the CircleId for the Circle
 * it belongs to. If the data is stored in a Structure, the FolderId. Another
 * important part of the Data is the name of the Data object, and the DataType
 * of it, and finally the date when it was added.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_DATA_ID,
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_FOLDER_ID,
        Constants.FIELD_DATA_NAME,
        Constants.FIELD_TYPENAME,
        Constants.FIELD_ADDED })
public final class Metadata implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @JsonbProperty(value = Constants.FIELD_DATA_ID, nillable = true)
    private String dataId = null;

    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID, nillable = true)
    private String circleId = null;

    @JsonbProperty(value = Constants.FIELD_FOLDER_ID, nillable = true)
    private String folderId = null;

    @JsonbProperty(value = Constants.FIELD_DATA_NAME, nillable = true)
    private String dataName = null;

    @JsonbProperty(value = Constants.FIELD_TYPENAME, nillable = true)
    private String typeName = null;

    @JsonbProperty(value = Constants.FIELD_ADDED, nillable = true)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
    private LocalDateTime added = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setDataId(final String dataId) {
        this.dataId = dataId;
    }

    public String getDataId() {
        return dataId;
    }

    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    public String getCircleId() {
        return circleId;
    }

    public void setFolderId(final String folderId) {
        this.folderId = folderId;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setDataName(final String dataName) {
        this.dataName = dataName;
    }

    public String getDataName() {
        return dataName;
    }

    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setAdded(final LocalDateTime added) {
        this.added = added;
    }

    public LocalDateTime getAdded() {
        return added;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Metadata{" +
                "dataId='" + dataId + '\'' +
                ", circleId='" + circleId + '\'' +
                ", folderId='" + folderId + '\'' +
                ", dataName='" + dataName + '\'' +
                ", typeName=" + typeName +
                ", added=" + added +
                '}';
    }
}
