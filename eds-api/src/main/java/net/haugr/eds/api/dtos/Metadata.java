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
package net.haugr.eds.api.dtos;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.common.Constants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * <p>The Metadata is the information needed as part of storing or reading
 * Data. This includes the Id of the data, the CircleId for the Circle it
 * belongs to. If the data is stored in a Structure, the FolderId. Another
 * important part of the Data is the name of the Data object, and the
 * DataType of it, and finally the date when it was added.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_DATA_ID,
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_FOLDER_ID,
        Constants.FIELD_DATA_NAME,
        Constants.FIELD_NAME_KEY,
        Constants.FIELD_TYPENAME,
        Constants.FIELD_MEDIA_TYPE,
        Constants.FIELD_SIZE,
        Constants.FIELD_THUMBNAIL_ID,
        Constants.FIELD_ADDED })
@Schema(description = "The metadata of a Data Object (data or folder), without its content.")
public final class Metadata implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** DataId. */
    @Schema(description = "ID of the Data Object.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_DATA_ID)
    @JsonbNillable
    private String dataId = null;

    /** CircleId. */
    @Schema(description = "ID of the Circle.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** FolderId. */
    @Schema(description = "ID of the folder the Data Object is in; null for a Circle's root folder.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_FOLDER_ID)
    @JsonbNillable
    private String folderId = null;

    /** Data Name. */
    @Schema(description = "Name of the Data Object, unique within its folder.", maxLength = Constants.MAX_DATA_NAME_LENGTH, examples = { "report.pdf" })
    @JsonbProperty(value = Constants.FIELD_DATA_NAME)
    @JsonbNillable
    private String dataName = null;

    /** Name Key. */
    @Schema(description = "Name Key of the Data Object, the keyed hash of its name a client of a chamber sent; null if none was sent.",
            maxLength = Constants.MAX_NAME_KEY_LENGTH, pattern = Constants.NAME_KEY_PATTERN_REGEX, examples = { "Xo3f0Qm8tVd2bJcR1uYk7nWz5aLpE4sHgC9vT6yNqMI" })
    @JsonbProperty(value = Constants.FIELD_NAME_KEY)
    @JsonbNillable
    private String nameKey = null;

    /** Type Name. */
    @Schema(description = "Name of the Data Type, e.g. `data` or `folder`.", maxLength = Constants.MAX_NAME_LENGTH, examples = { "data" })
    @JsonbProperty(value = Constants.FIELD_TYPENAME)
    @JsonbNillable
    private String typeName = null;

    /** Media Type. */
    @Schema(description = "Media type of the content, if set.", maxLength = Constants.MAX_MEDIA_TYPE_LENGTH, examples = { "video/mp4" })
    @JsonbProperty(value = Constants.FIELD_MEDIA_TYPE)
    @JsonbNillable
    private String mediaType = null;

    /** Size of the content. */
    @Schema(description = "Size of the content in bytes (the plaintext, not the stored encrypted form); null for a folder or a Data Object without content.", minimum = "0", examples = { "1048576" })
    @JsonbProperty(value = Constants.FIELD_SIZE)
    @JsonbNillable
    private Long size = null;

    /** Thumbnail of the content. */
    @Schema(description = "ID of the thumbnail of the content, if one was stored (PUT /data/{dataId}/thumbnail); download it as content. "
            + "Replacing the content removes the thumbnail.", pattern = Constants.ID_PATTERN_REGEX, examples = { "9b1d4e7a-2f3c-4a5b-8c6d-7e8f9a0b1c2d" })
    @JsonbProperty(value = Constants.FIELD_THUMBNAIL_ID)
    private String thumbnailId = null;

    /** Created Timestamp. */
    @Schema(description = "When the record was created. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_ADDED)
    @JsonbDateFormat
    @JsonbNillable
    private LocalDateTime added = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public Metadata() {
        // Empty Constructor
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

    public void setMediaType(final String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    public Long getSize() {
        return size;
    }

    public void setThumbnailId(final String thumbnailId) {
        this.thumbnailId = thumbnailId;
    }

    public String getThumbnailId() {
        return thumbnailId;
    }

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
                ", nameKey='" + nameKey + '\'' +
                ", typeName=" + typeName +
                ", mediaType='" + mediaType + '\'' +
                ", size=" + size +
                ", thumbnailId='" + thumbnailId + '\'' +
                ", added=" + added +
                '}';
    }
}
