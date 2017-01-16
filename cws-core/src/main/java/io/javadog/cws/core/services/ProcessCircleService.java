package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Action;
import io.javadog.cws.core.Servicable;

import javax.persistence.EntityManager;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessCircleService extends Servicable<ProcessCircleResponse, ProcessCircleRequest> {

    public ProcessCircleService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessCircleResponse process(final ProcessCircleRequest request) {
        verifyAndCheckRequest(request, Action.PROCESS_CIRCLE);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
