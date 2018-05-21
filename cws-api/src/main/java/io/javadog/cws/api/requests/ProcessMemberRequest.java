/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Utilities.copy;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
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
 * <p>When invoked with the INVALIDATE action, CWS will simply generate a new
 * internal asymmetric key for the Account, but not update the access to any
 * existing Circles, meaning that the Account will work as &quot;normal&quot;,
 * but no data from any Circles can be retrieved - only the metadata information
 * can be access. An invalidated Account, can be corrected again, when the
 * Circle Administrators of all Circles where the Member has access, re-issues
 * the access.</p>
 *
 * <p>If the DELETE action is used, by the System Administrator, any (other than
 * the System Administrator) Account, identified by the required Member Id, will
 * be deleted. If invoked by the Member, then the Member Account will be
 * deleted.Regardless, this action is irreversible, as all information about
 * the Account will be removed. It is important to note, that the data uploaded
 * by the, now deleted, Account is not removed, as there is no correlation
 * between Accounts and Data stored anywhere. Data is deleted, once the Circles
 * where the data is stored is being deleted.</p>
 *
 * <p>For more details, please see the 'processMember' request in the Management
 * interface: {@link io.javadog.cws.api.Management#processMember(ProcessMemberRequest)}</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "processMemberRequest")
@XmlType(name = "processMemberRequest", propOrder = { Constants.FIELD_ACTION, Constants.FIELD_MEMBER_ID,  Constants.FIELD_PUBLIC_KEY, Constants.FIELD_NEW_ACCOUNT_NAME, Constants.FIELD_NEW_CREDENTIAL })
public final class ProcessMemberRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @NotNull
    @XmlElement(name = Constants.FIELD_ACTION, required = true)
    private Action action = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = Constants.FIELD_MEMBER_ID, nillable = true)
    private String memberId = null;

    // The Public Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything.
    @XmlElement(name = Constants.FIELD_PUBLIC_KEY, required = true)
    private String publicKey = null;

    @Size(min = 1, max = Constants.MAX_NAME_LENGTH)
    @XmlElement(name = Constants.FIELD_NEW_ACCOUNT_NAME, nillable = true)
    private String newAccountName = null;

    @XmlElement(name = Constants.FIELD_NEW_CREDENTIAL, nillable = true)
    private byte[] newCredential = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

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
        this.newCredential = copy(newCredential);
    }

    public byte[] getNewCredential() {
        return copy(newCredential);
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
                case UPDATE:
                    checkNotTooLong(errors, Constants.FIELD_NEW_ACCOUNT_NAME, newAccountName, Constants.MAX_NAME_LENGTH, newAccountErrorMessage);
                    break;
                case INVALIDATE:
                    break;
                case DELETE:
                    checkValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "The given memberId is invalid.");
                    break;
                default:
                    errors.put(Constants.FIELD_ACTION, "Not supported Action has been provided.");
                    break;
            }
        }

        return errors;
    }
}
