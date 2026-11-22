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
package net.haugr.eds.fitnesse;

/**
 * <p>Deterministic generated content for the streaming fixtures: byte i of
 * the content depends only on i, so a download (or a range of it) can be
 * compared without keeping the content.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class GeneratedContent {

    private GeneratedContent() {
        // Utility class.
    }

    static byte valueAt(final long position) {
        return (byte) ((position * 31) + 7);
    }

    static byte[] create(final int size) {
        final byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = valueAt(i);
        }

        return bytes;
    }

    static boolean matches(final byte[] bytes, final long offset) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != valueAt(offset + i)) {
                return false;
            }
        }

        return true;
    }
}
