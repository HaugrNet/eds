/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>This is the response Object from the CWS, when the verify request was
 * invoked. It only contains a single boolean value, verified, which will tell
 * if the Object was correctly verified or not. If the request failed, or there
 * was a problem, then the default value false is set. It is therefore important
 * to check that the request was successful before assuming that the signature
 * check failed.</p>
 *
 * <p>Please see {@link CwsResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "verifyResult")
@XmlType(name = "verifyResult", propOrder = Constants.FIELD_VERIFIED)
public final class VerifyResponse extends CwsResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_VERIFIED, required = true)
    private boolean verified = false;

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public VerifyResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Constructor for more detailed responses.
     *
     * @param returnMessage The CWS Return Message
     */
    public VerifyResponse(final String returnMessage) {
        super(returnMessage);
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The CWS Return Code
     * @param returnMessage The CWS Return Message
     */
    public VerifyResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setVerified(final boolean verified) {
        this.verified = verified;
    }

    public boolean isVerified() {
        return verified;
    }
}
