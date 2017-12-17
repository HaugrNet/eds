/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.core.SystemBean;
import io.javadog.cws.core.misc.StringUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Path("/members")
@Consumes(MediaType.APPLICATION_JSON)
public class MemberService {

    private static final Logger log = Logger.getLogger(MemberService.class.getName());

    @Inject private SystemBean bean;

    @POST
    @Path("/createMember")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@NotNull final ProcessMemberRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessMemberResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.CREATE);
            response = bean.processMember(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("createMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @Path("/inviteMember")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response invite(@NotNull final ProcessMemberRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessMemberResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.INVITE);
            response = bean.processMember(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("inviteMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @Path("/updateMember")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@NotNull final ProcessMemberRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessMemberResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.UPDATE);
            response = bean.processMember(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("updateMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @DELETE
    @Path("/deleteMember")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@NotNull final ProcessMemberRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessMemberResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.DELETE);
            response = bean.processMember(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("deleteMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @Path("/fetchMembers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetch(@NotNull final FetchMemberRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        FetchMemberResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            response = bean.fetchMembers(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("fetchMembers", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }
}
