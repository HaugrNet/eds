/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.Management;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.dtos.TrusteeSecret;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
        Constants.FIELD_MEMBER_ID,
        Constants.FIELD_SECRET,
        Constants.FIELD_CIRCLE_KEY,
        Constants.FIELD_SECRETS })
@Schema(description = "Request to add, alter or remove a Trustee, i.e. a Member's membership of a Circle, or to set the secrets of Trustees.")
public final class ProcessTrusteeRequest extends Authentication implements CircleIdRequest, ActionRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Action. */
    @Schema(description = "Set by the endpoint; a value sent in the request is ignored.")
    @JsonbProperty(value = Constants.FIELD_ACTION)
    @JsonbNillable
    private Action action = null;

    /** The Trust Level. */
    @Schema(description = "The trust level to grant: READ, WRITE or ADMIN. Required when adding; when altering, the trust level, the secret or both are given.")
    @JsonbProperty(value = Constants.FIELD_TRUSTLEVEL)
    @JsonbNillable
    private TrustLevel trustLevel = null;

    /** The CircleId. */
    @Schema(description = "ID of the Circle.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** The MemberId. */
    @Schema(description = "ID of the Member.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID)
    @JsonbNillable
    private String memberId = null;

    /** The secret of the Trustee. */
    @Schema(description = "When adding or altering: the Trustee's secret in the Circle, text which EDS stores without looking inside, e.g. "
            + "a key of the Circle wrapped for this Member by a client. An empty value removes it, null leaves it as it is.",
            maxLength = Constants.MAX_SECRET_LENGTH)
    @JsonbProperty(value = Constants.FIELD_SECRET)
    @JsonbNillable
    private String secret = null;

    /** The external key of the Circle. */
    @Schema(description = "When setting secrets: the external key to store with the Circle, see Circle.circleKey; null leaves the stored one as it is.")
    @JsonbProperty(value = Constants.FIELD_CIRCLE_KEY)
    @JsonbNillable
    private String circleKey = null;

    /** The secrets of several Trustees. */
    @Schema(description = "When setting secrets: the secrets of Trustees of the Circle. Either all are set or none.")
    @JsonbProperty(value = Constants.FIELD_SECRETS)
    @JsonbNillable
    private List<TrusteeSecret> secrets = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public ProcessTrusteeRequest() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
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
                    checkSecret(errors, Constants.FIELD_SECRET, secret);
                    break;
                case ALTER:
                    validateAlter(errors);
                    break;
                case SECRETS:
                    validateSecrets(errors);
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

    private void validateAlter(final Map<String, String> errors) {
        checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot alter a Trustees TrustLevel, without knowing the Circle Id.");
        checkNotNullAndValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "Cannot alter a Trustees TrustLevel, without knowing the Member Id.");
        if (secret == null) {
            checkNotNull(errors, Constants.FIELD_TRUSTLEVEL, trustLevel, "Cannot alter a Trustee, without a new TrustLevel or a secret.");
        }
        checkValidTrustLevel(errors, trustLevel);
        checkSecret(errors, Constants.FIELD_SECRET, secret);
    }

    private void validateSecrets(final Map<String, String> errors) {
        checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot set secrets, without knowing the Circle Id.");
        if ((circleKey == null) && ((secrets == null) || secrets.isEmpty())) {
            errors.put(Constants.FIELD_SECRETS, "Neither secrets nor a circleKey were given.");
        }
        if (secrets != null) {
            for (final TrusteeSecret entry : secrets) {
                if (entry == null) {
                    errors.put(Constants.FIELD_SECRETS, "An entry of the secrets is missing.");
                } else {
                    checkNotNullAndValidId(errors, Constants.FIELD_SECRETS, entry.getMemberId(), "Every secret needs a valid Member Id.");
                    checkNotNull(errors, Constants.FIELD_SECRETS, entry.getSecret(), "Every entry needs a secret; an empty one removes it.");
                    checkSecret(errors, Constants.FIELD_SECRETS, entry.getSecret());
                }
            }
        }
    }

    private static void checkSecret(final Map<String, String> errors, final String field, final String value) {
        checkNotTooLong(errors, field, value, Constants.MAX_SECRET_LENGTH, "A secret must be at most " + Constants.MAX_SECRET_LENGTH + " characters.");
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

    /**
     * Sets the secret of the Trustee.
     *
     * @param secret The secret of the Trustee
     */
    public void setSecret(final String secret) {
        this.secret = secret;
    }

    /**
     * Retrieves the secret of the Trustee.
     *
     * @return The secret of the Trustee
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Sets the external key of the Circle.
     *
     * @param circleKey The external key of the Circle
     */
    public void setCircleKey(final String circleKey) {
        this.circleKey = circleKey;
    }

    /**
     * Retrieves the external key of the Circle.
     *
     * @return The external key of the Circle
     */
    public String getCircleKey() {
        return circleKey;
    }

    /**
     * Sets the secrets of several Trustees.
     *
     * @param secrets The secrets of several Trustees
     */
    public void setSecrets(final List<TrusteeSecret> secrets) {
        this.secrets = (secrets == null) ? null : new ArrayList<>(secrets);
    }

    /**
     * Retrieves the secrets of several Trustees.
     *
     * @return The secrets of several Trustees
     */
    public List<TrusteeSecret> getSecrets() {
        return (secrets == null) ? null : new ArrayList<>(secrets);
    }
}
