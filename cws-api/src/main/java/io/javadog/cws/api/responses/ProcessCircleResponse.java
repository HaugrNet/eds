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

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessCircleResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    public ProcessCircleResponse() {
        // Empty Constructor, required for WebServices
    }

    public ProcessCircleResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }
}
