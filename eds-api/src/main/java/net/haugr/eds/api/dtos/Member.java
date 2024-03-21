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
package net.haugr.eds.api.dtos;

import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.MemberRole;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * <p>The Member Object, is used as Accounts in EDS. The Object consists of an
 * Authentication Object, a description, creation and modification dates.</p>
 *
 * <p>The Authentication Object is mandatory, the description is optional, and
 * only serves as a hint for other Members.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_MEMBER_ID,
        Constants.FIELD_ACCOUNT_NAME,
        Constants.FIELD_MEMBER_ROLE,
        Constants.FIELD_PUBLIC_KEY,
        Constants.FIELD_ADDED })
public final class Member implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The MemberId. */
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID)
    @JsonbNillable
    private String memberId = null;

    /** The AccountName. */
    @JsonbProperty(value = Constants.FIELD_ACCOUNT_NAME)
    @JsonbNillable
    private String accountName = null;

    /** The Member Role. */
    @JsonbProperty(value = Constants.FIELD_MEMBER_ROLE)
    @JsonbNillable
    private MemberRole memberRole = null;

    // The Public Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything. For the same reason,
    // it is not used as part of the Standard Object methods, #equals(),
    // #hashCode() and #toString().
    /** The Public Key. */
    @JsonbProperty(value = Constants.FIELD_PUBLIC_KEY)
    @JsonbNillable
    private String publicKey = null;

    /** The Member Since. */
    @JsonbProperty(value = Constants.FIELD_ADDED)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
    @JsonbNillable
    private LocalDateTime added = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public Member() {
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
     * Set the AccountName.
     *
     * @param accountName AccountName
     */
    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    /**
     * Retrieves the AccountName.
     *
     * @return AccountName
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Set the Member Role.
     *
     * @param memberRole Member Role
     */
    public void setMemberRole(final MemberRole memberRole) {
        this.memberRole = memberRole;
    }

    /**
     * Retrieves the Member Role.
     *
     * @return Member Role
     */
    public MemberRole getMemberRole() {
        return memberRole;
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
     * Set the Member Since.
     *
     * @param added Member Since
     */
    public void setAdded(final LocalDateTime added) {
        this.added = added;
    }

    /**
     * Retriever the Member Since.
     *
     * @return Member Since
     */
    public LocalDateTime getAdded() {
        return added;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Member{" +
                "memberId='" + memberId + '\'' +
                ", accountName='" + accountName + '\'' +
                ", memberRole='" + memberRole + '\'' +
                ", added=" + added +
                '}';
    }
}
