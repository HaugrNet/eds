/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.dtos.Data;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProcessDataRequest", propOrder = { "action", "dataId", "data" })
public final class ProcessDataRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final Set<Action> ALLOWED = EnumSet.of(Action.PROCESS, Action.DELETE);

    private static final String FIELD_ACTION = "action";
    private static final String FIELD_DATA_ID = "dataId";
    private static final String FIELD_DATA = "data";

    @XmlElement(name = FIELD_ACTION, required = true)
    private Action action = null;

    @XmlElement(name = "FIELD_DATA_ID", required = true, nillable = true)
    private String dataId = null;

    @XmlElement(name = FIELD_DATA, required = true, nillable = true)
    private Data data = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * <p>Sets the Action for the processing Request. Allowed values from the
     * Actions enumerated Object includes:</p>
     * <ul>
     *   <li>PROCESS</li>
     *   <li>DELETE</li>
     * </ul>
     *
     * @param action Current Action
     * @throws IllegalArgumentException if the value is null or not allowed
     */
    @NotNull
    public void setAction(final Action action) {
        ensureNotNull(FIELD_ACTION, action);
        ensureValidEntry(FIELD_ACTION, action, ALLOWED);
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public void setDataId(final String dataId) {
        ensureValidId(FIELD_DATA_ID, dataId);
        this.dataId = dataId;
    }

    public String getDataId() {
        return dataId;
    }

    public void setData(final Data data) {
        ensureVerifiable(FIELD_DATA, data);
        this.data = data;
    }

    public Data getData() {
        return data;
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
            if (action == Action.PROCESS) {
                if (data == null) {
                    errors.put(FIELD_DATA, "Data is missing, null or invalid.");
                } else {
                    errors.putAll(data.validate());
                }
            } else if (action == Action.DELETE) {
                if (dataId == null) {
                    errors.put(FIELD_DATA_ID, "Missing or invalid Data Id.");
                }
            } else {
                errors.put(FIELD_ACTION, "Invalid Action provided.");
            }
        }

        return errors;
    }
}
