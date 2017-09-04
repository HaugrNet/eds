/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Utilities.copy;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "signRequest", propOrder = { "data", "expires" })
public final class SignRequest extends Authentication {

    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String FIELD_DATA = "data";
    private static final String FIELD_EXPIRES = "expires";

    @XmlElement(name = FIELD_DATA, required = true)
    private byte[] data = null;

    @XmlElement(name = FIELD_EXPIRES)
    private Date expires = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setData(final byte[] data) {
        ensureNotNull(FIELD_DATA, data);
        this.data = copy(data);
    }

    public byte[] getData() {
        return copy(data);
    }

    public void setExpires(final Date expires) {
        this.expires = copy(expires);
    }

    public Date getExpires() {
        return copy(expires);
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

        checkNotNull(errors, FIELD_DATA, data, "The Data Object to create a Signature is missing.");

        return errors;
    }
}
