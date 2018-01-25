/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.common;

import java.nio.charset.Charset;
import java.util.Date;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Utilities {

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private Utilities() {
        // Private Constructor, this is a Utility Class
    }

    /**
     * Copy method for standard Date Objects, to have a central way to protect
     * those pesky mutable Objects.
     *
     * @param date Date Object to protect
     * @return Copy of the given Date Object
     */
    public static Date copy(final Date date) {
        return (date != null) ? new Date(date.getTime()) : null;
    }

    /**
     * <p>Copy method for Byte Arrays, to have a central way to protect the data
     * structures from external changes.</p>
     *
     * <p>Note, that as the API is only exposed via a WebService (SOAP/REST),
     * there is currently no need for a proper copying.</p>
     *
     * @param bytes Byte Array to protect
     * @return Copy of the given Bytes
     */
    public static byte[] copy(final byte[] bytes) {
        return bytes;
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
