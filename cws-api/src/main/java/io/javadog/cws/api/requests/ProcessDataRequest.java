/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Constants.MAX_STRING_LENGTH;
import static io.javadog.cws.api.common.Utilities.copy;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;

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
@XmlType(name = "processDataRequest", propOrder = { "action", "id", "circleId", "folderId", "name", "typeName", "bytes" })
public final class ProcessDataRequest extends Authentication implements CircleIdRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final Set<Action> ALLOWED = EnumSet.of(Action.ADD, Action.UPDATE, Action.DELETE);

    private static final String FIELD_ACTION = "action";
    private static final String FIELD_ID = "id";
    private static final String FIELD_CIRCLE_ID = "circleId";
    private static final String FIELD_FOLDER_ID = "folderId";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPENAME = "typeName";
    private static final String FIELD_BYTES = "bytes";

    @XmlElement(name = FIELD_ACTION, required = true)
    private Action action = Action.PROCESS;

    @XmlElement(name = FIELD_ID, nillable = true, required = true)
    private String id = null;

    @XmlElement(name = FIELD_CIRCLE_ID, nillable = true)
    private String circleId = null;

    @XmlElement(name = FIELD_FOLDER_ID, nillable = true)
    private String folderId = null;

    @XmlElement(name = FIELD_NAME, nillable = true)
    private String name = null;

    @XmlElement(name = FIELD_TYPENAME)
    private String typeName = null;

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

    public void setId(final String id) {
        ensureValidId(FIELD_ID, id);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCircleId() {
        return circleId;
    }

    public void setFolderId(final String folderId) {
        this.folderId = folderId;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
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
            switch (action) {
                case ADD:
                    checkNotNullAndValidId(errors, FIELD_CIRCLE_ID, circleId, "The Circle Id is missing or invalid.");
                    checkValidId(errors, FIELD_FOLDER_ID, folderId, "The Folder Id is invalid.");
                    checkNotNullEmptyOrTooLong(errors, FIELD_NAME, name, MAX_STRING_LENGTH, "The name of the new Data Object is invalid.");
                    checkNotNullOrEmpty(errors, FIELD_TYPENAME, typeName, "The Data Type is missing or invalid.");
                    break;
                case UPDATE:
                    checkNotNullAndValidId(errors, FIELD_ID, id, "The Id is missing or invalid.");
                    checkValidId(errors, FIELD_FOLDER_ID, folderId, "The Folder Id is invalid.");
                    checkNotTooLong(errors, FIELD_NAME, name, MAX_STRING_LENGTH, "The name of the new Data Object is invalid.");
                    break;
                case DELETE:
                    checkNotNullAndValidId(errors, FIELD_ID, id, "The Id is missing or invalid.");
                    break;
                default:
                    errors.put(FIELD_ACTION, "Invalid Action provided.");
            }
        }

        return errors;
    }
}
