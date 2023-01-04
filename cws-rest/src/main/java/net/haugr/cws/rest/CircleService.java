/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
import net.haugr.cws.api.requests.FetchCircleRequest;
import net.haugr.cws.api.requests.ProcessCircleRequest;
import net.haugr.cws.core.ManagementBean;
import net.haugr.cws.core.model.Settings;

/**
 * <p>REST interface for the Circle functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Path(Constants.REST_CIRCLES_BASE)
public class CircleService {

    private static final String PROCESS_METHOD = "processCircle";
    private static final String FETCH_METHOD = "fetchCircles";

    @Inject
    private ManagementBean bean;
    private final Settings settings = Settings.getInstance();

    @POST
    @Path(Constants.REST_CIRCLES_CREATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response create(@NotNull final ProcessCircleRequest createCircleRequest) {
        createCircleRequest.setAction(Action.CREATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, createCircleRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_CREATE);
    }

    @POST
    @Path(Constants.REST_CIRCLES_UPDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response update(@NotNull final ProcessCircleRequest updateCircleRequest) {
        updateCircleRequest.setAction(Action.UPDATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, updateCircleRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_UPDATE);
    }

    @POST
    @Path(Constants.REST_CIRCLES_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessCircleRequest deleteCircleRequest) {
        deleteCircleRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, deleteCircleRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_DELETE);
    }

    @POST
    @Path(Constants.REST_CIRCLES_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchCircleRequest fetchCirclesRequest) {
        return CommonService.runRequest(settings, bean, FETCH_METHOD, fetchCirclesRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_FETCH);
    }
}
