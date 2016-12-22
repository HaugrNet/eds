package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Verifiable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>The Member Object, is used as Accounts in CWS. The Object consists of an
 * Authentication Object, a description, creation and modification dates.</p>
 *
 * <p>The Authentication Object is mandatory, the description is optional, and
 * only serves as a hint for other Members.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "member", propOrder = { "id", "authentication", "description", "modified", "since" })
public final class Member extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String FIELD_ID = "id";
    private static final String FIELD_AUTHENTICATION = "authentication";
    private static final String FIELD_DESCRIPTION = "description";
    private static final int MAX_LENGTH = 75;

    @XmlElement                  private String id = null;
    @XmlElement(required = true) private Authentication authentication = null;
    @XmlElement                  private String description = null;
    @XmlElement                  private Date modified = null;
    @XmlElement                  private Date since = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    @Pattern(regexp = Constants.PATTERN_REGEX)
    public void setId(final String id) {
        ensurePattern(FIELD_ID, id, Constants.PATTERN_REGEX);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @NotNull
    public void setAuthentication(final Authentication authentication) {
        ensureNotNull(FIELD_AUTHENTICATION, authentication);
        this.authentication = authentication;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    @Size(max = MAX_LENGTH)
    public void setDescription(final String description) {
        ensureMaxLength(FIELD_DESCRIPTION, description, MAX_LENGTH);
        this.description = description;
    }

    public String getDescription() {
        return description;
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
        final Map<String, String> errors = new HashMap<>();

        if (authentication == null) {
            errors.put(FIELD_AUTHENTICATION, "Value is missing, null or invalid.");
        } else {
            errors.putAll(authentication.validate());
        }

        return errors;
    }
}
