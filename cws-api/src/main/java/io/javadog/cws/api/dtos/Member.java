package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.TrustWorthiness;
import io.javadog.cws.api.common.Verify;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "member", propOrder = { "name", "credentialType", "credentials", "trustWorthiness" })
public final class Member extends Verify {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "credentialType";
    private static final String FIELD_CREDENTIAL = "credentials";
    private static final String FIELD_TRUST = "trustWorthiness";
    private static final int NAME_MIN_LENGTH = 1;
    private static final int NAME_MAX_LENGTH = 75;

    @XmlElement(required = true) private String name = null;
    @XmlElement(required = true) private CredentialType credentialType = null;
    @XmlElement(required = true) private char[] credentials = null;
    @XmlElement(required = true) private TrustWorthiness trustWorthiness = TrustWorthiness.READ;
    private Date modified = null;
    private Date added = null;

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public Member() {
        // Required for WebServices to work. Comment added to please Sonar.
    }

    /**
     * Default Constructor, for setting all the fields in this Class.
     *
     * @param name            Name of the Member
     * @param credentialType  Type of Credentials being used
     * @param credentials     Credentials used
     * @param trustWorthiness Member Trust level
     */
    public Member(final String name, final CredentialType credentialType, final char[] credentials, final TrustWorthiness trustWorthiness) {
        setName(name);
        setCredentialType(credentialType);
        setCredentials(credentials);
        setTrustWorthiness(trustWorthiness);
    }

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

    public void setTrustWorthiness(final TrustWorthiness trustWorthiness) {
        ensureNotNull(FIELD_TRUST, trustWorthiness);
        this.trustWorthiness = trustWorthiness;
    }

    public TrustWorthiness getTrustWorthiness() {
        return trustWorthiness;
    }
}
