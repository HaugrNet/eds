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
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response create(@NotNull final ProcessMemberRequest createMemberRequest) {
        final Long startTime = System.nanoTime();
        ProcessMemberResponse response;

        try {
            createMemberRequest.setAction(Action.CREATE);
            response = bean.processMember(createMemberRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "createMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "createMember", startTime, e));
            response = new ProcessMemberResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @Path("/inviteMember")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response invite(@NotNull final ProcessMemberRequest inviteMemberRequest) {
        final Long startTime = System.nanoTime();
        ProcessMemberResponse response;

        try {
            inviteMemberRequest.setAction(Action.INVITE);
            response = bean.processMember(inviteMemberRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "inviteMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "inviteMember", startTime, e));
            response = new ProcessMemberResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @Path("/updateMember")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response update(@NotNull final ProcessMemberRequest updateMemberRequest) {
        final Long startTime = System.nanoTime();
        ProcessMemberResponse response;

        try {
            updateMemberRequest.setAction(Action.UPDATE);
            response = bean.processMember(updateMemberRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "updateMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "updateMember", startTime, e));
            response = new ProcessMemberResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @DELETE
    @Path("/deleteMember")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response delete(@NotNull final ProcessMemberRequest deleteMemberRequest) {
        final Long startTime = System.nanoTime();
        ProcessMemberResponse response;

        try {
            deleteMemberRequest.setAction(Action.DELETE);
            response = bean.processMember(deleteMemberRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteMember", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteMember", startTime, e));
            response = new ProcessMemberResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @Path("/fetchMembers")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response fetch(@NotNull final FetchMemberRequest fetchMembersRequest) {
        final Long startTime = System.nanoTime();
        FetchMemberResponse response;

        try {
            response = bean.fetchMembers(fetchMembersRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchMembers", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchMembers", startTime, e));
            response = new FetchMemberResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }
}
