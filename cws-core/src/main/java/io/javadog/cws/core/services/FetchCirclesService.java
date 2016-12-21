package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.common.exceptions.CWSException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchCirclesService extends Servicable<FetchCircleResponse, FetchCircleRequest> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse process(final FetchCircleRequest request) {
        verify(request);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
