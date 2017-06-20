/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common.exceptions;

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
}