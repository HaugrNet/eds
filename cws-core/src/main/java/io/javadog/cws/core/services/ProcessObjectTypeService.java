package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.ProcessObjectTypeRequest;
import io.javadog.cws.api.responses.ProcessObjectTypeResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Action;
import io.javadog.cws.core.Servicable;

import javax.persistence.EntityManager;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessObjectTypeService extends Servicable<ProcessObjectTypeResponse, ProcessObjectTypeRequest> {

    public ProcessObjectTypeService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessObjectTypeResponse process(final ProcessObjectTypeRequest request) {
        verifyRequest(request, Action.PROCESS_OBJECT_TYPE);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
