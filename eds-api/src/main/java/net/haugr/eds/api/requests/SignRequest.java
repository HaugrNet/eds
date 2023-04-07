/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.api.requests;

import net.haugr.eds.api.common.ByteArrayAdapter;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.Utilities;
import java.time.LocalDateTime;
import java.util.Map;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTypeAdapter;

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
 * @since EDS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_DATA, Constants.FIELD_EXPIRES })
public final class SignRequest extends Authentication {

    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Data to Sign. */
    @JsonbProperty(value = Constants.FIELD_DATA, nillable = true)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    private byte[] data = null;

    /** Signature Expiration Date. */
    @JsonbProperty(value = Constants.FIELD_EXPIRES, nillable = true)
    @JsonbDateFormat(Constants.JSON_DATE_FORMAT)
    private LocalDateTime expires = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * Set the Data to Sign.
     *
     * @param data Data to Sign
     */
    public void setData(final byte[] data) {
        this.data = Utilities.copy(data);
    }

    /**
     * Retrieves the Data to Sign.
     *
     * @return Data to Sign
     */
    public byte[] getData() {
        return Utilities.copy(data);
    }

    /**
     * Set the Signature Expiration Date.
     *
     * @param expires Signature Expiration Date
     */
    public void setExpires(final LocalDateTime expires) {
        this.expires = expires;
    }

    /**
     * Retrieves the Signature Expiration Date.
     *
     * @return Signature Expiration Date
     */
    public LocalDateTime getExpires() {
        return expires;
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
