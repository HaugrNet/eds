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
package net.haugr.eds.core.services;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.model.Settings;

/**
 * <p>Counts the body of a request while it is read, and stops reading it at
 * the limit (ticket 25). The stone of Quarkus refuses a body whose length is
 * over the limit from its headers, before any of it is read (BodyLimit, ticket
 * 11); a body which comes without a length, as HTTP/2 allows, can't be
 * measured so, and is counted here instead: once it passes the limit, the
 * reading stops, and the request is answered with 413. So at most the limit is
 * held of it, however long it is.</p>
 *
 * <p>The reader of the JSON wraps what the stream throws (Jackson answers
 * 400), so the refusal is made once the reader gave up. The uploads, which
 * their endpoints read as a stream ({@link InputStream}) and limit themselves,
 * are not counted.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Provider
public final class BodyCounter implements ReaderInterceptor {

    /** The smallest limit, 16 MiB. */
    public static final long MIN_LIMIT = 16L * 1024 * 1024;
    /** Room for the fields around the Base64 of the largest inline content. */
    public static final long ROOM = 1024L * 1024;

    private final Settings settings;

    /**
     * Default Constructor.
     */
    public BodyCounter() {
        this(Settings.getInstance());
    }

    BodyCounter(final Settings settings) {
        this.settings = settings;
    }

    /**
     * The largest body of a request which is not an upload of content: the
     * largest inline content in Base64, with room for the rest, and at least
     * {@link #MIN_LIMIT}.
     *
     * @param settings The settings
     * @return Bytes
     */
    public static long limit(final Settings settings) {
        final long inline = settings.getDataMaxInlineSize();
        // Base64 takes 4 characters for every 3 bytes.
        final long base64 = ((inline + 2) / 3) * 4;
        return Math.max(MIN_LIMIT, base64 + ROOM);
    }

    // The counted stream is the body of the request, which the reader reads
    // and the HTTP layer closes; closing it here would end the request.
    @SuppressWarnings("PMD.CloseResource")
    @Override
    public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException {
        if (InputStream.class.isAssignableFrom(context.getType())) {
            return context.proceed();
        }
        final long limit = limit(settings);
        final Counted counted = new Counted(context.getInputStream(), limit);
        context.setInputStream(counted);
        try {
            return context.proceed();
        } catch (IOException | RuntimeException e) {
            // The reader wraps what the stream throws (Jackson answers 400),
            // so the refusal is made here, once the reader gave up.
            if (counted.count > limit) {
                throw new WebApplicationException(Response.status(ReturnCode.CONTENT_TOO_LARGE.getHttpCode())
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity("{\"returnCode\":" + ReturnCode.CONTENT_TOO_LARGE.getCode()
                                + ",\"returnMessage\":\"The request body is larger than the " + limit + " bytes allowed; content is uploaded with PUT "
                                + Constants.REST_DATA_BASE + Constants.REST_DATA_CONTENT + ".\"}")
                        .build());
            }
            throw e;
        }
    }

    private static final class Counted extends FilterInputStream {

        private final long limit;
        private long count;

        Counted(final InputStream in, final long limit) {
            super(in);
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            final int b = super.read();
            if (b >= 0) {
                counted(1);
            }
            return b;
        }

        @Override
        public int read(final byte[] buffer, final int offset, final int length) throws IOException {
            final int read = super.read(buffer, offset, length);
            if (read > 0) {
                counted(read);
            }
            return read;
        }

        @Override
        public long skip(final long count) throws IOException {
            // What is skipped is read from the client as well.
            final long skipped = super.skip(count);
            if (skipped > 0) {
                counted(skipped);
            }
            return skipped;
        }

        private void counted(final long read) throws IOException {
            count += read;
            if (count > limit) {
                throw new IOException("The request body is larger than " + limit + " bytes.");
            }
        }
    }
}
