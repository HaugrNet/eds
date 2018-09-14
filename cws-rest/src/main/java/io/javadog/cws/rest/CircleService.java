/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * <p>REST interface for the Circle functionality.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Path(Constants.REST_CIRCLES_BASE)
public class CircleService {

    private static final Logger LOG = Logger.getLogger(CircleService.class.getName());

    private final Settings settings = Settings.getInstance();
    @Inject private ManagementBean bean;

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_CIRCLES_CREATE)
    public Response create(@NotNull final ProcessCircleRequest createCircleRequest) {
        return processCircle(createCircleRequest, Action.CREATE, Constants.REST_CIRCLES_CREATE);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_CIRCLES_UPDATE)
    public Response update(@NotNull final ProcessCircleRequest updateCircleRequest) {
        return processCircle(updateCircleRequest, Action.UPDATE, Constants.REST_CIRCLES_UPDATE);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_CIRCLES_DELETE)
    public Response delete(@NotNull final ProcessCircleRequest deleteCircleRequest) {
        return processCircle(deleteCircleRequest, Action.DELETE, Constants.REST_CIRCLES_DELETE);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_CIRCLES_FETCH)
    public Response fetch(@NotNull final FetchCircleRequest fetchCirclesRequest) {
        final String restAction = Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_FETCH;
        final long startTime = System.nanoTime();
        FetchCircleResponse response;

        try {
            response = bean.fetchCircles(fetchCirclesRequest);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOG.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new FetchCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }

    private Response processCircle(final ProcessCircleRequest request, final Action action, final String logAction) {
        final String restAction = Constants.REST_CIRCLES_BASE + logAction;
        final long startTime = System.nanoTime();
        ProcessCircleResponse response;

        try {
            request.setAction(action);
            response = bean.processCircle(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOG.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new ProcessCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
