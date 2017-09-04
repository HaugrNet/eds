/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Utilities.copy;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.dtos.Metadata;

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
@XmlType(name = "processDataRequest", propOrder = { "action", "dataId", "metadata", "bytes" })
public final class ProcessDataRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final Set<Action> ALLOWED = EnumSet.of(Action.PROCESS, Action.DELETE);

    private static final String FIELD_ACTION = "action";
    private static final String FIELD_DATA_ID = "dataId";
    private static final String FIELD_DATA = "metadata";
    private static final String FIELD_BYTES = "bytes";

    @XmlElement(name = FIELD_ACTION, required = true)
    private Action action = Action.PROCESS;

    @XmlElement(name = FIELD_DATA_ID, nillable = true, required = true)
    private String dataId = null;

    @XmlElement(name = FIELD_DATA, nillable = true, required = true)
    private Metadata metadata = null;

    @XmlElement(name = FIELD_BYTES, nillable = true)
    private byte[] bytes = null;

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

    public void setMetadata(final Metadata metadata) {
        ensureVerifiable(FIELD_DATA, metadata);
        this.metadata = metadata;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setBytes(final byte[] bytes) {
        this.bytes = copy(bytes);
    }

    public byte[] getBytes() {
        return copy(bytes);
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
                if (metadata == null) {
                    errors.put(FIELD_DATA, "Data is missing, null or invalid.");
                } else {
                    errors.putAll(metadata.validate());
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
