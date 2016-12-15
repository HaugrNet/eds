package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.ProcessObjectRequest;
import io.javadog.cws.api.responses.ProcessObjectResponse;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.core.exceptions.CWSException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessObjectService extends Servicable<ProcessObjectResponse, ProcessObjectRequest> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessObjectResponse process(final ProcessObjectRequest request) {
        verify(request);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
