/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.api.dtos;

import net.haugr.cws.api.common.Constants;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serializable;

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
@JsonbPropertyOrder({ Constants.FIELD_TYPENAME, Constants.FIELD_TYPE })
public final class DataType implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @JsonbProperty(value = Constants.FIELD_TYPENAME, nillable = true)
    private String typeName = null;

    @JsonbProperty(value = Constants.FIELD_TYPE, nillable = true)
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
