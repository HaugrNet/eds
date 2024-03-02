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
import net.haugr.eds.api.common.TrustLevel;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>Request Object for the processing of Trustee's. It supports the following
 * actions:</p>
 *
 * <ul>
 *   <li><b>ADD</b> - For adding a new Trustee</li>
 *   <li><b>ALTER</b> - For altering an existing Trustee</li>
 *   <li><b>REMOVE</b> - For removing an existing Trustee</li>
 * </ul>
 *
 * <p>Action <b>ADD</b>; requires that both the Circle &amp; Member Id's are
 * provided, together with the {@link TrustLevel}, for the new Trustee.</p>
 *
 * <p>Action <b>ALTER</b>; requires that both the Circle &amp; Member Id's are
 * provided, together with the new {@link TrustLevel} setting for the
 * Trustee.</p>
 *
 * <p>Action <b>REMOVE</b>; requires that both the Circle &amp; Member Id's are
 * provided, so the relation can be removed.</p>
 *
 * <p>For more details, please see the 'processTrustee' request in the Management
 * interface: {@link Management#processTrustee(ProcessTrusteeRequest)}</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_ACTION,
        Constants.FIELD_TRUSTLEVEL,
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_MEMBER_ID })
public final class ProcessTrusteeRequest extends Authentication implements CircleIdRequest, ActionRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Action. */
    @JsonbProperty(value = Constants.FIELD_ACTION, nillable = true)
    private Action action = null;

    /** The Trust Level. */
    @JsonbProperty(value = Constants.FIELD_TRUSTLEVEL, nillable = true)
    private TrustLevel trustLevel = null;

    /** The CircleId. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID, nillable = true)
    private String circleId = null;

    /** The MemberId. */
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID, nillable = true)
    private String memberId = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public ProcessTrusteeRequest() {
    }

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
     * Set the Trust Level.
     *
     * @param trustLevel Trust Level
     */
    public void setTrustLevel(final TrustLevel trustLevel) {
        this.trustLevel = trustLevel;
    }

    /**
     * Retrieves the Trust Level.
     *
     * @return Trust Level
     */
    public TrustLevel getTrustLevel() {
        return trustLevel;
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
                case ADD:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot add a Trustee to a Circle, without a Circle Id.");
                    checkNotNullAndValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "Cannot add a Trustee to a Circle, without a Member Id.");
                    checkNotNull(errors, Constants.FIELD_TRUSTLEVEL, trustLevel, "Cannot add a Trustee to a Circle, without an initial TrustLevel.");
                    checkValidTrustLevel(errors, trustLevel);
                    break;
                case ALTER:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot alter a Trustees TrustLevel, without knowing the Circle Id.");
                    checkNotNullAndValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "Cannot alter a Trustees TrustLevel, without knowing the Member Id.");
                    checkNotNull(errors, Constants.FIELD_TRUSTLEVEL, trustLevel, "Cannot alter a Trustees TrustLevel, without knowing the new TrustLevel.");
                    checkValidTrustLevel(errors, trustLevel);
                    break;
                case REMOVE:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot remove a Trustee from a Circle, without knowing the Circle Id.");
                    checkNotNullAndValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "Cannot remove a Trustee from a Circle, without knowing the Member Id.");
                    break;
                default:
                    errors.put(Constants.FIELD_ACTION, "Not supported Action has been provided.");
                    break;
            }
        }

        return errors;
    }

    private static void checkValidTrustLevel(final Map<String, String> errors, final TrustLevel value) {
        if (value != null) {
            final Set<TrustLevel> allowed = EnumSet.of(TrustLevel.ADMIN, TrustLevel.WRITE, TrustLevel.READ);
            if (!allowed.contains(value)) {
                errors.put(Constants.FIELD_TRUSTLEVEL, "The TrustLevel must be one of ["
                        + TrustLevel.READ + ", "
                        + TrustLevel.WRITE + ", "
                        + TrustLevel.ADMIN + "].");
            }
        }
    }
}
