package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class FetchCircleResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    public FetchCircleResponse() {
    }

    public FetchCircleResponse(final int returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }
}
