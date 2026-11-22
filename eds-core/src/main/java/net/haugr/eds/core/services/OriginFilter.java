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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.regex.Pattern;
import net.haugr.eds.core.model.Settings;

/**
 * <p>Cross-Origin Resource Sharing (CORS): lets a page from another
 * address call this stone from a browser, if the stone's setting
 * {@code eds.cors.value} names that address. That is what a Rune needs
 * which shows the status of several stones (STATUS.md): each of them names
 * the address of that Rune.</p>
 *
 * <p>The setting holds the allowed origins, e.g.
 * {@code https://rune.example.com}, separated by commas or blanks;
 * {@code *} allows every origin. A request from an origin which isn't
 * allowed gets no CORS headers, so the browser keeps the answer from the
 * page. The stone's own Rune comes from the stone's address and isn't
 * affected. The header {@code Authorization} is allowed, as the resource
 * API and the status take the Session or a status key in it; cookies
 * aren't used, so credentials aren't allowed.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.2
 */
@Provider
@PreMatching
public final class OriginFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static final String ORIGIN = "Origin";
    static final String REQUEST_METHOD = "Access-Control-Request-Method";
    static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    static final String ALLOW_METHODS = "Access-Control-Allow-Methods";
    static final String ALLOW_HEADERS = "Access-Control-Allow-Headers";
    static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    static final String MAX_AGE = "Access-Control-Max-Age";
    static final String VALUE_METHODS = "GET, HEAD, POST, PUT, DELETE, OPTIONS";
    static final String VALUE_HEADERS = "Content-Type, Authorization, Range, If-Range";
    static final String VALUE_EXPOSED = "Content-Length, Content-Range, Content-Disposition, ETag";
    // The allowed origins of the setting are separated by commas or blanks.
    private static final Pattern SEPARATORS = Pattern.compile("[,\\s]+");

    private final Settings settings;

    /**
     * Default Constructor.
     */
    public OriginFilter() {
        this(Settings.getInstance());
    }

    OriginFilter(final Settings settings) {
        this.settings = settings;
    }

    /**
     * Answers the preflight request of a browser, which asks before the
     * real request whether it may be sent.
     */
    @Override
    public void filter(final ContainerRequestContext request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                && (request.getHeaderString(REQUEST_METHOD) != null)
                && allowed(request.getHeaderString(ORIGIN))) {
            request.abortWith(Response.noContent().build());
        }
    }

    /**
     * Tells the browser that the page of an allowed origin may read the
     * answer.
     */
    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {
        final String origin = request.getHeaderString(ORIGIN);
        if (allowed(origin)) {
            response.getHeaders().putSingle(ALLOW_ORIGIN, origin);
            response.getHeaders().putSingle(ALLOW_METHODS, VALUE_METHODS);
            response.getHeaders().putSingle(ALLOW_HEADERS, VALUE_HEADERS);
            response.getHeaders().putSingle(EXPOSE_HEADERS, VALUE_EXPOSED);
            response.getHeaders().putSingle(MAX_AGE, "600");
            // The answer depends on who asks, which caches must know.
            response.getHeaders().add(HttpHeaders.VARY, ORIGIN);
        }
    }

    /**
     * True if the origin is one the stone's setting allows.
     *
     * @param origin The Origin header of a request, or null
     * @return True if it is allowed
     */
    boolean allowed(final String origin) {
        if ((origin == null) || origin.isBlank()) {
            return false;
        }
        final String setting = settings.getCORS();
        if (setting == null) {
            return false;
        }
        for (final String allowed : SEPARATORS.split(setting)) {
            if ("*".equals(allowed) || allowed.equalsIgnoreCase(origin.trim())) {
                return true;
            }
        }
        return false;
    }
}
