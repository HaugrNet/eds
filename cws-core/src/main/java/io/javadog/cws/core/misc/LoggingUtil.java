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
package io.javadog.cws.core.misc;

import java.util.Locale;

/**
 * <p>Common Logging util.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class LoggingUtil {

    private LoggingUtil() {
        // Private Constructor, this is a utility Class.
    }

    /**
     * Returns a simple String with the duration in milliseconds which the given
     * action took to complete. The start time is given in NanoSeconds, using
     * the {@link System#nanoTime()} method.
     *
     * @param locale        The Locale used for generating the formatted string
     * @param action        The Requested action that was performed
     * @param startNanoTime The start time in ns via {@link System#nanoTime()}
     * @return Formatted String for the logging, contain action and duration
     */
    public static String requestDuration(final Locale locale, final String action, final long startNanoTime) {
        final double elapsed = (System.nanoTime() - startNanoTime) / 1000000.0D;

        return String.format(locale, "%s completed in %.2f ms", action, elapsed);
    }

    /**
     * Returns a simple String with the duration in milliseconds which the given
     * action took to complete. The start time is given in NanoSeconds, using
     * the {@link System#nanoTime()} method.
     *
     * @param locale        The Locale used for generating the formatted string
     * @param action        The Requested action that was performed
     * @param startNanoTime The start time in ns via {@link System#nanoTime()}
     * @param cause         Exception causing problems
     * @return Formatted String for the logging, contain action and duration
     */
    public static String requestDuration(final Locale locale, final String action, final long startNanoTime, final Throwable cause) {
        final double elapsed = (System.nanoTime() - startNanoTime) / 1000000.0D;

        return String.format(locale, "%s completed in %.2f ms with error: %s", action, elapsed, cause.getMessage());
    }
}
