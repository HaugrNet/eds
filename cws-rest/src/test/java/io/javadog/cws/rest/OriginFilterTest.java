/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.2
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
     * @since CWS 1.2
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
        public String getInitParameter(String name) {
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
     * @since CWS 1.2
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
        public String getParameter(String name) {
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
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeAttribute(final String name) {
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
        @Deprecated
        public String getRealPath(final String path) {
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
    }

    /**
     * @author Kim Jensen
     * @since CWS 1.2
     */
    private static class TestServletResponse implements HttpServletResponse {

        private final Map<String, String> header = new ConcurrentHashMap<>();

        /**
         * {@inheritDoc}
         */
        @Override
        public void addCookie(final Cookie cookie) {

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
        @Deprecated
        public String encodeUrl(final String url) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @Deprecated
        public String encodeRedirectUrl(final String url) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sendError(final int sc, final String msg) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sendError(final int sc) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sendRedirect(final String location) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setDateHeader(final String name, final long date) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addDateHeader(final String name, final long date) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setHeader(String name, String value) {
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
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addIntHeader(final String name, final int value) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setStatus(final int sc) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @Deprecated
        public void setStatus(final int sc, final String sm) {
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
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setContentLength(final int len) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setContentLengthLong(final long len) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setContentType(final String type) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setBufferSize(final int size) {
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
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void resetBuffer() {
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
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setLocale(final Locale loc) {
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
     * @since CWS 1.2
     */
    private static class TestFilterChain implements FilterChain {

        /**
         * {@inheritDoc}
         */
        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response) {
        }
    }
}
