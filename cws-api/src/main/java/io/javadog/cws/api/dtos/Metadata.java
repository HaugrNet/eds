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
@XmlType(name = "metadata", propOrder = { "dataId", "circleId", "folderId", "name", "dataType", "added" })
public final class Metadata implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_DATA_ID = "dataId";
    private static final String FIELD_CIRCLE_ID = "circleId";
    private static final String FIELD_FOLDER_ID = "folderId";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DATATYPE = "dataType";
    private static final String FIELD_ADDED = "added";

    @XmlElement(name = FIELD_DATA_ID, nillable = true, required = true)
    private String dataId = null;

    @XmlElement(name = FIELD_CIRCLE_ID, nillable = true, required = true)
    private String circleId = null;

    @XmlElement(name = FIELD_FOLDER_ID, nillable = true, required = true)
    private String folderId = null;

    @XmlElement(name = FIELD_NAME, nillable = true, required = true)
    private String name = null;

    @XmlElement(name = FIELD_DATATYPE, required = true)
    private DataType dataType = null;

    @XmlElement(name = FIELD_ADDED)
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

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
                Objects.equals(name, that.name) &&
                Objects.equals(dataType, that.dataType) &&
                Objects.equals(added, that.added);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(dataId, circleId, folderId, name, dataType, added);
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
                ", name='" + name + '\'' +
                ", dataType=" + dataType +
                ", added=" + added +
                '}';
    }
}
