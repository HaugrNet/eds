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

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * <p>Rules for serving untrusted content (PLAN.md, Streaming, "Serving user
 * content safely"). Only a small allowlist of media types is shown inline;
 * everything else, SVG and HTML included, is served as
 * {@code application/octet-stream} for download. The media type comes from
 * the Data Object's metadata, never from the content.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class ContentPolicy {

    /** Media types which may be displayed inline. */
    public static final Set<String> INLINE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "video/mp4", "video/webm");
    public static final String OCTET_STREAM = "application/octet-stream";
    /** CSP of content responses: content opened directly runs no scripts and has no origin. */
    public static final String CONTENT_SECURITY_POLICY = "default-src 'none'; img-src 'self'; media-src 'self'; style-src 'unsafe-inline'; sandbox";
    /** Media types of thumbnails (PLAN.md, L5): still images shown inline. */
    public static final Set<String> THUMBNAIL_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private ContentPolicy() {
        // Utility class.
    }

    public static boolean isInline(final String mediaType) {
        return (mediaType != null) && INLINE_TYPES.contains(mediaType.toLowerCase(Locale.ROOT));
    }

    /**
     * The Content-Type to serve the content with.
     *
     * @param mediaType Media type from the metadata, or null
     * @return Content-Type
     */
    public static String contentType(final String mediaType) {
        return isInline(mediaType) ? mediaType.toLowerCase(Locale.ROOT) : OCTET_STREAM;
    }

    /**
     * The Content-Disposition, with the object name as file name (RFC 6266,
     * with an ASCII fallback and the UTF-8 name).
     *
     * @param mediaType Media type from the metadata, or null
     * @param name      Name of the Data Object
     * @return Content-Disposition
     */
    public static String contentDisposition(final String mediaType, final String name) {
        final String type = isInline(mediaType) ? "inline" : "attachment";
        final String fallback = asciiName(name);

        return type + "; filename=\"" + fallback + "\"; filename*=UTF-8''" + percentEncode(name);
    }

    private static String asciiName(final String name) {
        final StringBuilder builder = new StringBuilder(name.length());
        for (final char c : name.toCharArray()) {
            builder.append(((c >= 0x20) && (c < 0x7F) && (c != '"') && (c != '\\')) ? c : '_');
        }

        return builder.toString();
    }

    private static String percentEncode(final String name) {
        final StringBuilder builder = new StringBuilder();
        for (final byte b : name.getBytes(StandardCharsets.UTF_8)) {
            final int c = b & 0xFF;
            if (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) || ((c >= '0') && (c <= '9')) || ("!#$&+-.^_`|~".indexOf(c) >= 0)) {
                builder.append((char) c);
            } else {
                builder.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16))).append(Character.toUpperCase(Character.forDigit(c & 0xF, 16)));
            }
        }

        return builder.toString();
    }
}
