/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cwsResult", propOrder = { Constants.FIELD_RETURN_CODE, Constants.FIELD_RETURN_MESSAGE })
public class CwsResponse implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_RETURN_CODE, required = true)
    private int returnCode = ReturnCode.SUCCESS.getCode();

    @XmlElement(name = Constants.FIELD_RETURN_MESSAGE, required = true)
    private String returnMessage = "Ok";

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public CwsResponse() {
        // Required for WebServices to work. Comment added to please Sonar.
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The CWS Return Code
     * @param returnMessage The CWS Return Message
     */
    public CwsResponse(final ReturnCode returnCode, final String returnMessage) {
        this.returnCode = returnCode.getCode();
        this.returnMessage = returnMessage;
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setReturnCode(final ReturnCode returnCode) {
        this.returnCode = returnCode.getCode();
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
        return returnCode == ReturnCode.SUCCESS.getCode();
    }
}
