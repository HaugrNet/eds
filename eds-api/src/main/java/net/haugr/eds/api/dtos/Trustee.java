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
package net.haugr.eds.api.dtos;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.TrustLevel;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * <p>A Trustee is a Member of a Circle, with a granted Trust Level.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_MEMBER_ID,
        Constants.FIELD_ACCOUNT_NAME,
        Constants.FIELD_AVATAR,
        Constants.FIELD_PUBLIC_KEY,
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_CIRCLE_NAME,
        Constants.FIELD_CIRCLE_AVATAR,
        Constants.FIELD_TRUSTLEVEL,
        Constants.FIELD_SECRET,
        Constants.FIELD_ADDED,
        Constants.FIELD_CHANGED })
@Schema(description = "A Trustee: a Member's membership of a Circle, with the trust level granted.")
public final class Trustee implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** MemberId. */
    @Schema(description = "ID of the Member.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID)
    @JsonbNillable
    private String memberId = null;

    /** Account Name. */
    @Schema(description = "The Member's account name, unique in EDS.", maxLength = Constants.MAX_NAME_LENGTH, examples = { "alice" })
    @JsonbProperty(value = Constants.FIELD_ACCOUNT_NAME)
    @JsonbNillable
    private String accountName = null;

    // The Public Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything. For the same reason,
    // it is not used as part of the Standard Object methods, #equals(),
    // #hashCode() and #toString().
    /** Public Key. */
    @Schema(description = "The Member's external public key, if registered.")
    @JsonbProperty(value = Constants.FIELD_PUBLIC_KEY)
    @JsonbNillable
    private String publicKey = null;

    // Like the Public Key, the secret is only stored, and not part of the
    // Standard Object methods.
    /** Secret. */
    @Schema(description = "The Trustee's secret in the Circle: text which EDS stores without looking inside, e.g. a key of the Circle wrapped for this Member by a client. "
            + "Only returned to the Member themselves and to the administrators of the Circle; null without one.",
            maxLength = Constants.MAX_SECRET_LENGTH)
    @JsonbProperty(value = Constants.FIELD_SECRET)
    @JsonbNillable
    private String secret = null;

    /** CircleId. */
    @Schema(description = "ID of the Circle.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** Circle Name. */
    @Schema(description = "Name of the Circle, unique in EDS.", maxLength = Constants.MAX_NAME_LENGTH, examples = { "Project Phoenix" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_NAME)
    @JsonbNillable
    private String circleName = null;

    /** The Member's Avatar. */
    @Schema(description = "The Member's avatar, see Member.avatar.", pattern = Constants.AVATAR_PATTERN_REGEX, maxLength = Constants.MAX_AVATAR_LENGTH, examples = { "raven" })
    @JsonbProperty(value = Constants.FIELD_AVATAR)
    @JsonbNillable
    private String avatar = null;

    /** The Circle's Avatar. */
    @Schema(description = "The Circle's avatar, see Circle.avatar.", pattern = Constants.AVATAR_PATTERN_REGEX, maxLength = Constants.MAX_AVATAR_LENGTH, examples = { "longship" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_AVATAR)
    @JsonbNillable
    private String circleAvatar = null;

    /** Trust Level. */
    @Schema(description = "The Member's trust level in the Circle: READ, WRITE or ADMIN.")
    @JsonbProperty(value = Constants.FIELD_TRUSTLEVEL)
    @JsonbNillable
    private TrustLevel trustLevel = null;

    /** Created Timestamp. */
    @Schema(description = "When the Member was added to the Circle. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_ADDED)
    @JsonbDateFormat
    @JsonbNillable
    private LocalDateTime added = null;

    /** Last Modified Timestamp. */
    @Schema(description = "When the trust level was last changed. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_CHANGED)
    @JsonbDateFormat
    @JsonbNillable
    private LocalDateTime changed = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public Trustee() {
        // Empty Constructor
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
     * Set the Account Name.
     *
     * @param accountName Account Name
     */
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    /**
     * Retrieves the Account Name.
     *
     * @return Account Name
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Set the Public Key.
     *
     * @param publicKey Public Key
     */
    public void setPublicKey(final String publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * Retrieves the Public Key.
     *
     * @return Public Key
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Sets the CircleId.
     *
     * @param circleId CircleId
     */
    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    /**
     * Retrieves the CircleId.
     *
     * @return CircleId
     */
    public String getCircleId() {
        return circleId;
    }

    /**
     * Sets the Circle Name.
     *
     * @param circleName Circle Name
     */
    public void setCircleName(String circleName) {
        this.circleName = circleName;
    }

    /**
     * Retrieves the Circle Name.
     *
     * @return Circle Name
     */
    public String getCircleName() {
        return circleName;
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
     * Set the Created Timestamp.
     *
     * @param added Created Timestamp
     */
    public void setAdded(final LocalDateTime added) {
        this.added = added;
    }

    /**
     * Retrieves the Created Timestamp.
     *
     * @return Created Timestamp
     */
    public LocalDateTime getAdded() {
        return added;
    }

    /**
     * Set the Last Modified Timestamp.
     *
     * @param changed Last Modified Timestamp
     */
    public void setChanged(final LocalDateTime changed) {
        this.changed = changed;
    }

    /**
     * Retrieves the Last Modified Timestamp.
     *
     * @return Last Modified Timestamp
     */
    public LocalDateTime getChanged() {
        return changed;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        // Note that the public key is omitted deliberately.
        return "Trustee{" +
                "memberId=" + memberId +
                ", circleId=" + circleId +
                ", trustLevel=" + trustLevel +
                ", added=" + added +
                ", changed=" + changed +
                '}';
    }

    /**
     * The Member's Avatar
     *
     * @param avatar The Member's Avatar
     */
    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    /**
     * The Member's Avatar
     *
     * @return The Member's Avatar
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * The Circle's Avatar
     *
     * @param circleAvatar The Circle's Avatar
     */
    public void setCircleAvatar(final String circleAvatar) {
        this.circleAvatar = circleAvatar;
    }

    /**
     * The Circle's Avatar
     *
     * @return The Circle's Avatar
     */
    public String getCircleAvatar() {
        return circleAvatar;
    }

    /**
     * Sets the Trustee's secret in the Circle.
     *
     * @param secret The Trustee's secret in the Circle
     */
    public void setSecret(final String secret) {
        this.secret = secret;
    }

    /**
     * Retrieves the Trustee's secret in the Circle.
     *
     * @return The Trustee's secret in the Circle
     */
    public String getSecret() {
        return secret;
    }
}
