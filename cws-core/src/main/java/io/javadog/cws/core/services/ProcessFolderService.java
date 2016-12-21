package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.ProcessFolderRequest;
import io.javadog.cws.api.responses.ProcessFolderResponse;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.common.exceptions.CWSException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessFolderService extends Servicable<ProcessFolderResponse, ProcessFolderRequest> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessFolderResponse process(final ProcessFolderRequest request) {
        verify(request);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
