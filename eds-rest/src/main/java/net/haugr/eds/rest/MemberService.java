/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.rest;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.model.Settings;

/**
 * <p>REST interface for the Member functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_MEMBERS_BASE)
public class MemberService {

    private static final String PROCESS_METHOD = "processMember";
    private static final String FETCH_METHOD = "fetchMembers";

    @Inject
    private ManagementBean bean;
    private final Settings settings = Settings.getInstance();

    /**
     * Default Constructor.
     */
    public MemberService() {
        // Empty Constructor
    }

    /**
     * The REST Create Member Endpoint.
     *
     * @param createMemberRequest Create Member Request
     * @return Create Member Response
     */
    @POST
    @Path(Constants.REST_MEMBERS_CREATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response create(@NotNull final ProcessMemberRequest createMemberRequest) {
        createMemberRequest.setAction(Action.CREATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, createMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_CREATE);
    }

    /**
     * The REST Invite Member Endpoint.
     *
     * @param inviteMemberRequest Invite Member Request
     * @return Invite Member Response
     */
    @POST
    @Path(Constants.REST_MEMBERS_INVITE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response invite(@NotNull final ProcessMemberRequest inviteMemberRequest) {
        inviteMemberRequest.setAction(Action.INVITE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, inviteMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_INVITE);
    }

    /**
     * The REST Login Member Endpoint.
     *
     * @param loginMemberRequest Login Member Request
     * @return Login Member Response
     */
    @POST
    @Path(Constants.REST_MEMBERS_LOGIN)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response login(@NotNull final ProcessMemberRequest loginMemberRequest) {
        loginMemberRequest.setAction(Action.LOGIN);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, loginMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_LOGIN);
    }

    /**
     * The REST Logout Member Endpoint.
     *
     * @param logoutMemberRequest Logout Member Request
     * @return Logout Member Response
     */
    @POST
    @Path(Constants.REST_MEMBERS_LOGOUT)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response logout(@NotNull final ProcessMemberRequest logoutMemberRequest) {
        logoutMemberRequest.setAction(Action.LOGOUT);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, logoutMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_LOGOUT);
    }

    /**
     * The REST Alter Member Endpoint.
     *
     * @param alterMemberRequest Alter Member Request
     * @return Alter Member Response
     */
    @POST
    @Path(Constants.REST_MEMBERS_ALTER)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response alter(@NotNull final ProcessMemberRequest alterMemberRequest) {
        alterMemberRequest.setAction(Action.ALTER);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, alterMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_ALTER);
    }

    /**
     * The REST Update Member Endpoint.
     *
     * @param updateMemberRequest Update Member Request
     * @return Update Member Response
     */
    @POST
    @Path(Constants.REST_MEMBERS_UPDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response update(@NotNull final ProcessMemberRequest updateMemberRequest) {
        updateMemberRequest.setAction(Action.UPDATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, updateMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_UPDATE);
    }

    /**
     * The REST Invalidate Member Endpoint.
     *
     * @param invalidateRequest Invalidate Member Request
     * @return Invalidate Member Response
     */
    @POST
    @Path(Constants.REST_MEMBERS_INVALIDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response invalidate(@NotNull final ProcessMemberRequest invalidateRequest) {
        invalidateRequest.setAction(Action.INVALIDATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, invalidateRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_INVALIDATE);
    }

    /**
     * The REST Delete Member Endpoint.
     *
     * @param deleteMemberRequest Delete Member Request
     * @return Delete Member Response
     */
    @POST
    @Path(Constants.REST_MEMBERS_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessMemberRequest deleteMemberRequest) {
        deleteMemberRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, deleteMemberRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_DELETE);
    }

    /**
     * The REST Fetch Members Endpoint.
     *
     * @param fetchMembersRequest Fetch Members Request
     * @return Fetch Members Response
     */
    @POST
    @Path(Constants.REST_MEMBERS_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchMemberRequest fetchMembersRequest) {
        return CommonService.runRequest(settings, bean, FETCH_METHOD, fetchMembersRequest, Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_FETCH);
    }
}
