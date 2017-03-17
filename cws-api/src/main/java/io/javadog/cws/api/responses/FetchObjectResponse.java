/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchObjectResponse extends CWSResponse{

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = 8868831828030258226L;

    public FetchObjectResponse() {
        // Empty Constructor, required for WebServices
    }

    public FetchObjectResponse(final int returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }
}
