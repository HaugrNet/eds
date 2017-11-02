/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Constants.FIELD_ACCOUNT_NAME;
import static io.javadog.cws.api.common.Constants.FIELD_ACTION;
import static io.javadog.cws.api.common.Constants.FIELD_MEMBER_ID;
import static io.javadog.cws.api.common.Constants.FIELD_NEW_ACCOUNT_NAME;
import static io.javadog.cws.api.common.Constants.FIELD_NEW_CREDENTIAL;
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
@XmlType(name = "processMemberRequest", propOrder = { FIELD_ACTION, FIELD_MEMBER_ID, FIELD_NEW_ACCOUNT_NAME, FIELD_NEW_CREDENTIAL })
public final class ProcessMemberRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @NotNull
    @XmlElement(name = FIELD_ACTION, required = true)
    private Action action = null;

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    @XmlElement(name = FIELD_MEMBER_ID, required = true)
    private String memberId = null;

    @Size(min = 1, max = MAX_NAME_LENGTH)
    @XmlElement(name = FIELD_NEW_ACCOUNT_NAME, required = true)
    private String newAccountName = null;

    @XmlElement(name = FIELD_NEW_CREDENTIAL, required = true)
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

    public void setNewAccountName(final String newAccountName) {
        this.newAccountName = newAccountName;
    }

    public String getNewAccountName() {
        return newAccountName;
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
                    checkNotNullOrEmpty(errors, FIELD_NEW_ACCOUNT_NAME, newAccountName, "The Account Name os missing.");
                    checkNotNullOrEmpty(errors, FIELD_NEW_CREDENTIAL, newCredential, "The Credentials are required to create new Account.");
                    break;
                case INVITE:
                    checkNotNullOrEmpty(errors, FIELD_NEW_ACCOUNT_NAME, newAccountName, "The Account Name os missing.");
                    break;
                case PROCESS:
                    checkNotTooLong(errors, FIELD_NEW_ACCOUNT_NAME, newAccountName, MAX_STRING_LENGTH, "The " + FIELD_NEW_ACCOUNT_NAME + " may not exceed " + MAX_STRING_LENGTH + " characters.");
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
