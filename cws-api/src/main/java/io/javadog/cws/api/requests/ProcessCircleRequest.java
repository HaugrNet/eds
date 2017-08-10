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
import io.javadog.cws.api.common.TrustLevel;
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
@XmlType(name = "processCircleRequest", propOrder = { "action", "circleId", "circleName", "memberId", "trustLevel" })
public final class ProcessCircleRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final Set<Action> ALLOWED = EnumSet.of(Action.CREATE, Action.UPDATE, Action.DELETE, Action.ADD, Action.ALTER, Action.REMOVE);

    private static final String FIELD_ACTION = "action";
    private static final String FIELD_CIRCLE_ID = "circleId";
    private static final String FIELD_CIRCLE_NAME = "circleName";
    private static final String FIELD_MEMBER_ID = "memberId";
    private static final String FIELD_TRUSTLEVEL = "trustlevel";

    @XmlElement(name = FIELD_ACTION, required = true)
    private Action action = null;

    @XmlElement(name = FIELD_CIRCLE_ID)
    private String circleId = null;

    @XmlElement(name = FIELD_CIRCLE_NAME)
    private String circleName = null;

    @XmlElement(name = FIELD_MEMBER_ID)
    private String memberId = null;

    @XmlElement(name = FIELD_TRUSTLEVEL)
    private TrustLevel trustLevel = null;

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

    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    public String getCircleId() {
        return circleId;
    }

    public void setCircleName(final String circleName) {
        this.circleName = circleName;
    }

    public String getCircleName() {
        return circleName;
    }

    public void setMemberId(final String memberId) {
        this.memberId = memberId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setTrustLevel(final TrustLevel trustLevel) {
        this.trustLevel = trustLevel;
    }

    public TrustLevel getTrustLevel() {
        return trustLevel;
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
                case CREATE:
                    checkNotNullOrEmpty(errors, FIELD_CIRCLE_NAME, circleName, "The " + FIELD_CIRCLE_NAME + " is invalid.");
                    checkNotNullAndValidId(errors, FIELD_MEMBER_ID, memberId, "The " + FIELD_MEMBER_ID + " is missing.");
                    break;
                case UPDATE:
                    checkNotNullAndValidId(errors, FIELD_CIRCLE_ID, circleId, "Invalid " + FIELD_CIRCLE_ID + " provided.");
                    checkNotNullOrEmpty(errors, FIELD_CIRCLE_NAME, circleName, "The " + FIELD_CIRCLE_NAME + " is not valid.");
                    break;
                case DELETE:
                    checkNotNullAndValidId(errors, FIELD_CIRCLE_ID, circleId, "Invalid " + FIELD_CIRCLE_ID + " provided.");
                    break;
                case ADD:
                case ALTER:
                    checkNotNullAndValidId(errors, FIELD_CIRCLE_ID, circleId, "Invalid " + FIELD_CIRCLE_ID + " provided.");
                    checkValidId(errors, FIELD_MEMBER_ID, memberId, "The " + FIELD_MEMBER_ID + " is missing.");
                    checkNotNull(errors, FIELD_TRUSTLEVEL, trustLevel, "The " + FIELD_TRUSTLEVEL + " is missing.");
                    break;
                case REMOVE:
                    checkNotNullAndValidId(errors, FIELD_CIRCLE_ID, circleId, "Invalid " + FIELD_CIRCLE_ID + " provided.");
                    checkValidId(errors, FIELD_MEMBER_ID, memberId, "The " + FIELD_MEMBER_ID + " is missing.");
                    break;
                default:
                    errors.put(FIELD_ACTION, "Invalid Action provided.");
            }
        }

        return errors;
    }
}
