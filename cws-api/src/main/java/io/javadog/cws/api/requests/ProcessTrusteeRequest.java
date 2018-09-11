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
import io.javadog.cws.api.common.TrustLevel;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;

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
 * interface: {@link io.javadog.cws.api.Management#processTrustee(ProcessTrusteeRequest)}</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "processTrusteeRequest")
@XmlType(name = "processTrusteeRequest", propOrder = { Constants.FIELD_ACTION, Constants.FIELD_CIRCLE_ID, Constants.FIELD_MEMBER_ID, Constants.FIELD_TRUSTLEVEL })
public final class ProcessTrusteeRequest extends Authentication implements CircleIdRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_CIRCLE_ID, nillable = true)
    private String circleId = null;

    @NotNull
    @XmlElement(name = Constants.FIELD_ACTION, required = true)
    private Action action = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_MEMBER_ID, nillable = true)
    private String memberId = null;

    @XmlElement(name = Constants.FIELD_TRUSTLEVEL, nillable = true)
    private TrustLevel trustLevel = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

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

    public void setAction(final Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
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
            errors.put(Constants.FIELD_ACTION, "No action has been provided.");
        } else {
            switch (action) {
                case ADD:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot add a Trustee to a Circle, without a Circle Id.");
                    checkNotNullAndValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "Cannot add a Trustee to a Circle, without a Member Id.");
                    checkNotNull(errors, Constants.FIELD_TRUSTLEVEL, trustLevel, "Cannot add a Trustee to a Circle, without an initial TrustLevel.");
                    break;
                case ALTER:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot alter a Trustees TrustLevel, without knowing the Circle Id.");
                    checkNotNullAndValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "Cannot alter a Trustees TrustLevel, without knowing the Member Id.");
                    checkNotNull(errors, Constants.FIELD_TRUSTLEVEL, trustLevel, "Cannot alter a Trustees TrustLevel, without knowing the new TrustLevel.");
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
}
