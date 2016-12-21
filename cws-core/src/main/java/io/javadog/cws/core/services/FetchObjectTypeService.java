package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.FetchObjectTypeRequest;
import io.javadog.cws.api.responses.FetchObjectTypeResponse;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.common.exceptions.CWSException;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FetchObjectTypeService extends Servicable<FetchObjectTypeResponse, FetchObjectTypeRequest> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchObjectTypeResponse process(final FetchObjectTypeRequest request) {
        verify(request);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
