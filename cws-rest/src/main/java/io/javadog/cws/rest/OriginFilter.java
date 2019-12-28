/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
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
package io.javadog.cws.rest;

import io.javadog.cws.core.model.Settings;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Simple Cross-Origin Resource Sharing (CORS) filter.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.2
 * @see <a href="https://enable-cors.org/">enable cors</a>
 */
public final class OriginFilter implements Filter {

    private final Settings settings = Settings.getInstance();

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
        final HttpServletRequest servletRequest = (HttpServletRequest) request;

        servletResponse.addHeader("Access-Control-Allow-Origin", settings.getCORS());
        servletResponse.addHeader("Access-Control-Allow-Methods", "POST");
        servletResponse.addHeader("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(servletRequest.getMethod())) {
            // For OPTIONS reply with ACCEPTED status code, per CORS handshake
            servletResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
        } else {
            // Otherwise, pass the request along the filter chain
            chain.doFilter(request, response);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // Nothing to do here
    }
}
