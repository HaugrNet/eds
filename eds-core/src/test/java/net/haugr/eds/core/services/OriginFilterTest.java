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

import jakarta.servlet.ServletConnection;
import java.nio.charset.Charset;
import java.util.function.Supplier;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.enums.StandardSetting;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.2
 */
final class OriginFilterTest extends DatabaseSetup {

    @Test
    void testFilter() throws IOException, ServletException {
        final OriginFilter filter = new OriginFilter();
        final FilterConfig filterConfig = new TestFilterConfig();

        // The init & destroy methods are unused, this is just to make sure
        // that we don't have any null pointer exceptions thrown.
        filter.init(filterConfig);
        filter.destroy();

        final ServletRequest request = new TestServletRequest();
        final HttpServletResponse response = new TestServletResponse();
        final FilterChain chain = new TestFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(3, response.getHeaderNames().size());
        assertEquals(StandardSetting.CORS.getValue(), response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("GET, OPTIONS, POST", response.getHeader("Access-Control-Allow-Methods"));
        assertEquals("Content-Type, Access-Control-Allow-Origin", response.getHeader("Access-Control-Allow-Headers"));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    /**
     * @author Kim Jensen
     * @since EDS 1.2
     */
    private static class TestFilterConfig implements FilterConfig {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getFilterName() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ServletContext getServletContext() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getInitParameter(final String name) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Enumeration<String> getInitParameterNames() {
            return null;
        }
    }

    /**
     * @author Kim Jensen
     * @since EDS 1.2
     */
    private static class TestServletRequest implements ServletRequest {

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getAttribute(final String name) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Enumeration<String> getAttributeNames() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getCharacterEncoding() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setCharacterEncoding(final String env) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getContentLength() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getContentLengthLong() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getContentType() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ServletInputStream getInputStream() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getParameter(final String name) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Enumeration<String> getParameterNames() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] getParameterValues(final String name) {
            return new String[0];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, String[]> getParameterMap() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getProtocol() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getScheme() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getServerName() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getServerPort() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BufferedReader getReader() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getRemoteAddr() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getRemoteHost() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setAttribute(final String name, final Object o) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeAttribute(final String name) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Locale getLocale() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Enumeration<Locale> getLocales() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSecure() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RequestDispatcher getRequestDispatcher(final String path) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRemotePort() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getLocalName() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getLocalAddr() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getLocalPort() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ServletContext getServletContext() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) throws IllegalStateException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AsyncContext getAsyncContext() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DispatcherType getDispatcherType() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getRequestId() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getProtocolRequestId() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ServletConnection getServletConnection() {
            return null;
        }
    }

    /**
     * @author Kim Jensen
     * @since EDS 1.2
     */
    private static class TestServletResponse implements HttpServletResponse {

        private final Map<String, String> header = new ConcurrentHashMap<>();

        /**
         * {@inheritDoc}
         */
        @Override
        public void addCookie(final Cookie cookie) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean containsHeader(final String name) {
            return header.containsKey(name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String encodeURL(final String url) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String encodeRedirectURL(final String url) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sendError(final int sc, final String msg) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sendError(final int sc) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sendRedirect(final String location) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sendRedirect(final String location, final boolean clearBuffer) throws IOException {
            HttpServletResponse.super.sendRedirect(location, clearBuffer);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sendRedirect(final String location, final int sc) throws IOException {
            HttpServletResponse.super.sendRedirect(location, sc);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sendRedirect(final String s, final int i, final boolean b) throws IOException {
            // Intentionally not implemented, not needed for the testing
            throw new IOException("Not implemented.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setDateHeader(final String name, final long date) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addDateHeader(final String name, final long date) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setHeader(final String name, final String value) {
            addHeader(name, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addHeader(final String name, final String value) {
            header.put(name, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setIntHeader(final String name, final int value) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addIntHeader(final String name, final int value) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setStatus(final int sc) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getStatus() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getHeader(final String name) {
            return header.get(name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<String> getHeaders(final String name) {
            return header.values();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<String> getHeaderNames() {
            return header.keySet();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setTrailerFields(final Supplier<Map<String, String>> supplier) {
            HttpServletResponse.super.setTrailerFields(supplier);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Supplier<Map<String, String>> getTrailerFields() {
            return HttpServletResponse.super.getTrailerFields();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getCharacterEncoding() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getContentType() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ServletOutputStream getOutputStream() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public PrintWriter getWriter() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setCharacterEncoding(final String charset) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setCharacterEncoding(final Charset encoding) {
            HttpServletResponse.super.setCharacterEncoding(encoding);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setContentLength(final int len) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setContentLengthLong(final long len) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setContentType(final String type) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setBufferSize(final int size) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getBufferSize() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flushBuffer() {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void resetBuffer() {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCommitted() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void reset() {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setLocale(final Locale loc) {
            // Intentionally not implemented, not needed for the testing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Locale getLocale() {
            return null;
        }
    }

    /**
     * @author Kim Jensen
     * @since EDS 1.2
     */
    private static class TestFilterChain implements FilterChain {

        /**
         * {@inheritDoc}
         */
        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response) {
            // Intentionally not implemented, not needed for the testing
        }
    }
}
