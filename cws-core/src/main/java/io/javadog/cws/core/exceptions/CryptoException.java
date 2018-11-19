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
 * <p>Crypto Exception, thrown if an error occurred from invoking the JCE logic
 * or in the Crypto components.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CryptoException extends CWSException {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /**
     * Business Case Constructor, for simple examples where the logic is not
     * permitting a given action.
     *
     * @param message Error description
     */
    public CryptoException(final String message) {
        super(ReturnCode.CRYPTO_ERROR, message);
    }

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
