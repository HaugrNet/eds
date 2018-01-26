/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api;

import java.nio.charset.Charset;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class TestUtilities {

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private TestUtilities() {
        // Private Constructor, this is a Utility Class
    }

    /**
     * Converts the given String to a UTF-8 encoded Byte Array.
     *
     * @param str String to convert to Byte Array
     * @return UTF-8 encoded Byte array
     */
    public static byte[] convert(final String str) {
        return str.getBytes(CHARSET);
    }

    /**
     * Converts the given Byte Array to a String, assuming it is UTF-8 encoded.
     *
     * @param bytes UTF-8 encoded Byte Array to convert to String
     * @return New String from the UTF-8 encoded Byte Array
     */
    public static String convert(final byte[] bytes) {
        return new String(bytes, CHARSET);
    }
}
