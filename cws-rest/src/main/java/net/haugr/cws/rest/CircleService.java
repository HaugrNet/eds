/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.rest;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import net.haugr.cws.api.common.Action;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.requests.FetchCircleRequest;
import net.haugr.cws.api.requests.ProcessCircleRequest;
import net.haugr.cws.api.responses.FetchCircleResponse;
import net.haugr.cws.api.responses.ProcessCircleResponse;
import net.haugr.cws.core.ManagementBean;
import net.haugr.cws.core.misc.LoggingUtil;
import net.haugr.cws.core.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>REST interface for the Circle functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Path(Constants.REST_CIRCLES_BASE)
public class CircleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CircleService.class);

    @Inject
    private ManagementBean bean;
    private final Settings settings = Settings.getInstance();

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
            LOGGER.info(LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOGGER.error(LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
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
            LOGGER.info(LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOGGER.error(LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e), e);
            response = new ProcessCircleResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
