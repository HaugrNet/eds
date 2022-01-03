/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.core.misc;

import java.util.Locale;

/**
 * <p>Common Logging util.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
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
