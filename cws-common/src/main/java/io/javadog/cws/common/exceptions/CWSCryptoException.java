package io.javadog.cws.common.exceptions;

import io.javadog.cws.api.common.Constants;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CWSCryptoException extends CWSException {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    public CWSCryptoException(final Throwable cause) {
        super(Constants.CRYPTO_ERROR, cause);
    }
}
