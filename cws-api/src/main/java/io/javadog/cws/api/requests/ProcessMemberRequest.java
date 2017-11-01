/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Constants.MAX_NAME_LENGTH;
import static io.javadog.cws.api.common.Constants.MAX_STRING_LENGTH;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "processMemberRequest", propOrder = { "action", "memberId", "accountName", "newCredential" })
public final class ProcessMemberRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_ACTION = "action";
    private static final String FIELD_MEMBER_ID = "memberId";
    private static final String FIELD_ACCOUNT_NAME = "accountName";
    private static final String FIELD_NEW_CREDENTIAL = "newCredential";

    @NotNull
    @XmlElement(name = FIELD_ACTION, required = true)
    private Action action = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = FIELD_MEMBER_ID)
    private String memberId = null;

    @Size(min = 1, max = MAX_NAME_LENGTH)
    @XmlElement(name = FIELD_ACCOUNT_NAME)
    private String accountName = null;

    @XmlElement(name = FIELD_NEW_CREDENTIAL)
    private String newCredential = null;

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

    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
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
        } else {
            switch (action) {
                case CREATE:
                    checkNotNullOrEmpty(errors, FIELD_ACCOUNT_NAME, accountName, "The Account Name os missing.");
                    checkNotNullOrEmpty(errors, FIELD_NEW_CREDENTIAL, newCredential, "The Credentials are required to create new Account.");
                    break;
                case INVITE:
                    checkNotNullOrEmpty(errors, FIELD_ACCOUNT_NAME, accountName, "The Account Name os missing.");
                    break;
                case PROCESS:
                    checkNotTooLong(errors, FIELD_ACCOUNT_NAME, accountName, MAX_STRING_LENGTH, "The " + FIELD_ACCOUNT_NAME + " may not exceed " + MAX_STRING_LENGTH + " characters.");
                    break;
                case DELETE:
                    checkNotNullAndValidId(errors, FIELD_MEMBER_ID, memberId, "A valid " + FIELD_MEMBER_ID + " is required to delete an account.");
                    break;
                default:
                    errors.put(FIELD_ACCOUNT_NAME, "Not supported Action has been provided.");
            }
        }

        return errors;
    }
}
