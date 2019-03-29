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
    public static final String CONSUMES = MediaType.APPLICATION_JSON;
    /** REST WebService production media type. */
    public static final String PRODUCES = MediaType.APPLICATION_JSON;

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
