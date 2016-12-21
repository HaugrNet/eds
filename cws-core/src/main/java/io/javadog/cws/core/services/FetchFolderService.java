package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.FetchFolderRequest;
import io.javadog.cws.api.responses.FetchFolderResponse;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.common.exceptions.exceptions.CWSException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchFolderService extends Servicable<FetchFolderResponse, FetchFolderRequest> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchFolderResponse process(final FetchFolderRequest request) {
        verify(request);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
