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
import jakarta.ws.rs.ext.Provider;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.core.status.StatusCollector;

/**
 * <p>Counts the requests for the status of the stone (STATUS.md): how
 * many, how many of them failed, how long they took and how much they
 * carried. Nothing about a request itself is kept.</p>
 *
 * <p>A request counts as failed with an HTTP status from 400, or, for the
 * JSON API, which always answers with 200, with a return code which isn't
 * a success. The sizes are those the headers tell; a body without a length
 * isn't counted.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Provider
public final class StatusFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String STARTED = "eds.status.started";

    /**
     * Default Constructor.
     */
    public StatusFilter() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    @Override
    public void filter(final ContainerRequestContext request) {
        request.setProperty(STARTED, System.nanoTime());
    }

    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {
        final Object started = request.getProperty(STARTED);
        final long nanos = (started instanceof Long start) ? (System.nanoTime() - start) : 0;
        final boolean failure = (response.getStatus() >= 400)
                || ((response.getEntity() instanceof EDSResponse answer) && !answer.isOk());
        StatusCollector.getInstance().request(nanos, failure, request.getLength(), response.getLength());
    }
}
