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
public final class CryptoException extends CWSException {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /**
     * To prevent that too much information about the underlying system is
     * exposed via the error text, the Cause must be separate from the message.
     * Hence, even for trivial problems we're not allowing the Cause to stand
     * alone.
     *
     * @param message Error description
     * @param cause   Cause of the error
     */
    public CryptoException(final String message, final Throwable cause) {
        super(ReturnCode.CRYPTO_ERROR, message, cause);
    }
}
