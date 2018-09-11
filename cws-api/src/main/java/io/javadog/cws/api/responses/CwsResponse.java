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
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * <p>General Response Object, embedded in all other Response Objects, as it
 * contains the processing result, i.e. return code &amp; message. If everything
 * went good, the return code will the code 200, same as the HTTP protocol will
 * return if everything went well. The return message will simple be 'Ok' in
 * this case.</p>
 *
 * <p>If a problem occurred, either a warning (problem which can be corrected by
 * the invoking system/member), or an error (internal problem, most likely a
 * resource issue). The return code &amp; message should hopefully provide
 * enough information for the System Administrator to correct the problem.</p>
 *
 * <p>The class {@link ReturnCode} for more information, and clarification of
 * the individual warnings or errors which may occur.</p>
 *
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

    public final void setReturnCode(final ReturnCode returnCode) {
        this.returnCode = returnCode.getCode();
    }

    public final int getReturnCode() {
        return returnCode;
    }

    public final void setReturnMessage(final String returnMessage) {
        this.returnMessage = returnMessage;
    }

    public final String getReturnMessage() {
        return returnMessage;
    }

    public final boolean isOk() {
        return returnCode == ReturnCode.SUCCESS.getCode();
    }
}
