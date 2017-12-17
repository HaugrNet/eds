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
@Path("/circles")
@Consumes(MediaType.APPLICATION_JSON)
public class CircleService {

    private static final Logger log = Logger.getLogger(CircleService.class.getName());

    @Inject private SystemBean bean;

    @POST
    @Path("/createCircle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@NotNull final ProcessCircleRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessCircleResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.CREATE);
            response = bean.processCircle(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("createCircle", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @Path("/updateCircle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@NotNull final ProcessCircleRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessCircleResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.UPDATE);
            response = bean.processCircle(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("updateCircle", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @DELETE
    @Path("/deleteCircle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@NotNull final ProcessCircleRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessCircleResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.DELETE);
            response = bean.processCircle(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("deleteCircle", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @Path("/addTrustee")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(@NotNull final ProcessCircleRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessCircleResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.ADD);
            response = bean.processCircle(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("addTrustee", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @Path("/alterTrustee")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response alter(@NotNull final ProcessCircleRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessCircleResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.ALTER);
            response = bean.processCircle(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("alterTrustee", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @DELETE
    @Path("/removeTrustee")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response remove(@NotNull final ProcessCircleRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessCircleResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.REMOVE);
            response = bean.processCircle(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("removeTrustee", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @Path("/fetchCircles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetch(@NotNull final FetchCircleRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        FetchCircleResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            response = bean.fetchCircles(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("fetchCircles", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }
}
