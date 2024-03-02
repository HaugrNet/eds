/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.rest;

import net.haugr.eds.core.model.Settings;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Simple Cross-Origin Resource Sharing (CORS) filter.</p>
 *
 * @author Kim Jensen
 * @see <a href="https://enable-cors.org/">enable cors</a>
 * @since EDS 1.2
 */
public final class OriginFilter implements Filter {

    private static final String KEY_ORIGIN = "Access-Control-Allow-Origin";
    private static final String KEY_METHODS = "Access-Control-Allow-Methods";
    private static final String KEY_HEADERS = "Access-Control-Allow-Headers";
    private static final String VALUE_METHODS = "GET, OPTIONS, POST";
    private static final String VALUE_HEADERS = "Content-Type, " + KEY_ORIGIN;
    private final Settings settings = Settings.getInstance();

    /**
     * Default Constructor.
     */
    public OriginFilter() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(FilterConfig filterConfig) {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final HttpServletResponse servletResponse = (HttpServletResponse) response;

        servletResponse.addHeader(KEY_ORIGIN, settings.getCORS());
        servletResponse.addHeader(KEY_METHODS, VALUE_METHODS);
        servletResponse.addHeader(KEY_HEADERS, VALUE_HEADERS);
        chain.doFilter(request, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // Nothing to do here
    }
}
