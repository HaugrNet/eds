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

import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.HttpHeaders;
import net.haugr.eds.api.responses.EDSResponse;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.ws.rs.core.MediaType;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
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
            description = "Creates a Member account with a passphrase (System Administrator only; only the first System Administrator "
                    + "may give it the role ADMIN). The System Administrator is not created by anyone: a stone has no account to begin "
                    + "with, and whoever first comes to it becomes its System Administrator, under the account name and with the "
                    + "passphrase given. No name is set for it.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessMemberResponse.class)))
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
            description = "Creates an invitation for a new Member, who then sets their own passphrase. An invitation always makes "
                    + "a user (STANDARD): a request with the role ADMIN is refused, as a link which falls into the wrong hands must "
                    + "not give more. An administrator is made by creating the account, or by changing the role afterwards.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessMemberResponse.class)))
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
            description = "Starts a session with a session key chosen by the client, so later requests don't need the passphrase.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessMemberResponse.class)))
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
            description = "Ends the requesting Member's session.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessMemberResponse.class)))
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
            description = "Changes another Member's role. Only the first System Administrator may, and its own role can't be changed.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessMemberResponse.class)))
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
            description = "Updates the requesting Member's own account: name, passphrase and/or external public key. Also accepts invitations.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessMemberResponse.class)))
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
            description = "Invalidates the requesting Member's own credentials, so the account can no longer be used.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessMemberResponse.class)))
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
            description = "Deletes the requesting Member's own account, or (System Administrator only) another account.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessMemberResponse.class)))
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
            description = "Returns all Members, or one Member with the Circles the requester may see.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FetchMemberResponse.class)))
    @POST
    @Path(Constants.REST_MEMBERS_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchMemberRequest fetchMembersRequest) {
        return CommonService.runRequest(settings, () -> managementBean.fetchMembers(fetchMembersRequest), Constants.REST_MEMBERS_BASE + Constants.REST_MEMBERS_FETCH);
    }

    // =========================================================================
    // ID-based resource API (PLAN.md, L1)
    // =========================================================================

    /**
     * List members: {@code GET /members} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @return FetchMemberResponse
     */
    @Operation(operationId = "getMembers", summary = "List members", description = "Returns the Members the requesting Member may see; as fetchMembers.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FetchMemberResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getMembers(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization) {
        final FetchMemberRequest request = new FetchMemberRequest();
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::fetchMembers, "GET /members");
    }

    /**
     * Get member: {@code GET /members/{memberId}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param memberId ID of the Member.
     * @return FetchMemberResponse
     */
    @Operation(operationId = "getMemberById", summary = "Get member", description = "Returns one Member and the Circles it shares with the requesting Member; as fetchMembers with a memberId.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FetchMemberResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Path("/{memberId}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getMemberById(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Member.", required = true) @PathParam("memberId") final String memberId) {
        final FetchMemberRequest request = new FetchMemberRequest();
        request.setMemberId(memberId);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::fetchMembers, "GET /members/{memberId}");
    }

    /**
     * Create member: {@code POST /members} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param body          The request body
     * @return ProcessMemberResponse
     */
    @Operation(operationId = "postMembers", summary = "Create member", description = "Creates a new Member; as createMember. "
            + "Any administrator may create a user (STANDARD); only the first System Administrator may create an administrator (ADMIN).")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessMemberResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @POST
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response postMembers(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            final ProcessMemberRequest body) {
        final ProcessMemberRequest request = (body != null) ? body : new ProcessMemberRequest();
        request.setAction(Action.CREATE);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::processMember, "POST /members");
    }

    /**
     * Alter member role: {@code PUT /members/{memberId}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param memberId ID of the Member.
     * @param body          The request body
     * @return ProcessMemberResponse
     */
    @Operation(operationId = "putMemberById", summary = "Alter member role",
            description = "Changes the role of a Member; as alterMember. Only the first System Administrator may, and its own role can't be changed.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessMemberResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path("/{memberId}")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putMemberById(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Member.", required = true) @PathParam("memberId") final String memberId,
            final ProcessMemberRequest body) {
        final ProcessMemberRequest request = (body != null) ? body : new ProcessMemberRequest();
        request.setAction(Action.ALTER);
        request.setMemberId(memberId);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::processMember, "PUT /members/{memberId}");
    }

    /**
     * Delete member: {@code DELETE /members/{memberId}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param memberId ID of the Member.
     * @return ProcessMemberResponse
     */
    @Operation(operationId = "deleteMemberById", summary = "Delete member", description = "Deletes a Member; as deleteMember with a memberId.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessMemberResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @DELETE
    @Path("/{memberId}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response deleteMemberById(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Member.", required = true) @PathParam("memberId") final String memberId) {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAction(Action.DELETE);
        request.setMemberId(memberId);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::processMember, "DELETE /members/{memberId}");
    }
}
