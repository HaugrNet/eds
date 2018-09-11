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
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
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
 * interface: {@link io.javadog.cws.api.Share#processDataType(ProcessDataTypeRequest)}</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "processDataTypeRequest")
@XmlType(name = "processDataTypeRequest", propOrder = { Constants.FIELD_ACTION, Constants.FIELD_TYPENAME, Constants.FIELD_TYPE })
public final class ProcessDataTypeRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_ACTION, required = true)
    private Action action = Action.PROCESS;

    @NotNull
    @Size(min = 1, max = Constants.MAX_NAME_LENGTH)
    @XmlElement(name = Constants.FIELD_TYPENAME, required = true)
    private String typeName = null;

    @NotNull
    @Size(min = 1, max = Constants.MAX_STRING_LENGTH)
    @XmlElement(name = Constants.FIELD_TYPE)
    private String type = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setAction(final Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

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
