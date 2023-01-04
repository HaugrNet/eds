/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.rest;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import net.haugr.cws.api.common.Action;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.requests.FetchMemberRequest;
import net.haugr.cws.api.requests.ProcessMemberRequest;
import net.haugr.cws.core.ManagementBean;
import net.haugr.cws.core.model.Settings;

/**
 * <p>REST interface for the Member functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Path(Constants.REST_MEMBERS_BASE)
public class MemberService {

    private static final String PROCESS_METHOD = "processMember";
    private static final String FETCH_METHOD = "fetchMembers";

    @Inject
    private ManagementBean bean;
    private final Settings settings = Settings.getInstance();

    @POST
    @Path(Constants.REST_MEMBERS_CREATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response create(@NotNull final ProcessMemberRequest createMemberRequest) {
        createMemberRequest.setAction(Action.CREATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, createMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_CREATE);
    }

    @POST
    @Path(Constants.REST_MEMBERS_INVITE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response invite(@NotNull final ProcessMemberRequest inviteMemberRequest) {
        inviteMemberRequest.setAction(Action.INVITE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, inviteMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_INVITE);
    }

    @POST
    @Path(Constants.REST_MEMBERS_LOGIN)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response login(@NotNull final ProcessMemberRequest updateMemberRequest) {
        updateMemberRequest.setAction(Action.LOGIN);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, updateMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_LOGIN);
    }

    @POST
    @Path(Constants.REST_MEMBERS_LOGOUT)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response logout(@NotNull final ProcessMemberRequest updateMemberRequest) {
        updateMemberRequest.setAction(Action.LOGOUT);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, updateMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_LOGOUT);
    }

    @POST
    @Path(Constants.REST_MEMBERS_ALTER)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response alter(@NotNull final ProcessMemberRequest updateMemberRequest) {
        updateMemberRequest.setAction(Action.ALTER);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, updateMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_ALTER);
    }

    @POST
    @Path(Constants.REST_MEMBERS_UPDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response update(@NotNull final ProcessMemberRequest updateMemberRequest) {
        updateMemberRequest.setAction(Action.UPDATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, updateMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_UPDATE);
    }

    @POST
    @Path(Constants.REST_MEMBERS_INVALIDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response invalidate(@NotNull final ProcessMemberRequest invalidateRequest) {
        invalidateRequest.setAction(Action.INVALIDATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, invalidateRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_INVALIDATE);
    }

    @POST
    @Path(Constants.REST_MEMBERS_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessMemberRequest deleteMemberRequest) {
        deleteMemberRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, deleteMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_DELETE);
    }

    @POST
    @Path(Constants.REST_MEMBERS_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchMemberRequest fetchMembersRequest) {
        return CommonService.runRequest(settings, bean, FETCH_METHOD, fetchMembersRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_FETCH);
    }
}
