/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Constants.MAX_NAME_LENGTH;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Base Authentication Object for all incoming Requests. It contains the name
 * of the Account, plus the credentials to unlock the Account. Both the name
 * and credentials is mandatory, whereas the type of credential is optional with
 * a fallback to PassPhrase.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "authentication", propOrder = { "account", "credential", "credentialType" })
public class Authentication extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String FIELD_ACCOUNT = "account";
    private static final String FIELD_CREDENTIAL = "credential";
    private static final String FIELD_TYPE = "credentialType";

    @NotNull
    @Size(min = 1, max = MAX_NAME_LENGTH)
    @XmlElement(name = FIELD_ACCOUNT, required = true)
    private String account = null;

    @NotNull
    @XmlElement(name = FIELD_CREDENTIAL, required = true)
    private String credential = null;

    @NotNull
    @XmlElement(name = FIELD_TYPE, required = true)
    private CredentialType credentialType = CredentialType.PASSPHRASE;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setAccount(final String account) {
        this.account = account;
    }

    public String getAccount() {
        return account;
    }

    public void setCredential(final String credential) {
        this.credential = credential;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredentialType(final CredentialType credentialType) {
        this.credentialType = credentialType;
    }

    public CredentialType getCredentialType() {
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

        checkNotNullEmptyOrTooLong(errors, FIELD_ACCOUNT, account, MAX_NAME_LENGTH, "Account is missing, null or invalid.");
        checkNotNull(errors, FIELD_CREDENTIAL, credential, "The Credential is missing.");
        if (credentialType == null) {
            credentialType = CredentialType.PASSPHRASE;
        }

        return errors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Authentication)) {
            return false;
        }

        final Authentication that = (Authentication) obj;
        return Objects.equals(account, that.account) &&
                Objects.equals(credential, that.credential) &&
                (credentialType == that.credentialType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(account, credential, credentialType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Authentication{" +
                "account='" + account + '\'' +
                ", credential=xxxxxxxx" +
                ", credentialType=" + credentialType +
                '}';
    }
}
