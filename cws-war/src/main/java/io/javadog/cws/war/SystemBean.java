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
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.core.SystemServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Stateless
public class SystemBean implements System {

    private static final Logger log = LoggerFactory.getLogger(SystemBean.class);

    @PersistenceContext(unitName = "cwsDatabase")
    private EntityManager entityManager;

    private final Settings settings = new Settings();
    private SystemServiceFactory factory = null;

    @PostConstruct
    public void init() {
        factory = new SystemServiceFactory(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.NEVER)
    public VersionResponse version() {
        final VersionResponse response = new VersionResponse();
        response.setVersion(Constants.CWS_VERSION);

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.NEVER)
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
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
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
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.NEVER)
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
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
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
        }

        return response;
    }
}
