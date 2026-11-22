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

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.Share;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.ByteArrayAdapter;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.Utilities;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import java.io.Serial;
import java.util.Map;

/**
 * <p>The Request Object supports several actions for adding, updating, and
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
 * where it is added. As created Objects do not need to have any data, the data
 * is optional.</p>
 *
 * <p>Action <b>UPDATE</b>; requires the Data Id, and an optional Folder Id, if
 * the Object is supposed to be moved within the internal folder structure, or a
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
        Constants.FIELD_NAME_KEY,
        Constants.FIELD_FOLDER_ID,
        Constants.FIELD_TARGET_FOLDER_ID,
        Constants.FIELD_TYPENAME,
        Constants.FIELD_MEDIA_TYPE,
        Constants.FIELD_DATA })
@Schema(description = "Request to add, update, copy, move or delete a Data Object. Which fields are used depends on the endpoint.")
public final class ProcessDataRequest extends Authentication implements CircleIdRequest, ActionRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Action. */
    @Schema(description = "Set by the endpoint; a value sent in the request is ignored.")
    @JsonbProperty(value = Constants.FIELD_ACTION)
    @JsonbNillable
    private Action action = null;

    /** The DataId. */
    @Schema(description = "ID of the Data Object to update, copy, move or delete.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_DATA_ID)
    @JsonbNillable
    private String dataId = null;

    /** The CircleId. */
    @Schema(description = "ID of the Circle to add the Data Object to.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** The Target CircleId. */
    @Schema(description = "ID of the Circle to copy or move the Data Object to.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_TARGET_CIRCLE_ID)
    @JsonbNillable
    private String targetCircleId = null;

    /** The DataName. */
    @Schema(description = "Name of the Data Object, unique within its folder.", maxLength = Constants.MAX_DATA_NAME_LENGTH, examples = { "report.pdf" })
    @JsonbProperty(value = Constants.FIELD_DATA_NAME)
    @JsonbNillable
    private String dataName = null;

    /** The NameKey. */
    @Schema(description = "Name Key of the Data Object: a keyed hash of its name, which a client of a chamber sends next to the "
            + "encrypted name, so the stone can keep the name unique within its folder without reading it. Opaque to the stone, "
            + "Base64url without padding. Kept with the Data Object; it is replaced when a new dataName is given, and dropped "
            + "if that comes without a Name Key. For a copy or a move, it is the Name Key in the target Circle.",
            maxLength = Constants.MAX_NAME_KEY_LENGTH, pattern = Constants.NAME_KEY_PATTERN_REGEX, examples = { "Xo3f0Qm8tVd2bJcR1uYk7nWz5aLpE4sHgC9vT6yNqMI" })
    @JsonbProperty(value = Constants.FIELD_NAME_KEY)
    @JsonbNillable
    private String nameKey = null;

    /** The FolderId. */
    @Schema(description = "ID of the folder to add the Data Object to; omit for the Circle's root folder.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_FOLDER_ID)
    @JsonbNillable
    private String folderId = null;

    /** The Target FolderId. */
    @Schema(description = "ID of the folder to copy or move the Data Object to; omit for the target Circle's root folder.",
            pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_TARGET_FOLDER_ID)
    @JsonbNillable
    private String targetFolderId = null;

    /** The TypeName. */
    @Schema(description = "Name of the Data Type; omit for `data`.", maxLength = Constants.MAX_NAME_LENGTH, examples = { "data" })
    @JsonbProperty(value = Constants.FIELD_TYPENAME)
    @JsonbNillable
    private String typeName = null;

    /** Media Type. */
    @Schema(description = "Media type of the content, e.g. `video/mp4`, used when serving it. Without it, the content is "
            + "served as `application/octet-stream`. Only a few types are shown inline, see the content download.",
            maxLength = Constants.MAX_MEDIA_TYPE_LENGTH, pattern = Constants.MEDIA_TYPE_PATTERN_REGEX, examples = { "video/mp4" })
    @JsonbProperty(value = Constants.FIELD_MEDIA_TYPE)
    @JsonbNillable
    private String mediaType = null;

    /** The Data. */
    @Schema(description = "The content, Base64-encoded. Optional: a Data Object may have no content.", type = SchemaType.STRING, format = "byte")
    @JsonbProperty(value = Constants.FIELD_DATA)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    @JsonbNillable
    private byte[] data = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public ProcessDataRequest() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
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
     * Set the NameKey.
     *
     * @param nameKey NameKey
     */
    public void setNameKey(final String nameKey) {
        this.nameKey = nameKey;
    }

    /**
     * Retrieves the NameKey.
     *
     * @return NameKey
     */
    public String getNameKey() {
        return nameKey;
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

    public void setMediaType(final String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return mediaType;
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
        if ((mediaType != null) && !java.util.regex.Pattern.matches(Constants.MEDIA_TYPE_PATTERN_REGEX, mediaType)) {
            errors.put(Constants.FIELD_MEDIA_TYPE, "The Media Type is invalid.");
        }
        checkValidNameKey(errors, nameKey);

        if (action == null) {
            errors.put(Constants.FIELD_ACTION, "No action has been provided.");
        } else {
            switch (action) {
                case ADD:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "The Circle Id is missing or invalid.");
                    checkValidId(errors, Constants.FIELD_FOLDER_ID, folderId, "The Folder Id is invalid.");
                    checkNotNullEmptyOrTooLong(errors, Constants.FIELD_DATA_NAME, dataName, Constants.MAX_DATA_NAME_LENGTH, "The name of the new Data Object is invalid.");
                    break;
                case UPDATE:
                    checkNotNullAndValidId(errors, Constants.FIELD_DATA_ID, dataId, "The Data Id to update is missing or invalid.");
                    checkValidId(errors, Constants.FIELD_FOLDER_ID, folderId, "The Folder Id is invalid.");
                    checkNotTooLong(errors, Constants.FIELD_DATA_NAME, dataName, Constants.MAX_DATA_NAME_LENGTH, "The new name of the Data Object is invalid.");
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
