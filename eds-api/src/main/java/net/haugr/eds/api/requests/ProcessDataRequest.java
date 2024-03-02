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

import net.haugr.eds.api.Share;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.ByteArrayAdapter;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.Utilities;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTypeAdapter;
import java.io.Serial;
import java.util.Map;

/**
 * <p>The Request Object supports several actions for adding, updating and
 * deleting Data Objects in EDS. The request supports the following Actions:</p>
 *
 * <ul>
 *   <li><b>ADD</b> - For adding a new Data Object</li>
 *   <li><b>UPDATE</b> - For updating an existing Data Object</li>
 *   <li><b>DELETE</b> - For deleting an existing Data Object</li>
 * </ul>
 *
 * <p>Action <b>ADD</b>; requires a Circle Id, and optionally a Folder Id (Data
 * Id, where the dataType is a folder), and the name of the Object. The name
 * must be between 1 and 75 characters, and it must be unique for the folder
 * where it is added. As Objects created doesn't need to have any data, the data
 * is optional.</p>
 *
 * <p>Action <b>UPDATE</b>; requires the Data Id, and an optional Folder Id, if
 * the Object is suppose to be moved within the internal folder structure, or a
 * new Data name, which must be unique for the folder where it should be placed,
 * and the length must be between 1 and 75 characters.</p>
 *
 * <p>Action <b>DELETE</b>; requires the Data Id.</p>
 *
 * <p>For more details, please see the 'processData' request in the Share
 * interface: {@link Share#processData(ProcessDataRequest)}</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_ACTION,
        Constants.FIELD_DATA_ID,
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_TARGET_CIRCLE_ID,
        Constants.FIELD_DATA_NAME,
        Constants.FIELD_FOLDER_ID,
        Constants.FIELD_TARGET_FOLDER_ID,
        Constants.FIELD_TYPENAME,
        Constants.FIELD_DATA })
public final class ProcessDataRequest extends Authentication implements CircleIdRequest, ActionRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Action. */
    @JsonbProperty(value = Constants.FIELD_ACTION, nillable = true)
    private Action action = null;

    /** The DataId. */
    @JsonbProperty(value = Constants.FIELD_DATA_ID, nillable = true)
    private String dataId = null;

    /** The CircleId. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID, nillable = true)
    private String circleId = null;

    /** The Target CircleId. */
    @JsonbProperty(value = Constants.FIELD_TARGET_CIRCLE_ID, nillable = true)
    private String targetCircleId = null;

    /** The DataName. */
    @JsonbProperty(value = Constants.FIELD_DATA_NAME, nillable = true)
    private String dataName = null;

    /** The FolderId. */
    @JsonbProperty(value = Constants.FIELD_FOLDER_ID, nillable = true)
    private String folderId = null;

    /** The Target FolderId. */
    @JsonbProperty(value = Constants.FIELD_TARGET_FOLDER_ID, nillable = true)
    private String targetFolderId = null;

    /** The TypeName. */
    @JsonbProperty(value = Constants.FIELD_TYPENAME, nillable = true)
    private String typeName = null;

    /** The Data. */
    @JsonbProperty(value = Constants.FIELD_DATA, nillable = true)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    private byte[] data = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public ProcessDataRequest() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAction(final Action action) {
        this.action = action;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Action getAction() {
        return action;
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
     * Set the Target CircleId.
     *
     * @param targetCircleId Target CircleId
     */
    public void setTargetCircleId(final String targetCircleId) {
        this.targetCircleId = targetCircleId;
    }

    /**
     * Retrieves the Target CircleId.
     *
     * @return Target CircleId
     */
    public String getTargetCircleId() {
        return targetCircleId;
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
     * Set the Target FolderId.
     *
     * @param targetFolderId Target FolderId
     */
    public void setTargetFolderId(final String targetFolderId) {
        this.targetFolderId = targetFolderId;
    }

    /**
     * Retrieves the Target FolderId.
     *
     * @return Target FolderId
     */
    public String getTargetFolderId() {
        return targetFolderId;
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

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = super.validate();

        if (action == null) {
            errors.put(Constants.FIELD_ACTION, "No action has been provided.");
        } else {
            switch (action) {
                case ADD:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "The Circle Id is missing or invalid.");
                    checkValidId(errors, Constants.FIELD_FOLDER_ID, folderId, "The Folder Id is invalid.");
                    checkNotNullEmptyOrTooLong(errors, Constants.FIELD_DATA_NAME, dataName, Constants.MAX_NAME_LENGTH, "The name of the new Data Object is invalid.");
                    break;
                case UPDATE:
                    checkNotNullAndValidId(errors, Constants.FIELD_DATA_ID, dataId, "The Data Id to update is missing or invalid.");
                    checkValidId(errors, Constants.FIELD_FOLDER_ID, folderId, "The Folder Id is invalid.");
                    checkNotTooLong(errors, Constants.FIELD_DATA_NAME, dataName, Constants.MAX_NAME_LENGTH, "The new name of the Data Object is invalid.");
                    break;
                case COPY:
                    checkNotNullAndValidId(errors, Constants.FIELD_DATA_ID, dataId, "The Data Id to copy is missing or invalid.");
                    checkNotNullAndValidId(errors, Constants.FIELD_TARGET_CIRCLE_ID, targetCircleId, "The target Circle Id is missing or invalid.");
                    checkValidId(errors, Constants.FIELD_TARGET_FOLDER_ID, targetFolderId, "The target Folder Id is invalid.");
                    break;
                case MOVE:
                    checkNotNullAndValidId(errors, Constants.FIELD_DATA_ID, dataId, "The Data Id to move is missing or invalid.");
                    checkNotNullAndValidId(errors, Constants.FIELD_TARGET_CIRCLE_ID, targetCircleId, "The target Circle Id is missing or invalid.");
                    checkValidId(errors, Constants.FIELD_TARGET_FOLDER_ID, targetFolderId, "The target Folder Id is invalid.");
                    break;
                case DELETE:
                    checkNotNullAndValidId(errors, Constants.FIELD_DATA_ID, dataId, "The Data Id to delete is missing or invalid.");
                    break;
                default:
                    errors.put(Constants.FIELD_ACTION, "Not supported Action has been provided.");
                    break;
            }
        }

        return errors;
    }
}
