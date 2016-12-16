package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.core.exceptions.CWSException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberService extends Servicable<ProcessMemberResponse, ProcessMemberRequest> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse process(final ProcessMemberRequest request) {
        verify(request);

        throw new CWSException(Constants.NOTIMPLEMENTED_ERROR, "Not Yet Implemented.");
    }
}
