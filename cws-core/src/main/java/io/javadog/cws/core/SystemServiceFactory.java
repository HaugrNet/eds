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

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SystemServiceFactory {

    public Servicable<FetchCircleResponse, FetchCircleRequest> prepareFetchCirclesService() {
        return new FetchCirclesService();
    }

    public Servicable<ProcessCircleResponse, ProcessCircleRequest> prepareProcessCircleService() {
        return new ProcessCircleService();
    }

    public Servicable<FetchMemberResponse, FetchMemberRequest> prepareFetchMemberService() {
        return new FetchMemberService();
    }

    public Servicable<ProcessMemberResponse, ProcessMemberRequest> prepareProcessMemberService() {
        return new ProcessMemberService();
    }
}
