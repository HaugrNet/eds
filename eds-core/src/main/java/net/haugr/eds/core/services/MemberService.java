/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
package net.haugr.eds.core.services;

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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * <p>REST interface for the Member functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_MEMBERS_BASE)
@Tag(name = "Members", description = "Operations for managing member accounts (create, invite, login, logout, update, delete).")
public class MemberService {

    private final ManagementBean managementBean;
    private final Settings settings;

    public MemberService() {
        this(null);
    }

    @Inject
    public MemberService(final ManagementBean managementBean) {
        this.managementBean = managementBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST Create Member Endpoint.
     *
     * @param createMemberRequest Create Member Request
     * @return Create Member Response
     */
    @Operation(
            summary = "Create member",
            description = "Creates a new member account. Requires System Administrator privileges. The account name must be unique (1-75 characters) and credentials must be provided.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_MEMBERS_CREATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response create(@NotNull final ProcessMemberRequest createMemberRequest) {
        createMemberRequest.setAction(Action.CREATE);
        return CommonService.runRequest(settings, () -> managementBean.processMember(createMemberRequest), Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_CREATE);
    }

    /**
     * The REST Invite Member Endpoint.
     *
     * @param inviteMemberRequest Invite Member Request
     * @return Invite Member Response
     */
    @Operation(
            summary = "Invite member",
            description = "Invites a new member by generating an invitation signature. Requires System Administrator privileges. The invited member can later set their own credentials.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_MEMBERS_INVITE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response invite(@NotNull final ProcessMemberRequest inviteMemberRequest) {
        inviteMemberRequest.setAction(Action.INVITE);
        return CommonService.runRequest(settings, () -> managementBean.processMember(inviteMemberRequest), Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_INVITE);
    }

    /**
     * The REST Login Member Endpoint.
     *
     * @param loginMemberRequest Login Member Request
     * @return Login Member Response
     */
    @Operation(
            summary = "Login member",
            description = "Authenticates a member and creates a new session. Returns a session token for subsequent authenticated requests.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_MEMBERS_LOGIN)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response login(@NotNull final ProcessMemberRequest loginMemberRequest) {
        loginMemberRequest.setAction(Action.LOGIN);
        return CommonService.runRequest(settings, () -> managementBean.processMember(loginMemberRequest), Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_LOGIN);
    }

    /**
     * The REST Logout Member Endpoint.
     *
     * @param logoutMemberRequest Logout Member Request
     * @return Logout Member Response
     */
    @Operation(
            summary = "Logout member",
            description = "Terminates the current session for the authenticated member.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_MEMBERS_LOGOUT)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response logout(@NotNull final ProcessMemberRequest logoutMemberRequest) {
        logoutMemberRequest.setAction(Action.LOGOUT);
        return CommonService.runRequest(settings, () -> managementBean.processMember(logoutMemberRequest), Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_LOGOUT);
    }

    /**
     * The REST Alter Member Endpoint.
     *
     * @param alterMemberRequest Alter Member Request
     * @return Alter Member Response
     */
    @Operation(
            summary = "Alter member",
            description = "Alters a member's role. Requires System Administrator privileges and a valid member ID and role.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_MEMBERS_ALTER)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response alter(@NotNull final ProcessMemberRequest alterMemberRequest) {
        alterMemberRequest.setAction(Action.ALTER);
        return CommonService.runRequest(settings, () -> managementBean.processMember(alterMemberRequest), Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_ALTER);
    }

    /**
     * The REST Update Member Endpoint.
     *
     * @param updateMemberRequest Update Member Request
     * @return Update Member Response
     */
    @Operation(
            summary = "Update member",
            description = "Updates the authenticated member's account name and/or credentials. The System Administrator cannot change their own account name.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_MEMBERS_UPDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response update(@NotNull final ProcessMemberRequest updateMemberRequest) {
        updateMemberRequest.setAction(Action.UPDATE);
        return CommonService.runRequest(settings, () -> managementBean.processMember(updateMemberRequest), Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_UPDATE);
    }

    /**
     * The REST Invalidate Member Endpoint.
     *
     * @param invalidateRequest Invalidate Member Request
     * @return Invalidate Member Response
     */
    @Operation(
            summary = "Invalidate member",
            description = "Invalidates the member's asymmetric key used for Circle access. The account appears functional but data access will fail until Circle Administrators restore access.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_MEMBERS_INVALIDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response invalidate(@NotNull final ProcessMemberRequest invalidateRequest) {
        invalidateRequest.setAction(Action.INVALIDATE);
        return CommonService.runRequest(settings, () -> managementBean.processMember(invalidateRequest), Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_INVALIDATE);
    }

    /**
     * The REST Delete Member Endpoint.
     *
     * @param deleteMemberRequest Delete Member Request
     * @return Delete Member Response
     */
    @Operation(
            summary = "Delete member",
            description = "Deletes a member account. System Administrator can delete other members by ID; regular members can delete their own account. This action is irreversible.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_MEMBERS_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessMemberRequest deleteMemberRequest) {
        deleteMemberRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, () -> managementBean.processMember(deleteMemberRequest), Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_DELETE);
    }

    /**
     * The REST Fetch Members Endpoint.
     *
     * @param fetchMembersRequest Fetch Members Request
     * @return Fetch Members Response
     */
    @Operation(
            summary = "Fetch members",
            description = "Fetches member accounts with optional filtering and pagination support.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_MEMBERS_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchMemberRequest fetchMembersRequest) {
        return CommonService.runRequest(settings, () -> managementBean.fetchMembers(fetchMembersRequest), Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_FETCH);
    }
}
