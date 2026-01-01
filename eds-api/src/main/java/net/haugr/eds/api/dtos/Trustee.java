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
        Constants.FIELD_PUBLIC_KEY,
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_CIRCLE_NAME,
        Constants.FIELD_TRUSTLEVEL,
        Constants.FIELD_ADDED,
        Constants.FIELD_CHANGED })
public final class Trustee implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** MemberId. */
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID)
    @JsonbNillable
    private String memberId = null;

    /** Account Name. */
    @JsonbProperty(value = Constants.FIELD_ACCOUNT_NAME)
    @JsonbNillable
    private String accountName = null;

    // The Public Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything. For the same reason,
    // it is not used as part of the Standard Object methods, #equals(),
    // #hashCode() and #toString().
    /** Public Key. */
    @JsonbProperty(value = Constants.FIELD_PUBLIC_KEY)
    @JsonbNillable
    private String publicKey = null;

    /** CircleId. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** Circle Name. */
    @JsonbProperty(value = Constants.FIELD_CIRCLE_NAME)
    @JsonbNillable
    private String circleName = null;

    /** Trust Level. */
    @JsonbProperty(value = Constants.FIELD_TRUSTLEVEL)
    @JsonbNillable
    private TrustLevel trustLevel = null;

    /** Created Timestamp. */
    @JsonbProperty(value = Constants.FIELD_ADDED)
    @JsonbDateFormat
    @JsonbNillable
    private LocalDateTime added = null;

    /** Last Modified Timestamp. */
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
}
