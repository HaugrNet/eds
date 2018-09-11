/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Utilities;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.Map;

/**
 * <p>This Object is needed, when a new Signature is being issued. The Object
 * requires a mandatory &quot;Document&quot; or simply a data Object (byte
 * array) upon which the Signature is generated. It is also possible to set an
 * expiration data.</p>
 *
 * <p>Please see {@link Authentication} for information about the account and
 * credentials information.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "signRequest")
@XmlType(name = "signRequest", propOrder = { Constants.FIELD_DATA, Constants.FIELD_EXPIRES })
public final class SignRequest extends Authentication {

    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @NotNull
    @XmlElement(name = Constants.FIELD_DATA, required = true)
    private byte[] data = null;

    @XmlElement(name = Constants.FIELD_EXPIRES)
    private Date expires = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setData(final byte[] data) {
        this.data = Utilities.copy(data);
    }

    public byte[] getData() {
        return Utilities.copy(data);
    }

    public void setExpires(final Date expires) {
        this.expires = Utilities.copy(expires);
    }

    public Date getExpires() {
        return Utilities.copy(expires);
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

        checkNotNull(errors, Constants.FIELD_DATA, data, "The Data Object to create a Signature is missing.");

        return errors;
    }
}
