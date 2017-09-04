/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static io.javadog.cws.api.common.Utilities.copy;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Verifiable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "signature", propOrder = { "id", "expires", "verifications", "created", "lastVerification" })
public final class Signature extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_ID = "id";
    private static final String FIELD_EXPIRES = "expires";
    private static final String FIELD_VERIFICATIONS = "verifications";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_LAST_VERIFICATION = "lastVerification";

    @XmlElement(name = FIELD_ID)
    private String id = null;

    @XmlElement(name = FIELD_EXPIRES)
    private Date expires = null;

    @XmlElement(name = FIELD_VERIFICATIONS)
    private Long verifications = null;

    @XmlElement(name = FIELD_CREATED)
    private Date created = null;

    @XmlElement(name = FIELD_LAST_VERIFICATION)
    private Date lastVerification = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setId(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setExpires(final Date expires) {
        this.expires = copy(expires);
    }

    public Date getExpires() {
        return copy(expires);
    }

    public void setVerifications(final Long verifications) {
        this.verifications = verifications;
    }

    public Long getVerifications() {
        return verifications;
    }

    public void setCreated(final Date created) {
        this.created = copy(created);
    }

    public Date getCreated() {
        return copy(created);
    }

    public void setLastVerification(final Date lastVerification) {
        this.lastVerification = copy(lastVerification);
    }

    public Date getLastVerification() {
        return copy(lastVerification);
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        return new ConcurrentHashMap<>();
    }
}
