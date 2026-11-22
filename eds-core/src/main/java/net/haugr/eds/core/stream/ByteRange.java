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
package net.haugr.eds.core.stream;

import java.util.Locale;

/**
 * <p>Parses an HTTP {@code Range} header (RFC 9110, section 14) for a single
 * byte range: {@code bytes=a-b}, {@code bytes=a-} and the suffix range
 * {@code bytes=-n}.</p>
 *
 * <p>Multiple ranges, other units and invalid syntax are ignored, meaning
 * the full content is served (allowed by RFC 9110). A syntactically valid
 * range outside the content is unsatisfiable (416).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class ByteRange {

    /** The outcome of parsing a Range header. */
    public enum Kind {
        /** No usable Range: serve the full content. */
        FULL,
        /** A satisfiable range. */
        PARTIAL,
        /** A valid range, but outside the content. */
        UNSATISFIABLE
    }

    private static final String PREFIX = "bytes=";

    private final Kind kind;
    private final long first;
    private final long last;

    private ByteRange(final Kind kind, final long first, final long last) {
        this.kind = kind;
        this.first = first;
        this.last = last;
    }

    /**
     * Parses the Range header for content of the given size.
     *
     * @param header The Range header, or null
     * @param size   The size of the content
     * @return The parsed range
     */
    public static ByteRange parse(final String header, final long size) {
        if (header == null) {
            return full(size);
        }
        final String value = header.trim().toLowerCase(Locale.ROOT);
        if (!value.startsWith(PREFIX) || value.contains(",")) {
            return full(size);
        }

        final String spec = value.substring(PREFIX.length()).trim();
        final int dash = spec.indexOf('-');
        if (dash < 0) {
            return full(size);
        }

        try {
            final String start = spec.substring(0, dash).trim();
            final String end = spec.substring(dash + 1).trim();

            return start.isEmpty() ? suffix(Long.parseLong(end), size) : fromTo(start, end, size);
        } catch (NumberFormatException _) {
            return full(size);
        }
    }

    /** A suffix range: the last n bytes. */
    private static ByteRange suffix(final long suffix, final long size) {
        return ((suffix <= 0) || (size == 0))
                ? unsatisfiable()
                : new ByteRange(Kind.PARTIAL, Math.max(0, size - suffix), size - 1);
    }

    /** A range from a first byte, to a last one or to the end. */
    private static ByteRange fromTo(final String start, final String end, final long size) {
        final long from = Long.parseLong(start);
        final long to = end.isEmpty() ? Long.MAX_VALUE : Long.parseLong(end);
        if ((from < 0) || (to < from)) {
            // Invalid, ignored (RFC 9110, 14.1.1).
            return full(size);
        }
        if (from >= size) {
            return unsatisfiable();
        }

        return new ByteRange(Kind.PARTIAL, from, Math.min(to, size - 1));
    }

    private static ByteRange full(final long size) {
        return new ByteRange(Kind.FULL, 0, size - 1);
    }

    private static ByteRange unsatisfiable() {
        return new ByteRange(Kind.UNSATISFIABLE, -1, -1);
    }

    public Kind kind() {
        return kind;
    }

    /** First byte, inclusive. */
    public long first() {
        return first;
    }

    /** Last byte, inclusive; -1 for empty content. */
    public long last() {
        return last;
    }

    public long length() {
        return (kind == Kind.UNSATISFIABLE) ? 0 : (last - first + 1);
    }
}
