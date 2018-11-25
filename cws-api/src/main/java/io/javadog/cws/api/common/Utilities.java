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
package io.javadog.cws.api.common;

import java.time.Instant;
import java.util.Date;

/**
 * <p>Common Utilities, primarily immutability functionality.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Utilities {

    private Utilities() {
        // Private Constructor, this is a Utility Class
    }

    /**
     * Although this method may appear to be silly, it serves the purpose of
     * acting as an intermediate between the old Date Object and the newer
     * Java8 Time functionality. Once an upgrade to JavaEE8+ has been completed,
     * so JPA where support for the Time functionality has been added, it can be
     * removed.
     *
     * @param millisSinceEpoch Milli Seconds since epoch (1970-01-01 00:00:00)
     * @return New Date
     */
    public static Date newDate(final long millisSinceEpoch) {
        return Date.from(Instant.ofEpochMilli(millisSinceEpoch));
    }

    /**
     * Although this method may appear to be silly, it serves the purpose of
     * acting as an intermediate between the old Date Object and the newer
     * Java8 Time functionality. Once an upgrade to JavaEE8+ has been completed,
     * so JPA where support for the Time functionality has been added, it can be
     * removed.
     *
     * @return New Date
     */
    public static Date newDate() {
        return Date.from(Instant.now());
    }

    /**
     * Copy method for standard Date Objects, to have a central way to protect
     * those pesky mutable Objects.
     *
     * @param date Date Object to protect
     * @return Copy of the given Date Object
     */
    public static Date copy(final Date date) {
        return (date != null) ? Date.from(date.toInstant()) : null;
    }

    /**
     * <p>Copy method for Byte Arrays, to have a central way to protect the data
     * structures from external changes.</p>
     *
     * <p>Note, that as the API is only exposed via a WebService (SOAP/REST),
     * there is currently no need for a proper copying.</p>
     *
     * <p>Further, the copy here is fake - meaning that it doesn't do anything.
     * This is important, as some byte arrays must be actively destroyed, which
     * is harder if multiple actual copies exists.</p>
     *
     * @param bytes Byte Array to protect
     * @return Copy of the given Bytes
     */
    public static byte[] copy(final byte[] bytes) {
        return bytes;
    }
}
