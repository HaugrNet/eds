/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.misc;

import java.util.Formatter;
import java.util.Locale;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class StringUtil {

    private StringUtil() {
        // Private Constructor, this is a utility Class.
    }

    public static String durationSince(final String action, final long startNanoTime) {
        final long elapsedNS = System.nanoTime() - startNanoTime;

        try (Formatter formatter = new Formatter(Locale.US)) {
            final double elaspedMS = elapsedNS / 1000000.0D;
            return formatter.format("%s completed in %(,.2f ms", action, elaspedMS).toString();
        }
    }
}
