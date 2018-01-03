/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.exceptions;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;

/**
 * Top Exception Class for CWS.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class CWSException extends RuntimeException {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private final ReturnCode returnCode;

    public CWSException(final ReturnCode returnCode, final String message) {
        super(message);
        this.returnCode = returnCode;
    }

    public CWSException(final ReturnCode returnCode, final Throwable cause) {
        super(cause);
        this.returnCode = returnCode;
    }

    public CWSException(final ReturnCode returnCode, final String message, final Throwable cause) {
        super(message, cause);
        this.returnCode = returnCode;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }
}
