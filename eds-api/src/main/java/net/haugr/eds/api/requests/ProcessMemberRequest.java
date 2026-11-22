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

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.Management;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.ByteArrayAdapter;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import java.io.Serial;
import java.util.Map;

/**
 * <p>It is possible to create new Member Accounts in 2 different ways, both
 * require the System Administrator. The first is to use the CREATE action and
 * set the AccountName and Credentials directly. If this is not desirable, then
 * the second version, INVITE, can be used to create an Invitation. This will
 * generate an invitation token which the invited Member can use to update their own
 * Account and set the credentials without the System Administrator knowing
 * about them.</p>
 *
 * <p>Only the System Administrator can add new Members to the system, this
 * limitation was added to prevent that the usage of a specific EDS instance is
 * growing out of control. For many setups, it is also necessary to prevent that
 * anyone who should not be a Member becomes a Member.</p>
 *
 * <ul>
 *   <li><b>CREATE</b> - For creating a new Member</li>
 *   <li><b>INVITE</b> - For inviting a new Member</li>
 *   <li><b>UPDATE</b> - For updating an existing Member Account</li>
 *   <li><b>INVALIDATE</b> - For invalidating an existing Member Account</li>
 *   <li><b>DELETE</b> - For deleting a Member Account</li>
 * </ul>
 *
 * <p>Action <b>CREATE</b>; This request can only be performed by the System
 * Administrator and requires that the name of the new Account is set, and it
 * must be a unique name with 1 to 75 characters. It is also required that the
 * credentials (password / passphrase) are set, these should be of significant
 * strength to ensure that it is not possible to easily crack it.</p>
 *
 * <p>Action <b>INVITE</b>; This request can only be performed by the System
 * Administrator and only requires that the name of the new Account is set, the
 * Account name must be unique and between 1 and 75 characters.</p>
 *
 * <p>Action <b>UPDATE</b>; This request can be invoked by any Member and will
 * allow that the Account name, to a new <i>unique</i> and credentials is being
 * updated. It should be noted that the System Administrator cannot alter the
 * Account Name, as this name is specifically used several internal operations.</p>
 *
 * <p>Action <b>INVALIDATE</b>; This request does not take any parameters, as
 * it will only work on the requesting Member, by re-generating the internal
 * Asymmetric Key, used for accessing Circles. By re-issuing it but not updating
 * the Circle access, the Account will appear to be working, but any request for
 * data will result in errors. The Account can be restored by having the access
 * to each Circle re-created, but this must be done by the Circle Administrators
 * of each Circle where the Member has access.</p>
 *
 * <p>Action <b>DELETE</b>; if the request is invoked by the System
 * Administrator, then it requires a Member Id, of the Member to be deleted. The
 * Member will then be removed from the system. If a Member invokes the request,
 * then the Member's Account will be deleted. Deleting an Account is an
 * irreversible action. However, as there is no correlation stored regarding the
 * data in the Circles which the Member belongs to, no data will be removed as
 * part of this request.</p>
 *
 * <p>For more details, please see the 'processMember' request in the Management
 * interface: {@link Management#processMember(ProcessMemberRequest)}</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_ACTION,
        Constants.FIELD_MEMBER_ID,
        Constants.FIELD_MEMBER_ROLE,
        Constants.FIELD_PUBLIC_KEY,
        Constants.FIELD_NEW_ACCOUNT_NAME,
        Constants.FIELD_NEW_CREDENTIAL,
        Constants.FIELD_AVATAR,
        Constants.FIELD_SETTINGS,
        Constants.FIELD_VAULT,
        Constants.FIELD_SECRET,
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_TRUSTLEVEL })
@Schema(description = "Request to create, invite, update, alter, invalidate or delete a Member, or to log in and out. Which fields are used depends on the endpoint.")
public final class ProcessMemberRequest extends Authentication implements ActionRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String CHARACTERS = " characters.";

    /** The Action. */
    @Schema(description = "Set by the endpoint; a value sent in the request is ignored.")
    @JsonbProperty(value = Constants.FIELD_ACTION)
    @JsonbNillable
    private Action action = null;

    /** The MemberId. */
    @Schema(description = "ID of the Member to alter, invalidate or delete.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID)
    @JsonbNillable
    private String memberId = null;

    /** The Member Role. */
    @Schema(description = "The new role, when altering a Member.")
    @JsonbProperty(value = Constants.FIELD_MEMBER_ROLE)
    @JsonbNillable
    private MemberRole memberRole = null;

    // The Public Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything.
    /** The Public Key. */
    @Schema(description = "Optional external public key to register for the Member.")
    @JsonbProperty(value = Constants.FIELD_PUBLIC_KEY)
    @JsonbNillable
    private String publicKey = null;

    /** The New AccountName. */
    @Schema(description = "The account name for a new or invited Member, or the new name when renaming.", maxLength = Constants.MAX_NAME_LENGTH, examples = { "bob" })
    @JsonbProperty(value = Constants.FIELD_NEW_ACCOUNT_NAME)
    @JsonbNillable
    private String newAccountName = null;

    /** The New Credential (Password or Passphrase). */
    @Schema(description = "A new credential, Base64-encoded: the passphrase for a new Member or a credential change (must meet the entropy rules).", type = SchemaType.STRING, format = "byte")
    @JsonbProperty(value = Constants.FIELD_NEW_CREDENTIAL)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    @JsonbNillable
    private byte[] newCredential = null;

    /** The Avatar. */
    @Schema(description = "When updating the own account: the new avatar, the ID of a picture the clients know; an empty value removes it, null leaves it as it is.",
            pattern = Constants.AVATAR_PATTERN_REGEX, maxLength = Constants.MAX_AVATAR_LENGTH, examples = { "raven" })
    @JsonbProperty(value = Constants.FIELD_AVATAR)
    @JsonbNillable
    private String avatar = null;

    /** The Member's own settings for clients. */
    @Schema(description = "When updating the own account: the Member's settings for clients, a JSON object as text of at most "
            + "4096 characters, which EDS stores as it is; an empty value removes them, null leaves them as they are.",
            maxLength = Constants.MAX_MEMBER_SETTINGS_LENGTH, examples = { "{\"language\":\"de\"}" })
    @JsonbProperty(value = Constants.FIELD_SETTINGS)
    @JsonbNillable
    private String settings = null;

    /** The Member's vault. */
    @Schema(description = "When updating the own account or accepting an invitation: the Member's vault, text which EDS stores "
            + "without looking inside and returns to the Member only; an empty value removes it, null leaves it as it is.",
            maxLength = Constants.MAX_VAULT_LENGTH)
    @JsonbProperty(value = Constants.FIELD_VAULT)
    @JsonbNillable
    private String vault = null;

    /** The secret of the invited Member in the Circle. */
    @Schema(description = "When inviting into a Circle: the secret of the invited Member in that Circle, text which "
            + "EDS stores without looking inside. It is kept with the invitation and becomes the Trustee's "
            + "secret when the invitation is accepted, or at once for an account which exists already.",
            maxLength = Constants.MAX_SECRET_LENGTH)
    @JsonbProperty(value = Constants.FIELD_SECRET)
    @JsonbNillable
    private String secret = null;

    @Schema(description = "When inviting: the Circle the invited Member joins, with `trustLevel`. Required for Members who aren't the System "
            + "Administrator, who must be an administrator (trust level ADMIN) of the Circle. An existing account is added to the Circle at once.",
            pattern = Constants.ID_PATTERN_REGEX, examples = { "8ba34e12-8830-4a1f-9681-b689cad52009" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    @Schema(description = "When inviting into a Circle: the trust level of the invited Member; default WRITE.")
    @JsonbProperty(value = Constants.FIELD_TRUSTLEVEL)
    @JsonbNillable
    private TrustLevel trustLevel = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public ProcessMemberRequest() {
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
     * Set the PublicKey.
     *
     * @param publicKey PublicKey
     */
    public void setPublicKey(final String publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * Retrieves the PublicKey.
     *
     * @return PublicKey
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Set the New AccountName.
     *
     * @param newAccountName New AccountName
     */
    public void setNewAccountName(final String newAccountName) {
        this.newAccountName = newAccountName;
    }

    /**
     * Retrieves the New AccountName.
     *
     * @return New AccountName
     */
    public String getNewAccountName() {
        return newAccountName;
    }

    /**
     * Set the New Credential (Password or Passphrase).
     *
     * @param newCredential New Credential
     */
    public void setNewCredential(final byte[] newCredential) {
        this.newCredential = Utilities.copy(newCredential);
    }

    /**
     * Retrieves the New Credential (Password or Passphrase).
     *
     * @return New Credential
     */
    public byte[] getNewCredential() {
        return Utilities.copy(newCredential);
    }

    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    public String getCircleId() {
        return circleId;
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
            final String newAccountErrorMessage = "The " + Constants.FIELD_NEW_ACCOUNT_NAME + " may not exceed " + Constants.MAX_NAME_LENGTH + CHARACTERS;
            switch (action) {
                case CREATE:
                    checkNotNullOrEmpty(errors, Constants.FIELD_NEW_ACCOUNT_NAME, newAccountName, "The New Account Name is missing.");
                    checkNotTooLong(errors, Constants.FIELD_NEW_ACCOUNT_NAME, newAccountName, Constants.MAX_NAME_LENGTH, newAccountErrorMessage);
                    checkNotNullOrEmpty(errors, Constants.FIELD_NEW_CREDENTIAL, newCredential, "The Credentials are required to create new Account.");
                    break;
                case INVITE:
                    checkNotNullOrEmpty(errors, Constants.FIELD_NEW_ACCOUNT_NAME, newAccountName, "The New Account Name is missing.");
                    checkNotTooLong(errors, Constants.FIELD_NEW_ACCOUNT_NAME, newAccountName, Constants.MAX_NAME_LENGTH, newAccountErrorMessage);
                    checkValidId(errors, Constants.FIELD_CIRCLE_ID, circleId, "The given circleId is invalid.");
                    checkNotTooLong(errors, Constants.FIELD_SECRET, secret, Constants.MAX_SECRET_LENGTH, "The secret must be at most " + Constants.MAX_SECRET_LENGTH + CHARACTERS);
                    if ((secret != null) && (circleId == null)) {
                        errors.put(Constants.FIELD_SECRET, "A secret belongs to a Circle, so it needs a circleId.");
                    }
                    break;
                case ALTER:
                    checkNotNullAndValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "The given memberId is invalid.");
                    checkNotNull(errors, Constants.FIELD_MEMBER_ROLE, memberRole, "The Role is missing.");
                    break;
                case UPDATE:
                    checkNotTooLong(errors, Constants.FIELD_NEW_ACCOUNT_NAME, newAccountName, Constants.MAX_NAME_LENGTH, newAccountErrorMessage);
                    checkValidAvatar(errors, avatar);
                    checkNotTooLong(errors, Constants.FIELD_SETTINGS, settings, Constants.MAX_MEMBER_SETTINGS_LENGTH,
                            "The settings must be at most " + Constants.MAX_MEMBER_SETTINGS_LENGTH + CHARACTERS);
                    checkNotTooLong(errors, Constants.FIELD_VAULT, vault, Constants.MAX_VAULT_LENGTH, "The vault must be at most " + Constants.MAX_VAULT_LENGTH + CHARACTERS);
                    break;
                case DELETE:
                    checkValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "The given memberId is invalid.");
                    break;
                case LOGIN, LOGOUT, INVALIDATE:
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

    /**
     * Sets the secret of the invited Member in the Circle.
     *
     * @param secret The secret of the invited Member in the Circle
     */
    public void setSecret(final String secret) {
        this.secret = secret;
    }

    /**
     * Retrieves the secret of the invited Member in the Circle.
     *
     * @return The secret of the invited Member in the Circle
     */
    public String getSecret() {
        return secret;
    }
}
