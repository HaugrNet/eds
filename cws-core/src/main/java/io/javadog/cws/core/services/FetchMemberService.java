package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.common.exceptions.exceptions.CWSException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchMemberService extends Servicable<FetchMemberResponse, FetchMemberRequest> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchMemberResponse process(final FetchMemberRequest request) {
        verify(request);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
