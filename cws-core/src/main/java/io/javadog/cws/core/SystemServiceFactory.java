package io.javadog.cws.core;

import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.core.services.FetchCirclesService;
import io.javadog.cws.core.services.FetchMemberService;
import io.javadog.cws.core.services.ProcessCircleService;
import io.javadog.cws.core.services.ProcessMemberService;
import io.javadog.cws.model.CommonDao;
import io.javadog.cws.model.jpa.CommonJpaDao;

import javax.persistence.EntityManager;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SystemServiceFactory {

    private final EntityManager entityManager;

    public SystemServiceFactory(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Servicable<FetchMemberResponse, FetchMemberRequest> prepareFetchMemberService() {
        return new FetchMemberService();
    }

    public Servicable<ProcessMemberResponse, ProcessMemberRequest> prepareProcessMemberService() {
        final CommonDao dao = new CommonJpaDao(entityManager);
        return new ProcessMemberService(dao);
    }

    public Servicable<FetchCircleResponse, FetchCircleRequest> prepareFetchCirclesService() {
        return new FetchCirclesService();
    }

    public Servicable<ProcessCircleResponse, ProcessCircleRequest> prepareProcessCircleService() {
        return new ProcessCircleService();
    }
}
