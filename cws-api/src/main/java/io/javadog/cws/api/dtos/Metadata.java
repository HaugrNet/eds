/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
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
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "metadata", propOrder = { Constants.FIELD_DATA_ID, Constants.FIELD_CIRCLE_ID, Constants.FIELD_FOLDER_ID, Constants.FIELD_DATA_NAME, Constants.FIELD_DATATYPE, Constants.FIELD_ADDED })
public final class Metadata implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_DATA_ID, nillable = true, required = true)
    private String dataId = null;

    @XmlElement(name = Constants.FIELD_CIRCLE_ID, nillable = true, required = true)
    private String circleId = null;

    @XmlElement(name = Constants.FIELD_FOLDER_ID, nillable = true, required = true)
    private String folderId = null;

    @XmlElement(name = Constants.FIELD_DATA_NAME, nillable = true, required = true)
    private String dataName = null;

    @XmlElement(name = Constants.FIELD_DATATYPE, required = true)
    private DataType dataType = null;

    @XmlElement(name = Constants.FIELD_ADDED)
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

    public void setDataType(final DataType dataType) {
        this.dataType = dataType;
    }

    public DataType getDataType() {
        return dataType;
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
                Objects.equals(dataType, that.dataType) &&
                Objects.equals(added, that.added);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(dataId, circleId, folderId, dataName, dataType, added);
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
                ", dataType=" + dataType +
                ", added=" + added +
                '}';
    }
}
