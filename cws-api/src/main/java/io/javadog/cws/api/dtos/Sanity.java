/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Utilities;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
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
 * @since CWS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_DATA_ID, Constants.FIELD_CHANGED })
public final class Sanity implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @JsonbProperty(value = Constants.FIELD_DATA_ID, nillable = true)
    private String dataId = null;

    @JsonbProperty(value = Constants.FIELD_CHANGED, nillable = true)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
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
