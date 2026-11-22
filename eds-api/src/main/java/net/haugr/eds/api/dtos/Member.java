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
import net.haugr.eds.api.common.MemberRole;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * <p>The Member Object is used as Accounts in EDS. The Object consists of an
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
        Constants.FIELD_AVATAR,
        Constants.FIELD_SETTINGS,
        Constants.FIELD_VAULT,
        Constants.FIELD_PUBLIC_KEY,
        Constants.FIELD_ADDED })
@Schema(description = "A Member account, as other Members see it.")
public final class Member implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The MemberId. */
    @Schema(description = "ID of the Member.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID)
    @JsonbNillable
    private String memberId = null;

    /** The AccountName. */
    @Schema(description = "The Member's account name, unique in EDS.", maxLength = Constants.MAX_NAME_LENGTH, examples = { "alice" })
    @JsonbProperty(value = Constants.FIELD_ACCOUNT_NAME)
    @JsonbNillable
    private String accountName = null;

    /** The Member Role. */
    @Schema(description = "The Member's role in EDS: ADMIN (System Administrator) or STANDARD.")
    @JsonbProperty(value = Constants.FIELD_MEMBER_ROLE)
    @JsonbNillable
    private MemberRole memberRole = null;

    /** Whether the Member is the first System Administrator. */
    @Schema(description = "True for the first System Administrator: the account which claimed the stone. "
            + "It can't lose its role or be deleted, only it gives the MasterKey, and only it makes, unmakes "
            + "or deletes other administrators. False for every other Member.")
    @JsonbProperty(value = Constants.FIELD_FIRST_ADMINISTRATOR)
    private boolean firstAdministrator = false;

    /** The Avatar. */
    @Schema(description = "The Member's avatar: the ID of a picture the clients know (e.g. `raven`), or null for none.",
            pattern = Constants.AVATAR_PATTERN_REGEX, maxLength = Constants.MAX_AVATAR_LENGTH, examples = { "raven" })
    @JsonbProperty(value = Constants.FIELD_AVATAR)
    @JsonbNillable
    private String avatar = null;

    /** The Member's own settings for clients. */
    @Schema(description = "The Member's own settings for clients, e.g. language and theme: a JSON object as text, "
            + "which EDS stores as it is. Only returned to the Member themselves; null without any.",
            maxLength = Constants.MAX_MEMBER_SETTINGS_LENGTH, examples = { "{\"language\":\"de\"}" })
    @JsonbProperty(value = Constants.FIELD_SETTINGS)
    @JsonbNillable
    private String settings = null;

    /** The Member's vault. */
    @Schema(description = "The Member's vault: text which EDS stores without looking inside, e.g. the Member's own private key, "
            + "wrapped by a client, so their other devices can fetch it. Only returned to the Member themselves; null without one.",
            maxLength = Constants.MAX_VAULT_LENGTH)
    @JsonbProperty(value = Constants.FIELD_VAULT)
    @JsonbNillable
    private String vault = null;

    // The Public Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything. For the same reason,
    // it is not used as part of the Standard Object methods, #equals(),
    // #hashCode() and #toString().
    /** The Public Key. */
    @Schema(description = "Optional external public key the Member has registered, e.g. for clients that encrypt data themselves. Stored as given.")
    @JsonbProperty(value = Constants.FIELD_PUBLIC_KEY)
    @JsonbNillable
    private String publicKey = null;

    /** The Member Since. */
    @Schema(description = "When the account was created. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_ADDED)
    @JsonbDateFormat
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
     * Sets whether the Member is the first System Administrator.
     *
     * @param firstAdministrator True for the first System Administrator
     */
    public void setFirstAdministrator(final boolean firstAdministrator) {
        this.firstAdministrator = firstAdministrator;
    }

    /**
     * Whether the Member is the first System Administrator.
     *
     * @return True for the first System Administrator
     */
    public boolean isFirstAdministrator() {
        return firstAdministrator;
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
                ", firstAdministrator=" + firstAdministrator +
                ", added=" + added +
                '}';
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
     * The Member's own settings for clients
     *
     * @param settings JSON object as text
     */
    public void setSettings(final String settings) {
        this.settings = settings;
    }

    /**
     * The Member's own settings for clients
     *
     * @return JSON object as text
     */
    public String getSettings() {
        return settings;
    }

    /**
     * Sets the Member's vault.
     *
     * @param vault The Member's vault
     */
    public void setVault(final String vault) {
        this.vault = vault;
    }

    /**
     * Retrieves the Member's vault.
     *
     * @return The Member's vault
     */
    public String getVault() {
        return vault;
    }
}
