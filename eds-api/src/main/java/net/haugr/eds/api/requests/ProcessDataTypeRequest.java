/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.api.requests;

import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.Share;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.Map;

/**
 * <p>When processing a DataType, it can be to either create a new or update an
 * existing. By default, 2 DataTypes exist, which cannot be modified.</p>
 *
 * <p>To create or update a custom DataType, the name of the DataType is needed
 * together with the type itself. Generally, the name is a shorthand description
 * of name of the actual Type, as the type may be a anything from a simple MIME
 * Type to a rule to extract the content of a stored Object.</p>
 *
 * <p>For more details, please see the 'processDataType' request in the Management
 * interface: {@link Share#processDataType(ProcessDataTypeRequest)}</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_ACTION,
        Constants.FIELD_TYPENAME,
        Constants.FIELD_TYPE })
public final class ProcessDataTypeRequest extends Authentication implements ActionRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Action. */
    @JsonbProperty(value = Constants.FIELD_ACTION)
    @JsonbNillable
    private Action action = Action.PROCESS;

    /** The TypeName. */
    @JsonbProperty(value = Constants.FIELD_TYPENAME)
    @JsonbNillable
    private String typeName = null;

    /** The Type. */
    @JsonbProperty(value = Constants.FIELD_TYPE)
    @JsonbNillable
    private String type = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public ProcessDataTypeRequest() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAction(final Action action) {
        this.action = action;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Action getAction() {
        return action;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = super.validate();

        if (action == null) {
            errors.put(Constants.FIELD_ACTION, "No action has been provided.");
        } else {
            switch (action) {
                case PROCESS:
                    checkNotNullEmptyOrTooLong(errors, Constants.FIELD_TYPENAME, typeName, Constants.MAX_NAME_LENGTH, "The name of the DataType is missing or invalid.");
                    checkNotNullEmptyOrTooLong(errors, Constants.FIELD_TYPE, type, Constants.MAX_STRING_LENGTH, "The type of the DataType is missing or invalid.");
                    break;
                case DELETE:
                    checkNotNullOrEmpty(errors, Constants.FIELD_TYPENAME, typeName, "The name of the DataType is missing or invalid.");
                    break;
                default:
                    errors.put(Constants.FIELD_ACTION, "Not supported Action has been provided.");
                    break;
            }
        }

        return errors;
    }
}
