/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Verifiable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
@XmlType(name = "member", propOrder = { "id", "authentication",  "added" })
public final class Member extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String FIELD_ID = "id";
    private static final String FIELD_AUTHENTICATION = "authentication";
    private static final String FIELD_ADDED = "added";

    @XmlElement(name = FIELD_ID)
    private String id = null;

    @XmlElement(name = FIELD_AUTHENTICATION, required = true)
    private Authentication authentication = null;

    @XmlElement(name = FIELD_ADDED)
    private Date added = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    public void setId(final String id) {
        ensurePattern(FIELD_ID, id, Constants.ID_PATTERN_REGEX);
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

    public void setAdded(final Date added) {
        this.added = new Date(added.getTime());
    }

    public Date getAdded() {
        return new Date(added.getTime());
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

        checkPattern(errors, FIELD_ID, id, Constants.ID_PATTERN_REGEX, "The Member Id is invalid.");
        checkNotNull(errors, FIELD_AUTHENTICATION, authentication, "The Authentication is missing, null or invalid.");

        return errors;
    }
}
