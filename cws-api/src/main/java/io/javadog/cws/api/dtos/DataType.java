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
import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>All data in CWS must have a designated DataType, this can be either of the
 * two default values ('Folders' &amp; 'Data'), but it may also be something
 * else.</p>
 *
 * <p>If the CWS instance it mainly used for file sharing, the DataTypes can be
 * setup with the various supported MIME Types. If the CWS instance is used for
 * Application Data sharing, then it may be used to contain some helpful
 * information about what the content may reflect, i.e. Object Type and perhaps
 * Object Version.</p>
 *
 * <p>If not needed, then it can be left alone. Default DataType in CWS is
 * 'data'.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = Constants.FIELD_DATATYPE, propOrder = { Constants.FIELD_TYPENAME, Constants.FIELD_TYPE })
public final class DataType implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_TYPENAME, required = true)
    private String typeName = null;

    @XmlElement(name = Constants.FIELD_TYPE, required = true)
    private String type = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    @Override
    public String toString() {
        return "DataType{" +
                "typeName='" + typeName + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
