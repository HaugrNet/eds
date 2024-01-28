/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTypeAdapter;
import java.util.Map;

/**
 * <p>When a &quot;document&quot; needs to have its signature verified, this
 * Object is needed as part of the EDS verify request. It requires that both
 * the Signature and the &quot;document&quot; (bytes) is present to perform
 * the verification check.</p>
 *
 * <p>Please see {@link Authentication} for information about the account and
 * credentials information.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_SIGNATURE, Constants.FIELD_DATA })
public final class VerifyRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Signature to verify. */
    @JsonbProperty(value = Constants.FIELD_SIGNATURE, nillable = true)
    private String signature = null;

    /** The Data to Verify. */
    @JsonbProperty(value = Constants.FIELD_DATA, nillable = true)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    private byte[] data = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * Set the Signature to verify.
     *
     * @param signature Signature to verify
     */
    public void setSignature(final String signature) {
        this.signature = signature;
    }

    /**
     * Retrieves the Signature to verify.
     *
     * @return Signature to verify
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Set the Data to Verify against the Signature.
     *
     * @param data Data to Verify
     */
    public void setData(final byte[] data) {
        this.data = Utilities.copy(data);
    }

    /**
     * Retrieves the Data to Verify against the Signature.
     *
     * @return Data to Verify
     */
    public byte[] getData() {
        return Utilities.copy(data);
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

        checkNotNullOrEmpty(errors, Constants.FIELD_SIGNATURE, signature, "The Signature is missing.");
        checkNotNull(errors, Constants.FIELD_DATA, data, "The Data Object to check the Signature against is missing.");

        return errors;
    }
}
