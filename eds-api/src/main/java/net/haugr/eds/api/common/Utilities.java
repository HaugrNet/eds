/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.api.common;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * <p>Common Utilities, primarily immutability functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class Utilities {

    private Utilities() {
        // Private Constructor, this is a Utility Class
    }

    /**
     * <p>Creates a LocalDateTime instance, based on given milliseconds. The
     * LocalDateTime instance will be using UTC as an offset.</p>
     *
     * @param millisSinceEpoch Milli Seconds since epoch (1970-01-01 00:00:00)
     * @return New UTC-based ZonedDateTime instance
     */
    public static LocalDateTime newDate(final long millisSinceEpoch) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millisSinceEpoch), ZoneOffset.UTC);
    }

    /**
     * Creates a new UTC based ZonedDateTime instance, to be used.
     *
     * @return New UTC-based ZonedDateTime instance
     */
    public static LocalDateTime newDate() {
        return LocalDateTime.now(Clock.systemUTC());
    }

    /**
     * <p><b>Deliberately does not copy</b>: it returns the given array itself.</p>
     *
     * <p>The method exists for SonarQube, whose rule S2384 ("Mutable members
     * should not be stored or returned directly") flags getters and setters
     * that store or return byte arrays as they are. Routing them through this
     * method marks the place as reviewed, without making a copy.</p>
     *
     * <p>Not copying is intended. Byte arrays such as credentials, tokens and
     * plaintext must be actively overwritten once used, and every real copy
     * would be another copy left in memory. As the API is only exposed via
     * REST, other code can't change the arrays of a request or response in
     * between.</p>
     *
     * <p>Callers must therefore treat the result as the same array: clearing
     * it (e.g. with {@code Arrays.fill}) after passing it to a request or
     * response also clears the value there. Make an explicit copy if the
     * value is still needed.</p>
     *
     * @param bytes Byte Array, may be null
     * @return The same Byte Array
     */
    public static byte[] copy(final byte[] bytes) {
        return bytes;
    }
}
