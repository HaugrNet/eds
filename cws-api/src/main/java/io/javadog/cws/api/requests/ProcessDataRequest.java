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
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Utilities;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;

/**
 * <p>The Request Object supports several actions for adding, updating and
 * deleting Data Objects in CWS. The request supports the following Actions:</p>
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
 * interface: {@link io.javadog.cws.api.Share#processData(ProcessDataRequest)}</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "processDataRequest")
@XmlType(name = "processDataRequest", propOrder = {
        Constants.FIELD_ACTION,
        Constants.FIELD_DATA_ID,
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_TARGET_CIRCLE_ID,
        Constants.FIELD_DATA_NAME,
        Constants.FIELD_FOLDER_ID,
        Constants.FIELD_TARGET_FOLDER_ID,
        Constants.FIELD_TYPENAME,
        Constants.FIELD_DATA })
public final class ProcessDataRequest extends Authentication implements CircleIdRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @NotNull
    @XmlElement(name = Constants.FIELD_ACTION, required = true)
    private Action action = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_DATA_ID)
    private String dataId = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_CIRCLE_ID)
    private String circleId = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_TARGET_CIRCLE_ID)
    private String targetCircleId = null;

    @Size(min = 1, max = Constants.MAX_NAME_LENGTH)
    @XmlElement(name = Constants.FIELD_DATA_NAME)
    private String dataName = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_FOLDER_ID)
    private String folderId = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_TARGET_FOLDER_ID)
    private String targetFolderId = null;

    @Size(min = 1, max = Constants.MAX_NAME_LENGTH)
    @XmlElement(name = Constants.FIELD_TYPENAME)
    private String typeName = null;

    @XmlElement(name = Constants.FIELD_DATA)
    private byte[] data = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setAction(final Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public void setDataId(final String dataId) {
        this.dataId = dataId;
    }

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

    public void setTargetCircleId(final String targetCircleId) {
        this.targetCircleId = targetCircleId;
    }

    public String getTargetCircleId() {
        return targetCircleId;
    }

    public void setDataName(final String dataName) {
        this.dataName = dataName;
    }

    public String getDataName() {
        return dataName;
    }

    public void setFolderId(final String folderId) {
        this.folderId = folderId;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setTargetFolderId(final String targetFolderId) {
        this.targetFolderId = targetFolderId;
    }

    public String getTargetFolderId() {
        return targetFolderId;
    }

    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setData(final byte[] data) {
        this.data = Utilities.copy(data);
    }

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
