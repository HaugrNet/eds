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

import io.javadog.cws.api.responses.CwsResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * <p>Common REST utilities, for all REST services.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class RestUtils {

    /** REST WebService consummation media type. */
    public static final String CONSUMES = MediaType.APPLICATION_JSON;
    /** REST WebService production media type. */
    public static final String PRODUCES = MediaType.APPLICATION_JSON;

    private RestUtils() {
        // Private Constructor, this is a Utility Class.
    }

    /**
     * <p>This method will build the response to be returned by the REST
     * service. Please note, that rather than just taking the existing
     * returnCode and use it, the method will always return that the request
     * was successful, even it if wasn't.</p>
     *
     * <p>The reason for this, is that it is not possible to set change the
     * error message being returned, and as it contains important information
     * about what went wrong and how it can be fixed - is is critical that it
     * is not lost. Hence, any request coming into CWS, which CWS and not the
     * underlying container is responding too, will be replied with a success,
     * even if the reply failed.</p>
     *
     * @param cwsResponse The CWS Response Object to convert
     * @return The converted REST Response Object
     */
    public static Response buildResponse(final CwsResponse cwsResponse) {
        return Response
                .ok()
                .type(PRODUCES)
                .entity(cwsResponse)
                .build();
    }
}
