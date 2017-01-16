package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SettingResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = 8868831828030258226L;

    public SettingResponse() {
        // Empty Constructor, required for WebServices
    }

    public SettingResponse(final int returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }
}
