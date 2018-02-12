/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.test.exceptions;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class StopTestException extends RuntimeException {

    public StopTestException(final Throwable cause) {
        super(cause);
    }
}
