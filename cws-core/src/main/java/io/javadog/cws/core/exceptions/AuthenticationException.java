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
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class AuthenticationException extends CWSException {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    public AuthenticationException(final String message) {
        super(ReturnCode.AUTHENTICATION_WARNING, message);
    }

    public AuthenticationException(final String message, final Throwable cause) {
        super(ReturnCode.AUTHENTICATION_WARNING, message, cause);
    }
}
