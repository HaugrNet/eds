/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Constants.MAX_NAME_LENGTH;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.TrustLevel;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
@XmlType(name = "processCircleRequest", propOrder = { "action", "circleId", "circleName", "memberId", "trustLevel" })
public final class ProcessCircleRequest extends Authentication implements CircleIdRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_ACTION = "action";
    private static final String FIELD_CIRCLE_ID = "circleId";
    private static final String FIELD_CIRCLE_NAME = "circleName";
    private static final String FIELD_MEMBER_ID = "memberId";
    private static final String FIELD_TRUSTLEVEL = "trustlevel";

    @NotNull
    @XmlElement(name = FIELD_ACTION, required = true)
    private Action action = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = FIELD_CIRCLE_ID)
    private String circleId = null;

    @Size(min = 1, max = MAX_NAME_LENGTH)
    @XmlElement(name = FIELD_CIRCLE_NAME)
    private String circleName = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = FIELD_MEMBER_ID)
    private String memberId = null;

    @XmlElement(name = FIELD_TRUSTLEVEL)
    private TrustLevel trustLevel = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setAction(final Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
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
                    checkNotNullOrEmpty(errors, FIELD_CIRCLE_NAME, circleName, "Cannot create a new Circle, without the Circle Name.");
                    checkNotNullAndValidId(errors, FIELD_MEMBER_ID, memberId, "Cannot create a new Circle, without an initial Circle Administrator, please provide a Member Id.");
                    break;
                case UPDATE:
                    checkNotNullAndValidId(errors, FIELD_CIRCLE_ID, circleId, "Cannot update the Circle Name, without knowing the Circle Id.");
                    checkNotNullOrEmpty(errors, FIELD_CIRCLE_NAME, circleName, "Cannot update the Circle Name, without a new Circle Name.");
                    break;
                case DELETE:
                    checkNotNullAndValidId(errors, FIELD_CIRCLE_ID, circleId, "Cannot delete a Circle, without knowing the Circle Id.");
                    break;
                case ADD:
                    checkNotNullAndValidId(errors, FIELD_CIRCLE_ID, circleId, "Cannot add a Trustee to a Circle, without a Circle Id.");
                    checkValidId(errors, FIELD_MEMBER_ID, memberId, "Cannot add a Trustee to a Circle, without a Member Id.");
                    checkNotNull(errors, FIELD_TRUSTLEVEL, trustLevel, "Cannot add a Trustee to a Circle, without an initial TrustLevel.");
                    break;
                case ALTER:
                    checkNotNullAndValidId(errors, FIELD_CIRCLE_ID, circleId, "Cannot alter a Trustees TrustLevel, without knowing the Circle Id.");
                    checkValidId(errors, FIELD_MEMBER_ID, memberId, "Cannot alter a Trustees TrustLevel, without knowing the Member Id.");
                    checkNotNull(errors, FIELD_TRUSTLEVEL, trustLevel, "Cannot alter a Trustees TrustLevel, without knowing the new TrustLevel.");
                    break;
                case REMOVE:
                    checkNotNullAndValidId(errors, FIELD_CIRCLE_ID, circleId, "Cannot remove a Trustee from a Circle, without knowing the Circle Id.");
                    checkValidId(errors, FIELD_MEMBER_ID, memberId, "Cannot remove a Trustee from a Circle, without knowing the Member Id.");
                    break;
                default:
                    errors.put(FIELD_ACTION, "Invalid Action provided.");
            }
        }

        return errors;
    }
}
