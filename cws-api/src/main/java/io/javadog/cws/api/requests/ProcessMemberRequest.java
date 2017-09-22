/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.dtos.Authentication;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "processMemberRequest", propOrder = { "action", "memberId", "accountName", "newCredentialType", "newCredential" })
public final class ProcessMemberRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final Set<Action> ALLOWED = EnumSet.of(Action.PROCESS, Action.INVITE, Action.DELETE);

    private static final int MAX_NAME_LENGTH = 256;
    private static final String FIELD_ACTION = "action";
    private static final String FIELD_MEMBER_ID = "memberId";
    private static final String FIELD_ACCOUNT_NAME = "accountName";
    private static final String FIELD_NEW_CREDENTIAL_TYPE = "newCredentialType";
    private static final String FIELD_NEW_CREDENTIAL = "newCredential";

    @XmlElement(name = FIELD_ACTION, required = true)
    private Action action = Action.PROCESS;

    @XmlElement(name = FIELD_MEMBER_ID)
    private String memberId = null;

    @XmlElement(name = FIELD_ACCOUNT_NAME)
    private String accountName = null;

    @XmlElement(name = FIELD_NEW_CREDENTIAL_TYPE)
    private CredentialType newCredentialType = null;

    @XmlElement(name = FIELD_NEW_CREDENTIAL)
    private String newCredential = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * <p>Sets the Action for the processing Request. Allowed values from the
     * Actions enumerated Object includes:</p>
     * <ul>
     *   <li>PROCESS</li>
     *   <li>INVITE</li>
     *   <li>DELETE</li>
     * </ul>
     *
     * @param action Current Action
     * @throws IllegalArgumentException if the value is null or not allowed
     */
    @NotNull
    public void setAction(final Action action) {
        ensureNotNull(FIELD_ACTION, action);
        ensureValidEntry(FIELD_ACTION, action, ALLOWED);
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public void setMemberId(final String memberId) {
        ensureValidId(FIELD_MEMBER_ID, memberId);
        this.memberId = memberId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setAccountName(final String accountName) {
        ensureNotEmptyOrTooLong(FIELD_ACCOUNT_NAME, accountName, MAX_NAME_LENGTH);
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setNewCredentialType(final CredentialType newCredentialType) {
        this.newCredentialType = newCredentialType;
    }

    public CredentialType getNewCredentialType() {
        return newCredentialType;
    }

    public void setNewCredential(final String newCredential) {
        this.newCredential = newCredential;
    }

    public String getNewCredential() {
        return newCredential;
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
            errors.put(FIELD_ACTION, "No action has been provided.");
        } else if (!ALLOWED.contains(action)) {
            errors.put(FIELD_ACCOUNT_NAME, "Not supported Action has been provided.");
        } else {
            if (action == Action.INVITE) {
                checkNotNullOrEmpty(errors, FIELD_ACCOUNT_NAME, accountName, "");
            } else if (action == Action.DELETE) {
                checkNotNullAndValidId(errors, FIELD_MEMBER_ID, memberId, "A valid " + FIELD_MEMBER_ID + " is required to delete an account.");
            } else if (action == Action.PROCESS) {
                checkValidId(errors, FIELD_MEMBER_ID, memberId, "The " + FIELD_MEMBER_ID + " must be valid.");
                if (memberId == null) {
                    checkNotNullOrEmpty(errors, FIELD_ACCOUNT_NAME, accountName, "The " + FIELD_ACCOUNT_NAME + " is required for a new Account.");
                }
                checkNotTooLong(errors, FIELD_ACCOUNT_NAME, accountName, MAX_NAME_LENGTH, "The " + FIELD_ACCOUNT_NAME + " may not exceed " + MAX_NAME_LENGTH + " characters.");
            }
        }

        return errors;
    }
}
