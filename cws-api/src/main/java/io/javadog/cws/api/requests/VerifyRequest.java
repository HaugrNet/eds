/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Utilities;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>When a &quot;document&quot; needs to have its signature verified, this
 * Object is needed as part of the CWS verify request. It requires that both
 * the Signature and the &quot;document&quot; (bytes) is present to perform
 * the verification check.</p>
 *
 * <p>Please see {@link Authentication} for information about the account and
 * credentials information.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "verifyRequest")
@XmlType(name = "verifyRequest", propOrder = { Constants.FIELD_SIGNATURE, Constants.FIELD_DATA })
public final class VerifyRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_SIGNATURE, required = true)
    private String signature = null;

    @XmlElement(name = Constants.FIELD_DATA, required = true)
    private byte[] data = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setSignature(final String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }

    public void setData(final byte[] data) {
        this.data = Utilities.copy(data);
    }

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
