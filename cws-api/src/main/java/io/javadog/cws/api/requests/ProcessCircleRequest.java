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
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;

/**
 * <p>Circles only have an Id, a name and an optional External Circle Key, the
 * Key is stored encrypted, if set. The name must be present, unique and between
 * 1 and 75 characters long. When invoking the processCircle request, it is
 * possible to perform the following actions:</p>
 *
 * <ul>
 *   <li><b>CREATE</b> - For creating a new Circle</li>
 *   <li><b>UPDATE</b> - For updating an existing Circle</li>
 *   <li><b>DELETE</b> - For deleting an existing Circle</li>
 * </ul>
 *
 * <p>Action <b>CREATE</b>; request requires a name for the Circle, which must
 * be unique, have a length between 1 &amp; 75 characters. If a Member Id is
 * also provided, then this will be the initial Circle Administrator. If no
 * Member Id has been provided, then the requesting Member will be the initial
 * Circle Administrator. It is also possible to set the Circle Key, which is
 * an external Key, shared by the members of the Circle, it will be stored
 * internally encrypted, but not used.</p>
 *
 * <p>Action <b>UPDATE</b>; request requires the Circle Id, and optionally a
 * new, unique, name for the Circle, with a length between 1 &amp; 75
 * characters. It is also possible to update the (external) Circle Key, which
 * means that the existing Circle Key will be replaced.</p>
 *
 * <p>Action <b>DELETE</b>; request requires a Circle Id, and may be performed
 * by anyone who have Administrative right in the Circle.</p>
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
