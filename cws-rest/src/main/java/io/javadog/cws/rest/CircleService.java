/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
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
import io.javadog.cws.core.ManagementBean;
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

    private final Settings settings = Settings.getInstance();
    @Inject private ManagementBean bean;

    @POST
    @Path("/createCircle")
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response create(@NotNull final ProcessCircleRequest createCircleRequest) {
        return processCircle(createCircleRequest, Action.CREATE, "createCircle");
    }

    @POST
    @Path("/updateCircle")
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response update(@NotNull final ProcessCircleRequest updateCircleRequest) {
        return processCircle(updateCircleRequest, Action.UPDATE, "updateCircle");
    }

    @POST
    @DELETE
    @Path("/deleteCircle")
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response delete(@NotNull final ProcessCircleRequest deleteCircleRequest) {
        return processCircle(deleteCircleRequest, Action.DELETE, "deleteCircle");
    }

    @POST
    @Path("/fetchCircles")
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response fetch(@NotNull final FetchCircleRequest fetchCirclesRequest) {
        final Long startTime = System.nanoTime();
        FetchCircleResponse response;

        try {
            response = bean.fetchCircles(fetchCirclesRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), "fetchCircles", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), "fetchCircles", startTime, e));
            response = new FetchCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }

    private Response processCircle(final ProcessCircleRequest request, final Action action, final String logAction) {
        final Long startTime = System.nanoTime();
        ProcessCircleResponse response;

        try {
            request.setAction(action);
            response = bean.processCircle(request);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), logAction, startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), logAction, startTime, e));
            response = new ProcessCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
