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
@XmlType(name = "circle", propOrder = { "circleId", "name", "created" })
public final class Circle implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String FIELD_CIRCLE_ID = "circleId";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_CREATED = "created";

    @XmlElement(name = FIELD_CIRCLE_ID, required = true)
    private String circleId = null;

    @XmlElement(name = FIELD_NAME, required = true)
    private String name = null;

    @XmlElement(name = FIELD_CREATED)
    private Date created = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    public String getCircleId() {
        return circleId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCreated(final Date created) {
        this.created = copy(created);
    }

    public Date getCreated() {
        return copy(created);
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

        if (!(obj instanceof Circle)) {
            return false;
        }

        final Circle that = (Circle) obj;
        return Objects.equals(circleId, that.circleId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(created, that.created);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(circleId, name, created);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Circle{" +
                "circleId='" + circleId + '\'' +
                ", name='" + name + '\'' +
                ", created=" + created +
                '}';
    }
}
