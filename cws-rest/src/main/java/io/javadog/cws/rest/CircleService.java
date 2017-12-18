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
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Path("/circles")
public class CircleService {

    private static final Logger log = Logger.getLogger(CircleService.class.getName());

    @Inject private SettingBean settings;
    @Inject private SystemBean bean;

    @POST
    @Path("/createCircle")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response create(@NotNull final ProcessCircleRequest createCircleRequest) {
        final Long startTime = System.nanoTime();
        ProcessCircleResponse response;

        try {
            createCircleRequest.setAction(Action.CREATE);
            response = bean.processCircle(createCircleRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "createCircle", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "createCircle", startTime, e));
            response = new ProcessCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @Path("/updateCircle")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response update(@NotNull final ProcessCircleRequest updateCircleRequest) {
        final Long startTime = System.nanoTime();
        ProcessCircleResponse response;

        try {
            updateCircleRequest.setAction(Action.UPDATE);
            response = bean.processCircle(updateCircleRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "updateCircle", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "updateCircle", startTime, e));
            response = new ProcessCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @DELETE
    @Path("/deleteCircle")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response delete(@NotNull final ProcessCircleRequest deleteCircleRequest) {
        final Long startTime = System.nanoTime();
        ProcessCircleResponse response;

        try {
            deleteCircleRequest.setAction(Action.DELETE);
            response = bean.processCircle(deleteCircleRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteCircle", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteCircle", startTime, e));
            response = new ProcessCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @Path("/addTrustee")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response add(@NotNull final ProcessCircleRequest addTrusteeRequest) {
        final Long startTime = System.nanoTime();
        ProcessCircleResponse response;

        try {
            addTrusteeRequest.setAction(Action.ADD);
            response = bean.processCircle(addTrusteeRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "addTrustee", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "addTrustee", startTime, e));
            response = new ProcessCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @Path("/alterTrustee")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response alter(@NotNull final ProcessCircleRequest alterTrusteeRequest) {
        final Long startTime = System.nanoTime();
        ProcessCircleResponse response;

        try {
            alterTrusteeRequest.setAction(Action.ALTER);
            response = bean.processCircle(alterTrusteeRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "alterTrustee", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "alterTrustee", startTime, e));
            response = new ProcessCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @DELETE
    @Path("/removeTrustee")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response remove(@NotNull final ProcessCircleRequest removeTrusteeRequest) {
        final Long startTime = System.nanoTime();
        ProcessCircleResponse response;

        try {
            removeTrusteeRequest.setAction(Action.REMOVE);
            response = bean.processCircle(removeTrusteeRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "removeTrustee", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "removeTrustee", startTime, e));
            response = new ProcessCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @Path("/fetchCircles")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response fetch(@NotNull final FetchCircleRequest fetchCirclesRequest) {
        final Long startTime = System.nanoTime();
        FetchCircleResponse response;

        try {
            response = bean.fetchCircles(fetchCirclesRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchCircles", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchCircles", startTime, e));
            response = new FetchCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }
}
