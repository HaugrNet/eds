/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static io.javadog.cws.api.common.Utilities.copy;

import io.javadog.cws.api.common.Constants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * <p>The Metadata is the information which is needed as part of storing or
 * reading Data. This includes the Id of the data, the CircleId for the Circle
 * it belongs to. If the data is stored in a Structure, the FolderId. Another
 * important part of the Data is the name of the Data object, and the DataType
 * of it, and finally the date when it was added.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = Constants.FIELD_METADATA, propOrder = { Constants.FIELD_DATA_ID, Constants.FIELD_CIRCLE_ID, Constants.FIELD_FOLDER_ID, Constants.FIELD_DATA_NAME, Constants.FIELD_TYPENAME, Constants.FIELD_ADDED })
public final class Metadata implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_DATA_ID, required = true)
    private String dataId = null;

    @XmlElement(name = Constants.FIELD_CIRCLE_ID, required = true)
    private String circleId = null;

    @XmlElement(name = Constants.FIELD_FOLDER_ID, required = true)
    private String folderId = null;

    @XmlElement(name = Constants.FIELD_DATA_NAME, required = true)
    private String dataName = null;

    @XmlElement(name = Constants.FIELD_TYPENAME, required = true)
    private String typeName = null;

    @XmlElement(name = Constants.FIELD_ADDED, required = true)
    private Date added = null;

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

    public void setAdded(final Date added) {
        this.added = copy(added);
    }

    public Date getAdded() {
        return copy(added);
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Metadata)) {
            return false;
        }

        final Metadata that = (Metadata) obj;
        return Objects.equals(dataId, that.dataId) &&
                Objects.equals(circleId, that.circleId) &&
                Objects.equals(folderId, that.folderId) &&
                Objects.equals(dataName, that.dataName) &&
                Objects.equals(typeName, that.typeName) &&
                Objects.equals(added, that.added);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(dataId, circleId, folderId, dataName, typeName, added);
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
                ", typeName=" + typeName +
                ", added=" + added +
                '}';
    }
}
