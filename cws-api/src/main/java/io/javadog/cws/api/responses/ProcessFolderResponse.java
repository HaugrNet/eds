package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessFolderResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = 8868831828030258226L;

    public ProcessFolderResponse() {
        // Empty Constructor, required for WebServices
    }

    public ProcessFolderResponse(final int returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }
}