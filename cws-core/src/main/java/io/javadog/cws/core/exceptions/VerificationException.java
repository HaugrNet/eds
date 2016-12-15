package io.javadog.cws.core.exceptions;

import io.javadog.cws.api.common.Constants;

/**
 * Verification Exception, thrown if the Request Object provided was either null
 * or invalid, meaning that the request cannot be correctly processed.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class VerificationException extends CWSException {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    public VerificationException(final String message) {
        super(Constants.VERIFICATION_WARNING, message);
    }
}
