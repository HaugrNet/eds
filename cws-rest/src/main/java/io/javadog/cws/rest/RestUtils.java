/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.responses.CwsResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * <p>Common REST utilities, for all REST services.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class RestUtils {

    /** REST WebService consummation media type. */
    public static final String CONSUMES = MediaType.APPLICATION_XML;
    /** REST WebService production media type. */
    public static final String PRODUCES = MediaType.APPLICATION_XML;

    private RestUtils() {
        // Private Constructor, this is a Utility Class.
    }

    public static Response buildResponse(final CwsResponse cwsResponse) {
        return Response
                .status(ReturnCode.findReturnCode(cwsResponse.getReturnCode()).getHttpCode())
                .type(PRODUCES)
                .entity(cwsResponse)
                .build();
    }
}
