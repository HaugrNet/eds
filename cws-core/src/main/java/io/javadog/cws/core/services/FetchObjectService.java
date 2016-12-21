package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.FetchObjectRequest;
import io.javadog.cws.api.responses.FetchObjectResponse;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.common.exceptions.CWSException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchObjectService extends Servicable<FetchObjectResponse, FetchObjectRequest> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchObjectResponse process(final FetchObjectRequest request) {
        verify(request);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
