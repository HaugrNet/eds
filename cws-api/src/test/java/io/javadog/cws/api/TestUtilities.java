/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
 * mailto: cws AT JavaDog DOT io
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
package io.javadog.cws.api;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class TestUtilities {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

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
        return (bytes != null) ? new String(bytes, CHARSET) : null;
    }
}
