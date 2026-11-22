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

import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.jce.DataStreams;
import net.haugr.eds.core.jce.SecretEDSKey;

/**
 * <p>A prepared content download (PLAN.md, S3): everything which is
 * checked before the response starts (authentication, access, Range), and
 * what is needed to stream it. If the preparation failed, only the error
 * response is set.</p>
 *
 * <p>The object holds the unwrapped Circle Secret for the duration of one
 * request, and must not be kept beyond it.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class ContentDownload {

    private final EDSResponse error;
    private final long dataId;
    private final String objectId;
    private final DataFormat.Context context;
    private final DataFormat.Header header;
    private final long storedLength;
    private final long size;
    private final String etag;
    private final ByteRange range;
    private final SecretEDSKey circleSecret;
    private final String mediaType;
    private final String name;
    // While the Circle Secret is rotated (PLAN.md, Crypto C6), the content may
    // be re-encrypted with the new secret between the two transactions of a
    // download; then it is read with this one.
    private Rotated rotated = null;

    /**
     * The new Circle Secret, for content prepared on the retiring one.
     *
     * @param keyId  Key row of the new secret
     * @param secret The new Circle Secret
     */
    public record Rotated(long keyId, SecretEDSKey secret) {
    }

    /**
     * What is stored of the content: the row of the content, the header of
     * the stored format, the length of what is stored, and the ETag.
     *
     * @param dataId       Internal ID of the content
     * @param header       Header of the stored format
     * @param storedLength Length of what is stored, in bytes
     * @param etag         Quoted ETag
     */
    public record Stored(long dataId, DataFormat.Header header, long storedLength, String etag) {
    }

    /**
     * How the Data Object is described to the one who downloads it.
     *
     * @param objectId  External ID of the Data Object
     * @param mediaType Its media type, or null
     * @param name      Its name
     */
    public record Described(String objectId, String mediaType, String name) {
    }

    private static final Stored NOTHING_STORED = new Stored(0, null, 0, null);
    private static final Described NOT_DESCRIBED = new Described(null, null, null);

    private ContentDownload(final EDSResponse error, final Stored stored, final Described described, final DataFormat.Context context,
                            final ByteRange range, final SecretEDSKey circleSecret) {
        this.error = error;
        this.dataId = stored.dataId();
        this.objectId = described.objectId();
        this.context = context;
        this.header = stored.header();
        this.storedLength = stored.storedLength();
        this.size = (header != null) ? DataFormat.plaintextLength(storedLength, header.chunkSize()) : 0;
        this.etag = stored.etag();
        this.range = range;
        this.circleSecret = circleSecret;
        this.mediaType = described.mediaType();
        this.name = described.name();
    }

    public static ContentDownload failed(final EDSResponse error) {
        return new ContentDownload(error, NOTHING_STORED, NOT_DESCRIBED, null, null, null);
    }

    /**
     * A download, which failed with 416: the error, but also the size, as
     * the response must say it (Content-Range: bytes &#42;/size).
     */
    public static ContentDownload unsatisfiable(final EDSResponse error, final DataFormat.Header header, final long storedLength, final String etag) {
        return new ContentDownload(error, new Stored(0, header, storedLength, etag), NOT_DESCRIBED, null, null, null);
    }

    public static ContentDownload prepared(final Stored stored, final Described described, final DataFormat.Context context,
                                           final ByteRange range, final SecretEDSKey circleSecret) {
        return new ContentDownload(null, stored, described, context, range, circleSecret);
    }

    public boolean isOk() {
        return error == null;
    }

    public EDSResponse error() {
        return error;
    }

    public boolean isPartial() {
        return (range != null) && (range.kind() == ByteRange.Kind.PARTIAL);
    }

    /** The HTTP status for a successful download: 200 or 206. */
    public int status() {
        return isPartial() ? 206 : 200;
    }

    /** Size of the full content. */
    public long size() {
        return size;
    }

    /** Number of bytes in the response body. */
    public long contentLength() {
        return isPartial() ? range.length() : size;
    }

    /** The Content-Range header for a 206 or 416 response, else null. */
    public String contentRange() {
        if (!isOk()) {
            return "bytes */" + size;
        }

        return isPartial() ? ("bytes " + range.first() + '-' + range.last() + '/' + size) : null;
    }

    /** The plan for the stored chunks, for a partial or non-empty download. */
    public DataStreams.Range plan() {
        if (size == 0) {
            return null;
        }
        final long first = isPartial() ? range.first() : 0;
        final long last = isPartial() ? range.last() : (size - 1);

        return DataStreams.plan(header, storedLength, first, last);
    }

    public long dataId() {
        return dataId;
    }

    public String objectId() {
        return objectId;
    }

    public DataFormat.Context context() {
        return context;
    }

    public DataFormat.Header header() {
        return header;
    }

    public long storedLength() {
        return storedLength;
    }

    public String etag() {
        return etag;
    }

    public SecretEDSKey circleSecret() {
        return circleSecret;
    }

    /**
     * Adds the new Circle Secret to a download prepared on the retiring one.
     *
     * @param next The new secret
     * @return This download
     */
    public ContentDownload rotating(final Rotated next) {
        this.rotated = next;
        return this;
    }

    /**
     * The secret and the context to read the content with, as the row says
     * it is stored now: the one prepared, or the new one after a rotation.
     *
     * @param storedKeyId The key row of the content, read in the transaction which streams it
     * @return The secret and the context, or null if the content is on neither
     */
    public Reading reading(final long storedKeyId) {
        if (storedKeyId == context.circleSecretVersion()) {
            return new Reading(circleSecret, context);
        }
        return ((rotated != null) && (storedKeyId == rotated.keyId()))
                ? new Reading(rotated.secret(), new DataFormat.Context(context.circleId(), context.objectId(), storedKeyId))
                : null;
    }

    /**
     * How the content is read.
     *
     * @param secret  The Circle Secret
     * @param context The Data Object, with the version of the secret
     */
    public record Reading(SecretEDSKey secret, DataFormat.Context context) {
    }

    public String contentType() {
        return ContentPolicy.contentType(mediaType);
    }

    public String contentDisposition() {
        return ContentPolicy.contentDisposition(mediaType, name);
    }
}
