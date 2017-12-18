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
import io.javadog.cws.core.SettingBean;
import io.javadog.cws.core.SystemBean;
import io.javadog.cws.core.misc.LoggingUtil;
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
public class MemberService {

    private static final Logger log = Logger.getLogger(MemberService.class.getName());

    @Inject private SettingBean settings;
    @Inject private SystemBean bean;

    @POST
    @Path("/createMember")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response create(@NotNull final ProcessMemberRequest createMemberRequest) {
        ProcessMemberResponse createMemberResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            createMemberRequest.setAction(Action.CREATE);
            createMemberResponse = bean.processMember(createMemberRequest);
            returnCode = createMemberResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "createMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(createMemberResponse).build();
    }

    @POST
    @Path("/inviteMember")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response invite(@NotNull final ProcessMemberRequest inviteMemberRequest) {
        ProcessMemberResponse inviteMemberResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            inviteMemberRequest.setAction(Action.INVITE);
            inviteMemberResponse = bean.processMember(inviteMemberRequest);
            returnCode = inviteMemberResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "inviteMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(inviteMemberResponse).build();
    }

    @POST
    @Path("/updateMember")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response update(@NotNull final ProcessMemberRequest updateMemberRequest) {
        ProcessMemberResponse updateMemberResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            updateMemberRequest.setAction(Action.UPDATE);
            updateMemberResponse = bean.processMember(updateMemberRequest);
            returnCode = updateMemberResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "updateMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(updateMemberResponse).build();
    }

    @POST
    @DELETE
    @Path("/deleteMember")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response delete(@NotNull final ProcessMemberRequest deleteMemberRequest) {
        ProcessMemberResponse deleteMemberResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            deleteMemberRequest.setAction(Action.DELETE);
            deleteMemberResponse = bean.processMember(deleteMemberRequest);
            returnCode = deleteMemberResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(deleteMemberResponse).build();
    }

    @POST
    @Path("/fetchMembers")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response fetch(@NotNull final FetchMemberRequest fetchMembersRequest) {
        FetchMemberResponse fetchMembersResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            fetchMembersResponse = bean.fetchMembers(fetchMembersRequest);
            returnCode = fetchMembersResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchMembers", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(fetchMembersResponse).build();
    }
}
