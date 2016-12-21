package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.common.Verifiable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "member", propOrder = { "name", "credentialType", "credentials", "trustLevel", "modified", "since" })
public final class Member extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "credentialType";
    private static final String FIELD_CREDENTIAL = "credentials";
    private static final String FIELD_TRUST = "trustLevel";
    private static final int NAME_MIN_LENGTH = 1;
    private static final int NAME_MAX_LENGTH = 75;

    @XmlElement(required = true) private String name = null;
    @XmlElement(required = true) private CredentialType credentialType = null;
    @XmlElement(required = true) private char[] credentials = null;
    @XmlElement(required = true) private TrustLevel trustLevel = TrustLevel.READ;
    @XmlElement                  private Date modified = null;
    @XmlElement                  private Date since = null;

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
        ensureNotNull(FIELD_CREDENTIAL, credentials);
        this.credentials = credentials;
    }

    public char[] getCredentials() {
        return credentials;
    }

    public void setTrustLevel(final TrustLevel trustLevel) {
        ensureNotNull(FIELD_TRUST, trustLevel);
        this.trustLevel = trustLevel;
    }

    public TrustLevel getTrustLevel() {
        return trustLevel;
    }

    public void setModified(final Date modified) {
        this.modified = modified;
    }

    public Date getModified() {
        return modified;
    }

    public void setSince(final Date since) {
        this.since = since;
    }

    public Date getSince() {
        return since;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        return new HashMap<>();
    }
}
