package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.ProcessObjectRequest;
import io.javadog.cws.api.responses.ProcessObjectResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Action;
import io.javadog.cws.core.Servicable;

import javax.persistence.EntityManager;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessObjectService extends Servicable<ProcessObjectResponse, ProcessObjectRequest> {

    public ProcessObjectService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessObjectResponse process(final ProcessObjectRequest request) {
        verifyRequest(request, Action.PROCESS_OBJECT);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
