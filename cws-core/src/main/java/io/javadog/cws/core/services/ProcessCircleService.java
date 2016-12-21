package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.common.exceptions.exceptions.CWSException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessCircleService extends Servicable<ProcessCircleResponse, ProcessCircleRequest> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessCircleResponse process(final ProcessCircleRequest request) {
        verify(request);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
