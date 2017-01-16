package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.FetchObjectTypeRequest;
import io.javadog.cws.api.responses.FetchObjectTypeResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Action;
import io.javadog.cws.core.Servicable;

import javax.persistence.EntityManager;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FetchObjectTypeService extends Servicable<FetchObjectTypeResponse, FetchObjectTypeRequest> {

    public FetchObjectTypeService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchObjectTypeResponse process(final FetchObjectTypeRequest request) {
        verifyAndCheckRequest(request, Action.FETCH_OBJECT_TYPE);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
