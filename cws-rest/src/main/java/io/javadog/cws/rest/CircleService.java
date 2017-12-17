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
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
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
@Path("/circles")
@Consumes(MediaType.APPLICATION_JSON)
public class CircleService {

    private static final Logger log = Logger.getLogger(CircleService.class.getName());

    @Inject private SettingBean settings;
    @Inject private SystemBean bean;

    @POST
    @Path("/createCircle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@NotNull final ProcessCircleRequest createCircleRequest) {
        ProcessCircleResponse createCircleResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            createCircleRequest.setAction(Action.CREATE);
            createCircleResponse = bean.processCircle(createCircleRequest);
            returnCode = createCircleResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "createCircle", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(createCircleResponse).build();
    }

    @POST
    @Path("/updateCircle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@NotNull final ProcessCircleRequest updateCircleRequest) {
        ProcessCircleResponse updateCircleResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            updateCircleRequest.setAction(Action.UPDATE);
            updateCircleResponse = bean.processCircle(updateCircleRequest);
            returnCode = updateCircleResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "updateCircle", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(updateCircleResponse).build();
    }

    @POST
    @DELETE
    @Path("/deleteCircle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@NotNull final ProcessCircleRequest deleteCircleRequest) {
        ProcessCircleResponse deleteCircleResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            deleteCircleRequest.setAction(Action.DELETE);
            deleteCircleResponse = bean.processCircle(deleteCircleRequest);
            returnCode = deleteCircleResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteCircle", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(deleteCircleResponse).build();
    }

    @POST
    @Path("/addTrustee")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(@NotNull final ProcessCircleRequest addTrusteeRequest) {
        ProcessCircleResponse addTrusteeResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            addTrusteeRequest.setAction(Action.ADD);
            addTrusteeResponse = bean.processCircle(addTrusteeRequest);
            returnCode = addTrusteeResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "addTrustee", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(addTrusteeResponse).build();
    }

    @POST
    @Path("/alterTrustee")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response alter(@NotNull final ProcessCircleRequest alterTrusteeRequest) {
        ProcessCircleResponse alterTrusteeResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            alterTrusteeRequest.setAction(Action.ALTER);
            alterTrusteeResponse = bean.processCircle(alterTrusteeRequest);
            returnCode = alterTrusteeResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "alterTrustee", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(alterTrusteeResponse).build();
    }

    @POST
    @DELETE
    @Path("/removeTrustee")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response remove(@NotNull final ProcessCircleRequest removeTrusteeRequest) {
        ProcessCircleResponse removeTrusteeResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            removeTrusteeRequest.setAction(Action.REMOVE);
            removeTrusteeResponse = bean.processCircle(removeTrusteeRequest);
            returnCode = removeTrusteeResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "removeTrustee", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(removeTrusteeResponse).build();
    }

    @POST
    @Path("/fetchCircles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetch(@NotNull final FetchCircleRequest fetchCirclesRequest) {
        FetchCircleResponse fetchCirclesResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            fetchCirclesResponse = bean.fetchCircles(fetchCirclesRequest);
            returnCode = fetchCirclesResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchCircles", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(fetchCirclesResponse).build();
    }
}
