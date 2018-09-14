/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.core.ManagementBean;
import io.javadog.cws.core.misc.LoggingUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * <p>REST interface for the Member functionality.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Path(Constants.REST_MEMBERS_BASE)
public class MemberService {

    private static final Logger LOG = Logger.getLogger(MemberService.class.getName());

    private final Settings settings = Settings.getInstance();
    @Inject private ManagementBean bean;

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_MEMBERS_CREATE)
    public Response create(@NotNull final ProcessMemberRequest createMemberRequest) {
        return processMember(createMemberRequest, Action.CREATE, Constants.REST_MEMBERS_CREATE);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_MEMBERS_INVITE)
    public Response invite(@NotNull final ProcessMemberRequest inviteMemberRequest) {
        return processMember(inviteMemberRequest, Action.INVITE, Constants.REST_MEMBERS_INVITE);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_MEMBERS_UPDATE)
    public Response update(@NotNull final ProcessMemberRequest updateMemberRequest) {
        return processMember(updateMemberRequest, Action.UPDATE, Constants.REST_MEMBERS_UPDATE);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_MEMBERS_INVALIDATE)
    public Response invalidate(@NotNull final ProcessMemberRequest invalidateRequest) {
        return processMember(invalidateRequest, Action.INVALIDATE, Constants.REST_MEMBERS_INVALIDATE);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_MEMBERS_DELETE)
    public Response delete(@NotNull final ProcessMemberRequest deleteMemberRequest) {
        return processMember(deleteMemberRequest, Action.DELETE, Constants.REST_MEMBERS_DELETE);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_MEMBERS_FETCH)
    public Response fetch(@NotNull final FetchMemberRequest fetchMembersRequest) {
        final String restAction = Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_FETCH;
        final long startTime = System.nanoTime();
        FetchMemberResponse response;

        try {
            response = bean.fetchMembers(fetchMembersRequest);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOG.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new FetchMemberResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }

    private Response processMember(final ProcessMemberRequest request, final Action action, final String logAction) {
        final String restAction = Constants.REST_MEMBERS_BASE + logAction;
        final long startTime = System.nanoTime();
        ProcessMemberResponse response;

        try {
            request.setAction(action);
            response = bean.processMember(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOG.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new ProcessMemberResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
