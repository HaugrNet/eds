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

import net.haugr.eds.api.Management;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
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
 * interface: {@link Management#processCircle(ProcessCircleRequest)}</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_ACTION,
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_CIRCLE_NAME,
        Constants.FIELD_MEMBER_ID,
        Constants.FIELD_CIRCLE_KEY })
public final class ProcessCircleRequest extends Authentication implements CircleIdRequest, ActionRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Action. */
    @JsonbProperty(value = Constants.FIELD_ACTION, nillable = true)
    private Action action = null;

    /** The CircleId. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID, nillable = true)
    private String circleId = null;

    /** The Circle Name. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_NAME, nillable = true)
    private String circleName = null;

    /** The MemberId. */
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID, nillable = true)
    private String memberId = null;

    // The Circle Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything.
    /** The Circle Key. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_KEY, nillable = true)
    private String circleKey = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

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

    /**
     * Set the CircleName.
     *
     * @param circleName CircleName
     */
    public void setCircleName(final String circleName) {
        this.circleName = circleName;
    }

    /**
     * Retrieves the CircleName.
     *
     * @return CircleName
     */
    public String getCircleName() {
        return circleName;
    }

    /**
     * Set the MemberId.
     *
     * @param memberId MemberId
     */
    public void setMemberId(final String memberId) {
        this.memberId = memberId;
    }

    /**
     * Retrieves the MemberId.
     *
     * @return MemberId
     */
    public String getMemberId() {
        return memberId;
    }

    /**
     * Set the CircleKey.
     *
     * @param circleKey CircleKey
     */
    public void setCircleKey(final String circleKey) {
        this.circleKey = circleKey;
    }

    /**
     * Retrieves the CircleKey.
     *
     * @return CircleKey
     */
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
