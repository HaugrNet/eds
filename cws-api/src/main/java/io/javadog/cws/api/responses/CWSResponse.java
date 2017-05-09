/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "authenticationToken", propOrder = { "returnCode", "returnMessage" })
public class CWSResponse implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private ReturnCode returnCode = ReturnCode.SUCCESS;
    private String returnMessage = "Ok";

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public CWSResponse() {
        // Required for WebServices to work. Comment added to please Sonar.
    }

    public CWSResponse(final ReturnCode returnCode, final String returnMessage) {
        this.returnCode = returnCode;
        this.returnMessage = returnMessage;
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setReturnCode(final ReturnCode returnCode) {
        this.returnCode = returnCode;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }

    public void setReturnMessage(final String returnMessage) {
        this.returnMessage = returnMessage;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    public boolean isOk() {
        return returnCode == ReturnCode.SUCCESS;
    }
}
