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
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.Map;

/**
 * <p>Circles only have an Id, a name, and an optional External Circle Key,
 * the Key is stored encrypted, if set. The name must be present, unique, and
 * between 1 and 75 characters long. When invoking the processCircle request,
 * it is possible to perform the following actions:</p>
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
 * by anyone who has Administrative right in the Circle.</p>
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
        Constants.FIELD_AVATAR,
        Constants.FIELD_MEMBER_ID,
        Constants.FIELD_CIRCLE_KEY,
        Constants.FIELD_SECRET })
@Schema(description = "Request to create, update or delete a Circle. Which fields are used depends on the endpoint.")
public final class ProcessCircleRequest extends Authentication implements CircleIdRequest, ActionRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String CHARACTERS = " characters.";

    /** The Action. */
    @Schema(description = "Set by the endpoint; a value sent in the request is ignored.")
    @JsonbProperty(value = Constants.FIELD_ACTION)
    @JsonbNillable
    private Action action = null;

    /** The CircleId. */
    @Schema(description = "ID of the Circle to update or delete.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** The Circle Name. */
    @Schema(description = "Name of the Circle, unique in EDS. Required when creating.", maxLength = Constants.MAX_NAME_LENGTH, examples = { "Project Phoenix" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_NAME)
    @JsonbNillable
    private String circleName = null;

    /** The Avatar. */
    @Schema(description = "The Circle's avatar, the ID of a picture the clients know. When updating, an empty value removes it, null leaves it as it is.",
            pattern = Constants.AVATAR_PATTERN_REGEX, maxLength = Constants.MAX_AVATAR_LENGTH, examples = { "longship" })
    @JsonbProperty(value = Constants.FIELD_AVATAR)
    @JsonbNillable
    private String avatar = null;

    /** The MemberId. */
    @Schema(description = "When creating: ID of the Member who becomes the Circle's first administrator.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID)
    @JsonbNillable
    private String memberId = null;

    // The Circle Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything.
    /** The Circle Key. */
    @Schema(description = "Optional external key to store with the Circle, see Circle.circleKey.")
    @JsonbProperty(value = Constants.FIELD_CIRCLE_KEY)
    @JsonbNillable
    private String circleKey = null;

    /** The secret of the first administrator in the new Circle. */
    @Schema(description = "When creating: the secret of the Circle's first administrator in it, text which EDS "
            + "stores without looking inside, e.g. a key of the Circle wrapped for them by a client.",
            maxLength = Constants.MAX_SECRET_LENGTH)
    @JsonbProperty(value = Constants.FIELD_SECRET)
    @JsonbNillable
    private String secret = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public ProcessCircleRequest() {
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
                    checkNotTooLong(errors, Constants.FIELD_CIRCLE_NAME, circleName, Constants.MAX_NAME_LENGTH,
                            "The " + Constants.FIELD_CIRCLE_NAME + " may not exceed " + Constants.MAX_NAME_LENGTH + CHARACTERS);
                    checkValidAvatar(errors, avatar);
                    checkNotTooLong(errors, Constants.FIELD_SECRET, secret, Constants.MAX_SECRET_LENGTH, "The secret must be at most " + Constants.MAX_SECRET_LENGTH + CHARACTERS);
                    break;
                case UPDATE:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot update the Circle Name, without knowing the Circle Id.");
                    checkNotTooLong(errors, Constants.FIELD_CIRCLE_NAME, circleName, Constants.MAX_NAME_LENGTH,
                            "The " + Constants.FIELD_CIRCLE_NAME + " may not exceed " + Constants.MAX_NAME_LENGTH + CHARACTERS);
                    checkValidAvatar(errors, avatar);
                    break;
                case DELETE:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot delete a Circle, without knowing the Circle Id.");
                    break;
                case ROTATE:
                    checkNotNullAndValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "Cannot rotate the Circle Secret, without knowing the Circle Id.");
                    break;
                default:
                    errors.put(Constants.FIELD_ACTION, "Not supported Action has been provided.");
                    break;
            }
        }

        return errors;
    }

    /**
     * The Avatar
     *
     * @param avatar The Avatar
     */
    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    /**
     * The Avatar
     *
     * @return The Avatar
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * Sets the secret of the first administrator in the new Circle.
     *
     * @param secret The secret of the first administrator in the new Circle
     */
    public void setSecret(final String secret) {
        this.secret = secret;
    }

    /**
     * Retrieves the secret of the first administrator in the new Circle.
     *
     * @return The secret of the first administrator in the new Circle
     */
    public String getSecret() {
        return secret;
    }
}
