/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Constants.FIELD_ACTION;
import static io.javadog.cws.api.common.Constants.FIELD_TYPE;
import static io.javadog.cws.api.common.Constants.FIELD_TYPENAME;
import static io.javadog.cws.api.common.Constants.MAX_NAME_LENGTH;
import static io.javadog.cws.api.common.Constants.MAX_STRING_LENGTH;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "processDataTypeRequest", propOrder = { FIELD_ACTION, FIELD_TYPENAME, FIELD_TYPE })
public final class ProcessDataTypeRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = FIELD_ACTION, required = true)
    private Action action = Action.PROCESS;

    @NotNull
    @Size(min = 1, max = MAX_NAME_LENGTH)
    @XmlElement(name = FIELD_TYPENAME, required = true)
    private String typeName = null;

    @NotNull
    @Size(min = 1, max = MAX_STRING_LENGTH)
    @XmlElement(name = FIELD_TYPE, required = true)
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
            errors.put(FIELD_ACTION, "No action has been provided.");
        } else {
            switch (action) {
                case PROCESS:
                    checkNotNullEmptyOrTooLong(errors, FIELD_TYPENAME, typeName, MAX_NAME_LENGTH, "The name of the DataType is missing or invalid.");
                    checkNotNullEmptyOrTooLong(errors, FIELD_TYPE, type, MAX_STRING_LENGTH, "The type of the DataType is missing or invalid.");
                    break;
                case DELETE:
                    checkNotNullEmptyOrTooLong(errors, FIELD_TYPENAME, typeName, MAX_NAME_LENGTH, "The name of the DataType is missing or invalid.");
                    break;
                default:
                    errors.put(FIELD_ACTION, "Invalid Action provided.");
            }
        }

        return errors;
    }
}
