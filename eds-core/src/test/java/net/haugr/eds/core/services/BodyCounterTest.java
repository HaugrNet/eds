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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * A body is counted while it is read, and the reading stops at the limit
 * (ticket 25); the stone of Quarkus tests it over HTTP/2 (EdsBodyLimitTest).
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class BodyCounterTest extends DatabaseSetup {

    /** What the reader of the body does with the stream it is given. */
    @FunctionalInterface
    private interface Reader {
        Object read(InputStream in) throws IOException;
    }

    @Test
    void testABodyOfTheLimitIsReadWhole() throws IOException {
        final Settings settings = newSettings();
        final long limit = BodyCounter.limit(settings);
        try (Source source = new Source(limit)) {
            final Object read = new BodyCounter(settings).aroundReadFrom(context(String.class, source, BodyCounterTest::drain));

            assertEquals(limit, read);
            assertEquals(limit, source.given);
        }
    }

    @Test
    void testTheReadingStopsAtTheLimit() throws IOException {
        final Settings settings = newSettings();
        final long limit = BodyCounter.limit(settings);
        // As Jackson, the reader wraps what the stream throws.
        final Reader wrapping = in -> {
            try {
                return drain(in);
            } catch (IOException e) {
                throw new IllegalStateException("unreadable", e);
            }
        };

        try (Source source = new Source(10 * limit)) {
            final BodyCounter counter = new BodyCounter(settings);
            final ReaderInterceptorContext context = context(String.class, source, wrapping);
            final WebApplicationException refused = assertThrows(WebApplicationException.class, () -> counter.aroundReadFrom(context));

            assertEquals(ReturnCode.CONTENT_TOO_LARGE.getHttpCode(), refused.getResponse().getStatus());
            assertTrue(((String) refused.getResponse().getEntity()).contains("\"returnCode\":" + ReturnCode.CONTENT_TOO_LARGE.getCode()));
            // No more than one buffer past the limit is taken from the client.
            assertTrue(source.given <= limit + Source.BUFFER, String.valueOf(source.given));
        }
    }

    @Test
    void testTheReadersOwnFailureIsKept() throws IOException {
        final Settings settings = newSettings();
        final IOException broken = new IOException("broken JSON");

        try (Source source = new Source(10)) {
            final BodyCounter counter = new BodyCounter(settings);
            final ReaderInterceptorContext context = context(String.class, source, in -> {
                in.read();
                throw broken;
            });
            final IOException thrown = assertThrows(IOException.class, () -> counter.aroundReadFrom(context));

            assertSame(broken, thrown);
        }
    }

    @Test
    void testAByteAtATimeIsCountedToo() throws IOException {
        final Settings settings = newSettings();
        final long limit = BodyCounter.limit(settings);
        final Reader bytes = in -> {
            long count = 0;
            while (in.read() >= 0) {
                count++;
            }
            return count;
        };

        try (Source source = new Source(limit + 1)) {
            final BodyCounter counter = new BodyCounter(settings);
            final ReaderInterceptorContext context = context(String.class, source, bytes);
            assertThrows(WebApplicationException.class, () -> counter.aroundReadFrom(context));
        }
    }

    @Test
    void testWhatIsSkippedIsCountedToo() throws IOException {
        final Settings settings = newSettings();
        final long limit = BodyCounter.limit(settings);
        final Reader skipping = in -> in.skip(limit + 1);

        try (Source source = new Source(limit + 1)) {
            final BodyCounter counter = new BodyCounter(settings);
            final ReaderInterceptorContext context = context(String.class, source, skipping);
            assertThrows(WebApplicationException.class, () -> counter.aroundReadFrom(context));
        }
    }

    @Test
    void testAnUploadIsNotCounted() throws IOException {
        final Settings settings = newSettings();
        try (Source source = new Source(1)) {
            final Object read = new BodyCounter(settings).aroundReadFrom(context(InputStream.class, source, in -> in));

            assertSame(source, read);
        }
    }

    @Test
    void testTheLimitFollowsTheInlineContent() {
        final Settings settings = newSettings();
        assertEquals(BodyCounter.MIN_LIMIT, BodyCounter.limit(settings));

        settings.set(StandardSetting.DATA_MAX_INLINE_SIZE.getKey(), String.valueOf(30L * 1024 * 1024));
        assertEquals(40L * 1024 * 1024 + BodyCounter.ROOM, BodyCounter.limit(settings));
    }

    private static long drain(final InputStream in) throws IOException {
        final byte[] buffer = new byte[Source.BUFFER];
        long count = 0;
        for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
            count += read;
        }
        return count;
    }

    /**
     * A context whose proceed hands the reader the stream as it then is.
     */
    private static ReaderInterceptorContext context(final Class<?> type, final InputStream body, final Reader reader) {
        final InputStream[] stream = { body };
        return (ReaderInterceptorContext) Proxy.newProxyInstance(BodyCounterTest.class.getClassLoader(),
                new Class<?>[] { ReaderInterceptorContext.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "getType" -> type;
                    case "getInputStream" -> stream[0];
                    case "setInputStream" -> {
                        stream[0] = (InputStream) args[0];
                        yield null;
                    }
                    case "proceed" -> reader.read(stream[0]);
                    default -> throw new InvocationTargetException(new UnsupportedOperationException(method.getName()));
                });
    }

    /**
     * A body of the given length, which counts what it gives.
     */
    private static final class Source extends InputStream {

        static final int BUFFER = 8192;
        private final long length;
        long given;

        Source(final long length) {
            this.length = length;
        }

        @Override
        public int read() {
            if (given >= length) {
                return -1;
            }
            given++;
            return 'A';
        }

        @Override
        public int read(final byte[] buffer, final int offset, final int count) {
            if (given >= length) {
                return -1;
            }
            final int read = (int) Math.min(count, length - given);
            given += read;
            return read;
        }
    }
}
