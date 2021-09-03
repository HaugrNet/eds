/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
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
package net.haugr.cws.api.common;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * <p>Common Utilities, primarily immutability functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class Utilities {

    private Utilities() {
        // Private Constructor, this is a Utility Class
    }

    /**
     *
     * @param millisSinceEpoch Milli Seconds since epoch (1970-01-01 00:00:00)
     * @return New UTC based ZonedDateTime instance
     */
    public static LocalDateTime newDate(final long millisSinceEpoch) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millisSinceEpoch), ZoneOffset.UTC);
    }

    /**
     * Creates a new UTC based ZonedDateTime instance, to be used.
     *
     * @return New UTC based ZonedDateTime instance
     */
    public static LocalDateTime newDate() {
        return LocalDateTime.now(Clock.systemUTC());
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
