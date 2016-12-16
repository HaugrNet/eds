package io.javadog.cws.war;

import io.javadog.cws.api.System;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.core.SystemServiceFactory;
import io.javadog.cws.core.exceptions.CWSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SystemSOAPService implements System {

    private static final Logger log = LoggerFactory.getLogger(SystemSOAPService.class);

    private static final String GENERAL_RETURN_MESSAGE = "An unknown error occurred. Please consult the CWS System Log.";

    private SystemServiceFactory factory = null;

    @PostConstruct
    public void init() {
        factory = new SystemServiceFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionResponse version() {
        final VersionResponse response = new VersionResponse();
        response.setVersion(Constants.CWS_VERSION);

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        FetchCircleResponse response;

        try {
            final Servicable<FetchCircleResponse, FetchCircleRequest> service = factory.prepareFetchCirclesService();
            response = service.process(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchCircleResponse(e.getReturnCode(), e.getMessage());
        } catch (RuntimeException e) {
            // If an error occurs at this level, which has not been caught
            // earlier, then it is either a programming error or a problem with
            // the container itself. Please consult the log file for more
            // information.
            log.error(e.getMessage(), e);
            response = new FetchCircleResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        ProcessCircleResponse response;

        try {
            final Servicable<ProcessCircleResponse, ProcessCircleRequest> service = factory.prepareProcessCircleService();
            response = service.process(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new ProcessCircleResponse(e.getReturnCode(), e.getMessage());
        } catch (RuntimeException e) {
            // If an error occurs at this level, which has not been caught
            // earlier, then it is either a programming error or a problem with
            // the container itself. Please consult the log file for more
            // information.
            log.error(e.getMessage(), e);
            response = new ProcessCircleResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        FetchMemberResponse response;

        try {
            final Servicable<FetchMemberResponse, FetchMemberRequest> service = factory.prepareFetchMemberService();
            response = service.process(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchMemberResponse(e.getReturnCode(), e.getMessage());
        } catch (RuntimeException e) {
            // If an error occurs at this level, which has not been caught
            // earlier, then it is either a programming error or a problem with
            // the container itself. Please consult the log file for more
            // information.
            log.error(e.getMessage(), e);
            response = new FetchMemberResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        ProcessMemberResponse response;

        try {
            final Servicable<ProcessMemberResponse, ProcessMemberRequest> service = factory.prepareProcessMemberService();
            response = service.process(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new ProcessMemberResponse(e.getReturnCode(), e.getMessage());
        } catch (RuntimeException e) {
            // If an error occurs at this level, which has not been caught
            // earlier, then it is either a programming error or a problem with
            // the container itself. Please consult the log file for more
            // information.
            log.error(e.getMessage(), e);
            response = new ProcessMemberResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }
}
