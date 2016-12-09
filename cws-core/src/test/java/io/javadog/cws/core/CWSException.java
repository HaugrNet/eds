package io.javadog.cws.core;

/**
 * Top Exception Class for CWS.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CWSException extends RuntimeException {

    private static final long serialVersionUID = 6693557870047302588L;

    public CWSException(final Throwable cause) {
        super(cause);
    }
}
