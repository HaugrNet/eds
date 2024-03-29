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
package net.haugr.eds.api.requests;

import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.common.ByteArrayAdapter;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.Utilities;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import java.io.Serial;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Base Authentication Object for all incoming Requests. It contains the
 * name of the Account, plus the credentials to unlock the Account. Both the
 * name and credentials are mandatory, whereas the type of credential is
 * optional with a fallback to PassPhrase.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_ACCOUNT_NAME,
        Constants.FIELD_CREDENTIAL,
        Constants.FIELD_CREDENTIALTYPE })
public class Authentication extends AbstractRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** Account Name. */
    @JsonbProperty(value = Constants.FIELD_ACCOUNT_NAME)
    @JsonbNillable
    private String accountName = null;

    /** Credential (Password or Passphrase). */
    @JsonbProperty(value = Constants.FIELD_CREDENTIAL)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    @JsonbNillable
    private byte[] credential = null;

    /** Credential Type. */
    @JsonbProperty(value = Constants.FIELD_CREDENTIALTYPE)
    @JsonbNillable
    private CredentialType credentialType = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public Authentication() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Set AccountName.
     *
     * @param accountName AccountName
     */
    public final void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    /**
     * Retrieves the AccountName.
     *
     * @return AccountName
     */
    public final String getAccountName() {
        return accountName;
    }

    /**
     * Sets the Credential (Password or Passphrase).
     *
     * @param credential Credential (Password or Passphrase)
     */
    public final void setCredential(final byte[] credential) {
        this.credential = Utilities.copy(credential);
    }

    /**
     * Retrieves the Credential (Password or Passphrase).
     *
     * @return Credential (Password or Passphrase)
     */
    public final byte[] getCredential() {
        return Utilities.copy(credential);
    }

    /**
     * Sets the Credential Type.
     *
     * @param credentialType Credential Type
     */
    public final void setCredentialType(final CredentialType credentialType) {
        this.credentialType = credentialType;
    }

    /**
     * Retrieves Credential Type.
     *
     * @return Credential Type
     */
    public final CredentialType getCredentialType() {
        return credentialType;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = new ConcurrentHashMap<>();

        // To properly process an Authenticated Request, then EDS requires
        // information about the type of Credential used. If none is set, then
        // EDS will set one based on the given information, assuming that if
        // the account name is set, then a PassPhrase is used, otherwise a
        // Session is used. If neither is correct, then EDS will simply fail
        // to handle the request properly, with an error information.
        if (credentialType == null) {
            if (accountName != null) {
                credentialType = CredentialType.PASSPHRASE;
            } else {
                credentialType = CredentialType.SESSION;
            }
        }

        // With a defined CredentialType, it is possible to perform additional
        // checks to ensure that the required information is present.
        switch (credentialType) {
            case SESSION:
                checkNotNullOrEmpty(errors, Constants.FIELD_CREDENTIAL, credential, "The Session (Credential) is missing.");
                break;
            case SIGNATURE:
                checkNotNullEmptyOrTooLong(errors, Constants.FIELD_ACCOUNT_NAME, accountName, Constants.MAX_NAME_LENGTH, "AccountName is missing, null or invalid.");
                checkNotNullOrEmpty(errors, Constants.FIELD_CREDENTIAL, credential, "The Credential is missing.");
                break;
            default:
                checkNotNullEmptyOrTooLong(errors, Constants.FIELD_ACCOUNT_NAME, accountName, Constants.MAX_NAME_LENGTH, "AccountName is missing, null or invalid.");
                checkNotNullOrEmpty(errors, Constants.FIELD_CREDENTIAL, credential, "The Credential is missing.");
                credentialType = CredentialType.PASSPHRASE;
                break;
        }

        return errors;
    }
}
