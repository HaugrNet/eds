/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
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

import net.haugr.cws.api.common.ByteArrayAdapter;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.CredentialType;
import net.haugr.cws.api.common.Utilities;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTypeAdapter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Base Authentication Object for all incoming Requests. It contains the
 * name of the Account, plus the credentials to unlock the Account. Both the
 * name and credentials are mandatory, whereas the type of credential is
 * optional with a fallback to PassPhrase.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_ACCOUNT_NAME,
        Constants.FIELD_CREDENTIAL,
        Constants.FIELD_CREDENTIALTYPE })
public class Authentication extends AbstractRequest {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @JsonbProperty(value = Constants.FIELD_ACCOUNT_NAME, nillable = true)
    private String accountName = null;

    @JsonbProperty(value = Constants.FIELD_CREDENTIAL, nillable = true)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    private byte[] credential = null;

    @JsonbProperty(value = Constants.FIELD_CREDENTIALTYPE, nillable = true)
    private CredentialType credentialType = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public final void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public final String getAccountName() {
        return accountName;
    }

    public final void setCredential(final byte[] credential) {
        this.credential = Utilities.copy(credential);
    }

    public final byte[] getCredential() {
        return Utilities.copy(credential);
    }

    public final void setCredentialType(final CredentialType credentialType) {
        this.credentialType = credentialType;
    }

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

        // To properly process an Authenticated Request, then CWS requires
        // information about the type of Credential used. If none is set, then
        // CWS will set one based on the given information, assuming that if
        // the account name is set, then a PassPhrase is used, otherwise a
        // Session is used. If neither is correct, then CWS will simply fail
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
