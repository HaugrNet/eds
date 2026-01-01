/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
package net.haugr.eds.api.responses;

import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.io.Serializable;

/**
 * <p>General Response Object, embedded in all other Response Objects, as it
 * contains the processing result, i.e., return code &amp; message. If everything
 * went good, the return code will code 200, the same as the HTTP protocol will
 * return if everything went well. The return message will simply be 'Ok' in
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
 * @since EDS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_RETURN_CODE, Constants.FIELD_RETURN_MESSAGE })
public class EDSResponse implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The EDS Return Code. */
    @JsonbProperty(Constants.FIELD_RETURN_CODE)
    private int returnCode = ReturnCode.SUCCESS.getCode();

    /** The EDS Return Message. */
    @JsonbProperty(Constants.FIELD_RETURN_MESSAGE)
    private String returnMessage = "Ok";

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public EDSResponse() {
        // Required for WebServices to work. Comment added to please Sonar.
    }

    /**
     * Constructor for more detailed responses.
     *
     * @param returnMessage The EDS Return Message
     */
    public EDSResponse(final String returnMessage) {
        this.returnMessage = returnMessage;
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public EDSResponse(final ReturnCode returnCode, final String returnMessage) {
        this.returnCode = returnCode.getCode();
        this.returnMessage = returnMessage;
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * Set the Return Code.
     *
     * @param returnCode Return Code
     */
    public final void setReturnCode(final ReturnCode returnCode) {
        this.returnCode = returnCode.getCode();
    }

    /**
     * Retrieves the Return Code.
     *
     * @return Return Code
     */
    public final int getReturnCode() {
        return returnCode;
    }

    /**
     * Set the Return Message.
     *
     * @param returnMessage Return Message
     */
    public final void setReturnMessage(final String returnMessage) {
        this.returnMessage = returnMessage;
    }

    /**
     * Retrieves the Return Message.
     *
     * @return Return Message
     */
    public final String getReturnMessage() {
        return returnMessage;
    }

    /**
     * Returns true if the request completed successfully, otherwise false.
     *
     * @return True if successful, otherwise false
     */
    public final boolean isOk() {
        return returnCode == ReturnCode.SUCCESS.getCode();
    }
}
