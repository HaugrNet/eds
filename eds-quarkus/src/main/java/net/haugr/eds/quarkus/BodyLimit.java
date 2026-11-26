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
package net.haugr.eds.quarkus;

import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.regex.Pattern;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.services.BodyCounter;

/**
 * <p>Limits the body of every request but the two uploads of content, before
 * any of it is read (ticket 11). The HTTP layer has one limit for all routes,
 * {@code quarkus.http.limits.max-body-size}, which the content upload needs
 * to be above {@code eds.stream.max.object.size} (1100M). So every other
 * request, JSON which is parsed before anybody is authenticated, could carry
 * as much: four anonymous requests of 300 MB at once took a stone with a heap
 * of 512 MB down (2026-11-23).</p>
 *
 * <p>The limit is {@link BodyCounter#limit(Settings)}: the largest content
 * the JSON {@code data} field may hold, {@code eds.data.max.inline.size}, in
 * Base64 and with room for the rest of the request, and at least 16 MiB. A
 * body over it is answered with 413. A body without a length can't be
 * measured before it is read: chunked, in HTTP/1.1, is answered with 411; a
 * body of HTTP/2 without {@code content-length} passes here, as the headers
 * can't tell it from no body at all, and is counted while it is read
 * ({@link BodyCounter}, ticket 25).
 * Rune, Tolk and the generated clients always send the length.</p>
 *
 * <p>The uploads of content and of thumbnails ({@code PUT .../content},
 * {@code PUT .../thumbnail}) are streamed and limited by their endpoints.
 * They are told by the normalized path, the one requests are routed by, so
 * {@code /data/./content} is no upload (the review of 2026-11-23).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class BodyLimit {

    // Runs before the routes, so before any of the body is read.
    private static final int PRIORITY = 1000;
    // A length of up to 18 digits always fits a long.
    private static final Pattern LENGTH = Pattern.compile("\\d{1,18}");
    private static final String LENGTH_REQUIRED = "A request body needs a Content-Length.";
    // The stream of a refused request is ended, as its body is never read.
    private static final long CANCEL = 8;
    private static final Pattern UPLOAD = Pattern.compile(
            Pattern.quote(Constants.REST_API + Constants.REST_DATA_BASE) + "/[^/]+/(content|thumbnail)/?");

    private final Settings settings;

    /**
     * Default Constructor.
     */
    public BodyLimit() {
        this(Settings.getInstance());
    }

    BodyLimit(final Settings settings) {
        this.settings = settings;
    }

    /**
     * Puts the filter in front of all routes.
     *
     * @param filters The filters of the HTTP layer
     */
    void register(@Observes final Filters filters) {
        filters.register(this::check, PRIORITY);
    }

    /**
     * The largest body of a request which is not an upload of content.
     *
     * @return Bytes
     */
    long limit() {
        return BodyCounter.limit(settings);
    }

    void check(final RoutingContext context) {
        final HttpServerRequest request = context.request();
        if (isUpload(request.method(), context.normalizedPath())) {
            context.next();
            return;
        }

        final String length = request.getHeader(HttpHeaders.CONTENT_LENGTH);
        if (length == null) {
            // In HTTP/2 a body needs no length, and a request without a body
            // has none either (the generated Java client sends POST and PUT
            // so); the headers can't tell the two apart, so HTTP/2 passes,
            // and its body is counted while it is read (BodyCounter, ticket
            // 25).
            if (request.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
                refuse(context, 411, ReturnCode.VERIFICATION_WARNING, LENGTH_REQUIRED);
            } else {
                context.next();
            }
            return;
        }

        final long limit = limit();
        if (tooLarge(length, limit)) {
            refuse(context, ReturnCode.CONTENT_TOO_LARGE.getHttpCode(), ReturnCode.CONTENT_TOO_LARGE,
                    "The request body is larger than the " + limit + " bytes allowed; content is uploaded with PUT "
                            + Constants.REST_DATA_BASE + Constants.REST_DATA_CONTENT + '.');
        } else {
            context.next();
        }
    }

    static boolean isUpload(final HttpMethod method, final String path) {
        return (method == HttpMethod.PUT) && (path != null) && UPLOAD.matcher(path).matches();
    }

    /**
     * True if the length is over the limit, or isn't a length at all; the
     * latter the HTTP layer refuses anyway.
     */
    static boolean tooLarge(final String length, final long limit) {
        final String digits = length.trim();
        return !LENGTH.matcher(digits).matches() || (Long.parseLong(digits) > limit);
    }

    private static void refuse(final RoutingContext context, final int status, final ReturnCode returnCode, final String message) {
        // The body is never read. In HTTP/1.1 the connection can't be used
        // again, so it is closed once the answer is out, also while the
        // client still sends; in HTTP/2 only the stream is ended, as the
        // connection carries other requests too.
        final boolean http2 = context.request().version() == HttpVersion.HTTP_2;
        final HttpServerResponse response = context.response()
                .setStatusCode(status)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        if (!http2) {
            response.putHeader(HttpHeaders.CONNECTION, "close");
        }
        response.end("{\"returnCode\":" + returnCode.getCode() + ",\"returnMessage\":\"" + message + "\"}")
                .onComplete(sent -> {
                    if (http2) {
                        response.reset(CANCEL);
                    } else {
                        context.request().connection().close();
                    }
                });
    }
}
