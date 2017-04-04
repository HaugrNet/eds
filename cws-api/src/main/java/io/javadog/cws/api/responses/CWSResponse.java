/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;

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

    private int returnCode = Constants.SUCCESS;
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

    public CWSResponse(final int returnCode, final String returnMessage) {
        this.returnCode = returnCode;
        this.returnMessage = returnMessage;
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setReturnCode(final int returnCode) {
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnMessage(final String returnMessage) {
        this.returnMessage = returnMessage;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    public boolean isOk() {
        return returnCode == 0;
    }
}
