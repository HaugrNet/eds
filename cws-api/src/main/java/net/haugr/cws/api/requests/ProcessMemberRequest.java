/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.api.requests;

import net.haugr.cws.api.Management;
import net.haugr.cws.api.common.Action;
import net.haugr.cws.api.common.ByteArrayAdapter;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.MemberRole;
import net.haugr.cws.api.common.Utilities;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTypeAdapter;
import java.util.Map;

/**
 * <p>It is possible to create new Member Accounts in 2 different ways, both
 * require the System Administrator. First is to use the CREATE action, and
 * set the AccountName and Credentials directly. If this is not desirable, then
 * the second version, INVITE, can be used ti create an Invitation, which will
 * generate a signature which the invited Member can use to update their own
 * Account and set the credentials without the System Administrator knowing of
 * them.</p>
 *
 * <p>Only the System Administrator can add new Members to the system, this
 * limitation was added to prevent that the usage of a specific CWS instance is
 * growing out of control. For many setups, it is also needed to prevent that
 * anyone who should not be a Member, becomes a Member.</p>
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
 * Administrator, and requires that the name of the new Account is set, and it
 * must be a unique name with 1 to 75 characters. It is also required, that the
 * credentials (password / passphrase) is set, these should be of significant
 * strength to ensure that it is not possible to easily crack it.</p>
 *
 * <p>Action <b>INVITE</b>; This request can only be performed by the System
 * Administrator, and only requires that the name of the new Account is set, the
 * Account name must be unique and between 1 to 75 characters.</p>
 *
 * <p>Action <b>UPDATE</b>; This request can be invoked by any Member, and will
 * allow that the Account name, to a new <i>unique</i> and credentials is being
 * updated. It should be noted, that the System Administrator cannot alter the
 * Account Name, as this name is specifically used several internal operations.</p>
 *
 * <p>Action <b>INVALIDATE</b>; This request does not take any parameters, as
 * it will only work on the requesting Member, by re-generating the internal
 * Asymmetric Key, used for accessing Circles. By re-issuing it, but not update
 * the Circle access, the Account will appear to be working, but any request for
 * data will result in errors. The Account can be restored, by having the access
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
 * @since CWS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_ACTION,
        Constants.FIELD_MEMBER_ID,
        Constants.FIELD_MEMBER_ROLE,
        Constants.FIELD_PUBLIC_KEY,
        Constants.FIELD_NEW_ACCOUNT_NAME,
        Constants.FIELD_NEW_CREDENTIAL })
public final class ProcessMemberRequest extends Authentication implements ActionRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @JsonbProperty(value = Constants.FIELD_ACTION, nillable = true)
    private Action action = null;

    @JsonbProperty(value = Constants.FIELD_MEMBER_ID, nillable = true)
    private String memberId = null;

    @JsonbProperty(value = Constants.FIELD_MEMBER_ROLE, nillable = true)
    private MemberRole memberRole = null;

    // The Public Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything.
    @JsonbProperty(value = Constants.FIELD_PUBLIC_KEY, nillable = true)
    private String publicKey = null;

    @JsonbProperty(value = Constants.FIELD_NEW_ACCOUNT_NAME, nillable = true)
    private String newAccountName = null;

    @JsonbProperty(value = Constants.FIELD_NEW_CREDENTIAL, nillable = true)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    private byte[] newCredential = null;

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

    public void setMemberId(final String memberId) {
        this.memberId = memberId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberRole(final MemberRole memberRole) {
        this.memberRole = memberRole;
    }

    public MemberRole getMemberRole() {
        return memberRole;
    }

    public void setPublicKey(final String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setNewAccountName(final String newAccountName) {
        this.newAccountName = newAccountName;
    }

    public String getNewAccountName() {
        return newAccountName;
    }

    public void setNewCredential(final byte[] newCredential) {
        this.newCredential = Utilities.copy(newCredential);
    }

    public byte[] getNewCredential() {
        return Utilities.copy(newCredential);
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
            final String newAccountErrorMessage = "The " + Constants.FIELD_NEW_ACCOUNT_NAME + " may not exceed " + Constants.MAX_STRING_LENGTH + " characters.";
            switch (action) {
                case CREATE:
                    checkNotNullOrEmpty(errors, Constants.FIELD_NEW_ACCOUNT_NAME, newAccountName, "The New Account Name is missing.");
                    checkNotTooLong(errors, Constants.FIELD_NEW_ACCOUNT_NAME, newAccountName, Constants.MAX_NAME_LENGTH, newAccountErrorMessage);
                    checkNotNullOrEmpty(errors, Constants.FIELD_NEW_CREDENTIAL, newCredential, "The Credentials are required to create new Account.");
                    break;
                case INVITE:
                    checkNotNullOrEmpty(errors, Constants.FIELD_NEW_ACCOUNT_NAME, newAccountName, "The New Account Name is missing.");
                    checkNotTooLong(errors, Constants.FIELD_NEW_ACCOUNT_NAME, newAccountName, Constants.MAX_NAME_LENGTH, newAccountErrorMessage);
                    break;
                case LOGIN:
                    checkNotNullOrEmpty(errors, Constants.FIELD_NEW_CREDENTIAL, newCredential, "The Credentials are required to create new Session.");
                    break;
                case ALTER:
                    checkNotNullAndValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "The given memberId is invalid.");
                    checkNotNull(errors, Constants.FIELD_MEMBER_ROLE, memberRole, "The Role is missing.");
                    break;
                case UPDATE:
                    checkNotTooLong(errors, Constants.FIELD_NEW_ACCOUNT_NAME, newAccountName, Constants.MAX_NAME_LENGTH, newAccountErrorMessage);
                    break;
                case DELETE:
                    checkValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "The given memberId is invalid.");
                    break;
                case LOGOUT:
                case INVALIDATE:
                    break;
                default:
                    errors.put(Constants.FIELD_ACTION, "Not supported Action has been provided.");
                    break;
            }
        }

        return errors;
    }
}
