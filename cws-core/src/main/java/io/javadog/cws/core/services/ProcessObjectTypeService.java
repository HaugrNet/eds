package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.ProcessObjectTypeRequest;
import io.javadog.cws.api.responses.ProcessObjectTypeResponse;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.common.exceptions.exceptions.CWSException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessObjectTypeService extends Servicable<ProcessObjectTypeResponse, ProcessObjectTypeRequest> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessObjectTypeResponse process(final ProcessObjectTypeRequest request) {
        verify(request);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
