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

import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.common.Constants;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.io.Serializable;

/**
 * <p>All data in EDS must have a designated DataType, this can be either of the
 * two default values ('Folders' &amp; 'Data'), but it may also be something
 * else.</p>
 *
 * <p>If the EDS instance is mainly used for file sharing, the DataTypes can be
 * setup with the various supported MIME Types. If the EDS instance is used for
 * Application Data sharing, then it may be used to contain some helpful
 * information about what the content may reflect, i.e., Object Type and perhaps
 * Object Version.</p>
 *
 * <p>If not needed, then it can be left alone. Default DataType in EDS is
 * 'data'.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_TYPENAME, Constants.FIELD_TYPE })
public final class DataType implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** Type Name. */
    @JsonbProperty(value = Constants.FIELD_TYPENAME)
    @JsonbNillable
    private String typeName = null;

    /** Type. */
    @JsonbProperty(value = Constants.FIELD_TYPE)
    @JsonbNillable
    private String type = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public DataType() {
        // Empty Constructor
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
     * Set the Type.
     *
     * @param type Type
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Retrieves the Type.
     *
     * @return Type
     */
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
