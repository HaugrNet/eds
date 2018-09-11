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
package io.javadog.cws.core.exceptions;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;

/**
 * <p>Authentication Exception, if a problem occurred with identifying a Member
 * in CWS.</p>
 *
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
