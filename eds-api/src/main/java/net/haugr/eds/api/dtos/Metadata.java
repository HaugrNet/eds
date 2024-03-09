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
package net.haugr.eds.api.dtos;

import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.common.Constants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

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
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** DataId. */
    @JsonbProperty(value = Constants.FIELD_DATA_ID)
    @JsonbNillable
    private String dataId = null;

    /** CircleId. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** FolderId. */
    @JsonbProperty(value = Constants.FIELD_FOLDER_ID)
    @JsonbNillable
    private String folderId = null;

    /** Data Name. */
    @JsonbProperty(value = Constants.FIELD_DATA_NAME)
    @JsonbNillable
    private String dataName = null;

    /** Type Name. */
    @JsonbProperty(value = Constants.FIELD_TYPENAME)
    @JsonbNillable
    private String typeName = null;

    /** Created Timestamp. */
    @JsonbProperty(value = Constants.FIELD_ADDED)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
    @JsonbNillable
    private LocalDateTime added = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public Metadata() {
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
     * Set the CircleId.
     *
     * @param circleId CircleId
     */
    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    /**
     * Retrieves the CircleId.
     *
     * @return CircleId
     */
    public String getCircleId() {
        return circleId;
    }

    /**
     * Set the FolderId.
     *
     * @param folderId FolderId
     */
    public void setFolderId(final String folderId) {
        this.folderId = folderId;
    }

    /**
     * Retrieves the FolderId.
     *
     * @return FolderId
     */
    public String getFolderId() {
        return folderId;
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

    /**
     * Set the TypeName.
     *
     * @param typeName TypeName
     */
    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    /**
     * Retrieves the TypeName.
     *
     * @return TypeName
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Set the Created Timestamp.
     *
     * @param added Created Timestamp
     */
    public void setAdded(final LocalDateTime added) {
        this.added = added;
    }

    /**
     * Retrieves the Created Timestamp.
     *
     * @return Created Timestamp
     */
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
