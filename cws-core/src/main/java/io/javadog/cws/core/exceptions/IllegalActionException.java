/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2019 JavaDog.io
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
 * <p>CWS Specific Exception for {@link ReturnCode#ILLEGAL_ACTION}.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
public final class IllegalActionException extends CWSException {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /**
     * Business Case Constructor, for simple examples where the logic is not
     * permitting a given action.
     *
     * @param message Error description
     */
    public IllegalActionException(final String message) {
        super(ReturnCode.ILLEGAL_ACTION, message);
    }
}
