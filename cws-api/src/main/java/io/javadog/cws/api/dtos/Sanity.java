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
package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Utilities;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Date;

/**
 * The Sanity Object contain information about a Data record, which has failed
 * the sanity check, i.e. the encrypted bytes have been changed, so it is no
 * longer possible to decrypt the Object. The information returned include the
 * Id of the Data Object which failed the sanity check, and the date at which it
 * failed it.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = Constants.FIELD_SANITY, propOrder = { Constants.FIELD_DATA_ID, Constants.FIELD_CHANGED })
public final class Sanity implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_DATA_ID)
    private String dataId = null;

    @XmlElement(name = Constants.FIELD_CHANGED)
    private Date changed = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setDataId(final String dataId) {
        this.dataId = dataId;
    }

    public String getDataId() {
        return dataId;
    }

    public void setChanged(final Date changed) {
        this.changed = Utilities.copy(changed);
    }

    public Date getChanged() {
        return Utilities.copy(changed);
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Sanity{" +
                "dataId='" + dataId + '\'' +
                ", changed=" + changed +
                '}';
    }
}
