/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;

/**
 * <p>When processing a Circle, it is possible to do 3 things, either create a
 * new Circle, update an existing Circle or delete a Circle. Circles basically
 * only have a name (max 75 characters) and an Id, so when creating a new
 * Circle, only a name is required, when deleting a Circle, only the CircleId is
 * required, and when updating a circle, both is required.</p>
 *
 * <p>When creating a new Circle, the requesting member account is automatically
 * assigned as the initial Circle Administrator. If a different Circle
 * Administrator is desired, it can be set by providing the MemberId of the
 * Account to use.</p>
 *
 * <p>As with all processing request, the Action must also be set, this is used
 * to internally ascertain what should be done. This request allows the
 * following actions:</p>
 *
 * <ul>
 *   <li><b>CREATE</b> - For creating a new Circle</li>
 *   <li><b>UPDATE</b> - For updating an existing Circle</li>
 *   <li><b>DELETE</b> - For deleting an existing Circle</li>
 * </ul>
 *
 * <p>If required, it is also possible to store an External Key for the Circle,
 * the External Key is not used for anything internally - it is simply being
 * read in and stored encrypted, using the internal Circle Key. The content
 * of the External Key can thus be anything.</p>
 *
 * <p>For more details, please see the 'processCircle' request in the Management
 * interface: {@link io.javadog.cws.api.Management#processCircle(ProcessCircleRequest)}</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "processCircleRequest")
@XmlType(name = "processCircleRequest", propOrder = { Constants.FIELD_ACTION, Constants.FIELD_CIRCLE_ID, Constants.FIELD_CIRCLE_NAME, Constants.FIELD_MEMBER_ID, Constants.FIELD_CIRCKE_KEY })
public final class ProcessCircleRequest extends Authentication implements CircleIdRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @NotNull
    @XmlElement(name = Constants.FIELD_ACTION, required = true)
    private Action action = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_CIRCLE_ID, nillable = true)
    private String circleId = null;

    @Size(min = 1, max = Constants.MAX_NAME_LENGTH)
    @XmlElement(name = Constants.FIELD_CIRCLE_NAME, nillable = true)
    private String circleName = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_MEMBER_ID, nillable = true)
    private String memberId = null;

    // The Circle Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything.
    @XmlElement(name = Constants.FIELD_CIRCKE_KEY, nillable = true)
    private String circleKey = null;

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

    public void setCircleKey(final String circleKey) {
        this.circleKey = circleKey;
    }

    public String getCircleKey() {
        return circleKey;
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
                case CREATE:
                    checkNotNullOrEmpty(errors, Constants.FIELD_CIRCLE_NAME, circleName, "Cannot create a new Circle, without the Circle Name.");
                    checkNotTooLong(errors, Constants.FIELD_CIRCLE_NAME, circleName, Constants.MAX_NAME_LENGTH, "The " + Constants.FIELD_CIRCLE_NAME + " may not exceed " + Constants.MAX_NAME_LENGTH + " characters.");
                    break;
                case UPDATE:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot update the Circle Name, without knowing the Circle Id.");
                    checkNotTooLong(errors, Constants.FIELD_CIRCLE_NAME, circleName, Constants.MAX_NAME_LENGTH, "The " + Constants.FIELD_CIRCLE_NAME + " may not exceed " + Constants.MAX_NAME_LENGTH + " characters.");
                    break;
                case DELETE:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot delete a Circle, without knowing the Circle Id.");
                    break;
                default:
                    errors.put(Constants.FIELD_ACTION, "Not supported Action has been provided.");
                    break;
            }
        }

        return errors;
    }
}
