package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.Verifiable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.HashMap;
import java.util.Map;

/**
 * Common Object for All requests to the CWS, which contain enough information
 * to properly authenticate a member and thus authorize this member to access
 * the System.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "authenticate", propOrder = { "name", "credentialType", "credentials" })
public class Authenticate extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "credentialType";
    private static final String FIELD_CREDENTIALS = "credentials";
    private static final int NAME_MIN_LENGTH = 1;
    private static final int NAME_MAX_LENGTH = 75;

    @XmlElement(required = true) private String name = null;
    @XmlElement(required = true) private CredentialType credentialType = null;
    @XmlElement(required = true) private char[] credentials = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    @NotNull
    @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
    public void setName(final String name) {
        ensureNotNull(FIELD_NAME, name);
        ensureLength(FIELD_NAME, name, NAME_MIN_LENGTH, NAME_MAX_LENGTH);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @NotNull
    public void setCredentialType(final CredentialType credentialType) {
        ensureNotNull(FIELD_TYPE, credentialType);
        this.credentialType = credentialType;
    }

    public CredentialType getCredentialType() {
        return credentialType;
    }

    @NotNull
    public void setCredentials(final char[] credentials) {
        ensureNotNull(FIELD_CREDENTIALS, credentials);
        this.credentials = credentials;
    }

    public char[] getCredentials() {
        return credentials;
    }

    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = new HashMap<>();
        final String error = "Value is missing, null or invalid.";

        if (name == null) {
            errors.put(FIELD_NAME, error);
        }
        if (credentials == null) {
            errors.put(FIELD_CREDENTIALS, error);
        }
        if (credentialType == null) {
            errors.put(FIELD_TYPE, error);
        }

        return errors;
    }
}
